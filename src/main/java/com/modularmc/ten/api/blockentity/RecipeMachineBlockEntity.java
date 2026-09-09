package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntBiFunction;

/**
 * 配方机器基类：负责配方查找/身份检测（配方变化重置进度）、四维 B_actual
 * 锁定（物品/流体/概率输出/储能取最小）、动态槽位堆叠上限与
 * 七阶段原子产出（快照-模拟-提交，失败回滚）。
 */
public abstract class RecipeMachineBlockEntity extends ProcessingMachineBlockEntity {

    private static final int ABSOLUTE_MAX_STACK = 99;

    public SlotInfo slotInfo;
    protected FormsCombinedRecipe currentRecipe;

    public RecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SlotInfo info) {
        super(type, pos, state);
        this.slotInfo = info;
    }

    public abstract FormsCombinedRecipe findRecipe();

    @Override
    public int baseTickTime() {
        return currentRecipe != null ? currentRecipe.time() : 200;
    }

    @Override
    public boolean conditionStart() {
        // Capture previous recipe before looking up the new one
        FormsCombinedRecipe previousRecipe = currentRecipe;
        FormsCombinedRecipe nextRecipe = findRecipe();

        // Reset progress if recipe identity changed.
        // Comparison uses stable recipe registry ID (ResourceLocation) via Objects.equals:
        // - null↔recipe: IDs differ → reset
        // - recipe A↔recipe B: IDs differ → reset
        // - same recipe (same ID, possibly different instance): IDs equal → preserve
        ResourceLocation prevId = previousRecipe != null ? previousRecipe.getId() : null;
        ResourceLocation nextId = nextRecipe != null ? nextRecipe.getId() : null;
        boolean identityChanged = !Objects.equals(prevId, nextId);
        if (identityChanged) {
            progress = 0;
            clearLockedBatch(); // 配方身份变化时清批处理锁
        }

        currentRecipe = nextRecipe;

        if (currentRecipe == null) {
            clearLockedBatch();
            // 空闲时也设置 64×理论B 堆叠（装批量升级即常驻生效，无需配方运行）
            installDynamicSlotLimit();
            return false;
        }

        // Lock maxProgress when recipe identity changes or no lock exists yet.
        // Duration multiplier is captured at start to prevent per-tick drift.
        if (identityChanged || !hasLockedMaxProgress()) {
            maxProgress = Math.max(1, (int) Math.ceil(baseTickTime() * durationMultiplier));
            lockMaxProgressForNewOperation(maxProgress);
        }

        // ───── 批处理 B 锁定（四维计算）─────
        if (identityChanged || !hasLockedBatch()) {
            // New operation or no lock: calculate B_actual from all dimensions
            int B_theory = Math.max(1, 1 + batch);
            int B_byItems = computeBByItems();
            int B_byFluids = computeBByFluids();
            int B_byOutput = computeBByOutput();
            int B_byEnergy = computeBByEnergy();

            if (!validateAndLockB(B_theory, B_byItems, B_byFluids, B_byOutput, B_byEnergy)) {
                // B < 1: cannot start — locks are cleared by validateAndLockB
                installDynamicSlotLimit();
                return false;
            }
        } else {
            // Same operation, B already locked: verify recipes still matches for continuation.
            // Do NOT recalculate B — it stays fixed until completion or identity change.
            // ── Q1 Fixed-B Stalled Contract ──
            // When inputs drop below what lockedB requires, revalidateInputs() returns false
            // and conditionStart returns false (machine stalls / blocks cooking).
            // lockedB is NOT reduced or cleared here — it stays at its original value.
            // This is intentional: lockedB is a commit to a fixed batch size for the entire
            // operation. The machine will resume automatically when enough input is replenished
            // (revalidateInputs recovers). Dynamic B downgrading would violate the contract
            // that lockedB is immutable once set. Only onCookFinish completion or recipe
            // identity change clears lockedB.
            if (!revalidateInputs()) {
                return false;
            }
        }

        installDynamicSlotLimit();
        return true;
    }

    // ───── 批处理 B 维度计算辅助 ─────

    /**
     * Computes B_byItems: maximum batch size constrained by item input availability.
     * For each consumable (chance > 0) ingredient, needs amountOrCount per batch unit.
     * For catalyst (chance &le; 0), only needs a single copy — not B-multiplied.
     * Uses sequential allocation across input slots matching the recipe's ingredient semantics.
     *
     * @return B_byItems, or {@code Integer.MAX_VALUE} if no consumable items constrain B
     */
    protected int computeBByItems() {
        if (currentRecipe == null || itemHandler == null) return 0;
        var itemInputs = currentRecipe.allInputItems();
        if (itemInputs.isEmpty()) return Integer.MAX_VALUE;

        int B = Integer.MAX_VALUE;
        for (var ing : itemInputs) {
            if (ing.isAllowAll()) continue;
            int amountPerBatch = ing.amountOrCount();
            if (amountPerBatch <= 0) continue;

            // Sum available quantity across all matching input slots
            int available = 0;
            for (int i = slotInfo.i1(); i <= slotInfo.i2() && i < itemHandler.getSlots(); i++) {
                if (!slotType(i).canIn()) continue;
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!ing.contains(stack.getItem())) continue;
                available += stack.getCount();
            }

            if (ing.chance() <= 0) {
                // Catalyst: only needs a single copy (not multiplied by B)
                if (available < amountPerBatch) return 0; // Missing catalyst → cannot start
                continue; // Catalyst does not constrain B further
            }

            // Consumable: needs amountPerBatch per batch unit
            if (available < amountPerBatch) return 0; // Can't even do 1 batch
            int bForThis = available / amountPerBatch;
            B = Math.min(B, bForThis);
        }

        return B == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, B);
    }

    /**
     * Computes B_byFluids: maximum batch size constrained by fluid input availability.
     * All fluid inputs are consumable and multiplied by B.
     *
     * @return B_byFluids, or {@code Integer.MAX_VALUE} if no fluid inputs constrain B
     */
    protected int computeBByFluids() {
        if (currentRecipe == null) return 0;
        var fluidInputs = currentRecipe.allInputFluids();
        if (fluidInputs.isEmpty()) return Integer.MAX_VALUE;

        int B = Integer.MAX_VALUE;
        for (var ing : fluidInputs) {
            if (ing.isAllowAll()) continue;
            int amountPerBatch = ing.amountOrCount();
            if (amountPerBatch <= 0) continue;

            // Sum available amount across all matching input tanks
            int available = 0;
            for (int i = slotInfo.fi1(); i <= slotInfo.fi2() && i < tanks.size(); i++) {
                if (!tankType(i).canIn()) continue;
                FluidStack fluid = tanks.get(i).getFluid();
                if (fluid.isEmpty()) continue;
                if (!ing.contains(fluid.getFluid())) continue;
                available += fluid.getAmount();
            }

            if (available < amountPerBatch) return 0;
            int bForThis = available / amountPerBatch;
            B = Math.min(B, bForThis);
        }

        return B == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, B);
    }

    /**
     * Computes B_byOutput: maximum batch size constrained by worst-case output capacity.
     * Uses target-aware candidate enumeration from min(B_theory,19) down to 1.
     * For each candidate B, calls {@link #canFitOutputsForBatch(int)} which simulates
     * worst-case placement per real target slot/tank with existing stack/fluid type,
     * effective limit 99, and same-target aggregation across ingredients.
     * <p>
     * Returns the first (largest) B that fits, or 0 if none fits.
     * Removes the old global totalSpace / unique-item-division approach which
     * overestimated B when different items compete for the same target slot.
     *
     * @return B_byOutput, or {@code Integer.MAX_VALUE} if no outputs constrain B
     */
    protected int computeBByOutput() {
        if (currentRecipe == null) return 0;
        var itemOutputs = currentRecipe.allOutputItems();
        var fluidOutputs = currentRecipe.allOutputFluids();
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) return Integer.MAX_VALUE;

        int B_theory = Math.max(1, 1 + batch);
        int maxB = Math.min(B_theory, 19);

        // Enumerate descending: first (largest) B that fits is the answer
        for (int candidate = maxB; candidate >= 1; candidate--) {
            if (canFitOutputsForBatch(candidate)) return candidate;
        }
        return 0;
    }

    /**
     * Computes B_byEnergy: maximum batch size constrained by stored energy.
     * Uses base FE/t (without B multiplier) — not total FE/t to avoid circular dependency.
     *
     * @return B_byEnergy, or {@code Integer.MAX_VALUE} if energy is unlimited
     */
    protected int computeBByEnergy() {
        if (energyStorage == null) return 0;
        int stored = energyStorage.getEnergyStored();
        if (stored <= 0) return 0;
        int baseFePerTick = Math.max(1, getActualEfficiency());
        return stored / baseFePerTick;
    }

    /**
     * Per-tick check: verifies the lockedB batch can still complete successfully.
     * Checks that inputs are still sufficient for lockedB consumption and that
     * worst-case output can still fit.
     * <p>
     * Unlike {@link #conditionStart()}, this does NOT modify lockedB or any locks.
     *
     * @return true if the batch should be blocked (stalled), false if it can continue
     */
    protected boolean checkBatchCooking() {
        if (currentRecipe == null) return true;
        if (getLockedBatchSize() <= 1) {
            // B=1: use existing fast path (single-craft check)
            if (!canFitAllOutputs()) return true;
            var outputFluids = currentRecipe.allOutputFluids();
            for (var ing : outputFluids) {
                if (!canFitFluidOutput(ing)) return true;
            }
            return false;
        }

        int B = getLockedBatchSize();

        // Check item inputs: for each consumable ingredient, verify enough for B × amount
        for (var ing : currentRecipe.allInputItems()) {
            if (ing.isAllowAll()) continue;
            int perBatch = ing.amountOrCount();
            if (perBatch <= 0) continue;

            int available = 0;
            for (int i = slotInfo.i1(); i <= slotInfo.i2() && i < itemHandler.getSlots(); i++) {
                if (!slotType(i).canIn()) continue;
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty() || !ing.contains(stack.getItem())) continue;
                available += stack.getCount();
            }

            if (ing.chance() <= 0) {
                // Catalyst: single copy needed
                if (available < perBatch) return true;
            } else {
                // Consumable: perBatch × B needed
                long needed = (long) perBatch * B;
                if (available < needed) return true;
            }
        }

        // Check fluid inputs
        for (var ing : currentRecipe.allInputFluids()) {
            if (ing.isAllowAll()) continue;
            int perBatch = ing.amountOrCount();
            if (perBatch <= 0) continue;

            int available = 0;
            for (int i = slotInfo.fi1(); i <= slotInfo.fi2() && i < tanks.size(); i++) {
                if (!tankType(i).canIn()) continue;
                FluidStack fluid = tanks.get(i).getFluid();
                if (fluid.isEmpty() || !ing.contains(fluid.getFluid())) continue;
                available += fluid.getAmount();
            }

            long needed = (long) perBatch * B;
            if (available < needed) return true;
        }

        // Check output capacity: worst case for B
        if (!canFitOutputsForBatch(B)) return true;

        return false;
    }

    /**
     * Checks whether the worst-case output for B batch units can fit.
     * Simulates placing all deterministic outputs × B into output slots/tanks.
     *
     * @param B the batch size to check
     * @return true if worst-case output fits
     */
    protected boolean canFitOutputsForBatch(int B) {
        if (currentRecipe == null) return true;
        var itemOutputs = currentRecipe.allOutputItems();
        boolean allFit = true;

        // Item output capacity check using simulated slot state
        if (!itemOutputs.isEmpty() && itemHandler != null) {
            int outSlotCount = slotInfo.o2() - slotInfo.o1() + 1;
            ItemStack[] simulated = new ItemStack[outSlotCount];
            for (int i = 0; i < outSlotCount; i++) {
                ItemStack existing = itemHandler.getStackInSlot(slotInfo.o1() + i);
                simulated[i] = existing.isEmpty() ? ItemStack.EMPTY : existing.copy();
            }

            // Process each output ingredient
            for (var ing : itemOutputs) {
                if (ing.isAllowAll()) continue;
                if (ing.chance() <= 0) continue;
                ItemStack sym = ing.symbolItem();
                if (sym.isEmpty()) continue;
                Item item = sym.getItem();

                // Worst-case per batch unit
                int perUnitWorst = Math.multiplyExact(ing.amountOrCount(), ing.rolls());
                if (perUnitWorst <= 0) continue;
                long totalWorst = (long) perUnitWorst * B;
                if (totalWorst > Integer.MAX_VALUE) return false;

                int remaining = (int) totalWorst;
                // Use candidate B explicitly for slot limit — not global lockedB
                int limitPerSlot = computeItemSlotLimit(item, B);

                for (int i = 0; i < outSlotCount && remaining > 0; i++) {
                    ItemStack slot = simulated[i];
                    if (!slot.isEmpty() && slot.getItem() != item) continue;

                    int existingCount = slot.isEmpty() ? 0 : slot.getCount();
                    int slotLimit = Math.max(limitPerSlot, existingCount);
                    int space = slotLimit - existingCount;
                    int toAdd = Math.min(space, remaining);

                    if (toAdd > 0) {
                        if (slot.isEmpty()) {
                            simulated[i] = new ItemStack(item, toAdd);
                        } else {
                            slot.grow(toAdd);
                        }
                        remaining -= toAdd;
                    }
                }

                if (remaining > 0) {
                    allFit = false;
                    break;
                }
            }
        }

        if (!allFit) return false;

        // Fluid output capacity check
        for (var ing : currentRecipe.allOutputFluids()) {
            if (ing.isAllowAll()) continue;
            int amountPerBatch = ing.amountOrCount();
            if (amountPerBatch <= 0) continue;
            long totalNeeded = (long) amountPerBatch * B;
            if (totalNeeded > Integer.MAX_VALUE) return false;

            FluidStack needed = ing.symbolFluid();
            if (needed.isEmpty()) continue;
            needed.setAmount((int) totalNeeded);

            boolean fits = false;
            for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size(); i++) {
                if (tankType(i).canOut()) {
                    // Check if this one tank can hold the full amount
                    if (tanks.get(i).fill(needed, IFluidHandler.FluidAction.SIMULATE) >= needed.getAmount()) {
                        fits = true;
                        break;
                    }
                }
            }
            if (!fits) return false;
        }

        return true;
    }

    // ───── Dynamic slot limit support ─────

    /**
     * Installs a dynamic slot limit provider on the item handler for output slots.
     * Called when a new recipe starts (in {@link #conditionStart()}).
     * <p>
     * 改革后：槽位堆叠上限固定为 {@code 64 × lockedB}（简单固定倍率，与配方 amount/rolls 无关）。
     * roll 类型配方不再影响堆叠。机器内部通过自定义 oversized codec 持久化超 99 堆叠。
     */
    protected void installDynamicSlotLimit() {
        if (itemHandler == null) return;

        // 空闲时也用理论 B（1+Σbatch）设置固定 64×B 堆叠：装批量升级即常驻生效，
        // 无需等机器运行锁定批量（对齐末影箱动态堆叠的常态生效语义）。
        // 仅当有配方且有具体输出物品时，才需要按输出槽位语义区分（同公式 64×B）。
        var itemOutputs = currentRecipe == null ? null : currentRecipe.allOutputItems();
        itemHandler.setDynamicSlotLimit(createOutputSlotLimitProvider(itemOutputs));
    }

    /**
     * Creates a {@link ToIntBiFunction} that computes per-slot limits.
     * Slot limit = 64 × theoreticalB（简单固定倍率，与配方 amount/rolls 无关）。
     * 用理论 B（1+Σbatch，装升级即反映），空闲与运行时都常驻生效。
     * roll 类型配方不再影响堆叠。
     * <p>
     * 输出槽（slotInfo.o1..o2）用 64×B；其余槽（输入/升级）保持默认 64。
     *
     * @param itemOutputs 配方输出物品（可 null；空闲/无输出时仍按输出槽位 64×B 处理）
     */
    private ToIntBiFunction<Integer, ItemStack> createOutputSlotLimitProvider(
                                                                              List<FormsCombinedIngredient> itemOutputs) {
        int batchSize = getTheoreticalBatchSize();
        long limitWithB = 64L * batchSize;
        int cap = (int) Math.min(limitWithB, Integer.MAX_VALUE);
        return (slot, candidate) -> {
            // 输入槽（i1..i2）与输出槽（o1..o2）都应用 64×B 堆叠（双向，对齐精妙存储：
            // 槽位上限 = 物品 maxStackSize × 批量倍率）。升级槽等其他槽保持默认 64。
            boolean isInput = slot >= slotInfo.i1() && slot <= slotInfo.i2();
            boolean isOutput = slot >= slotInfo.o1() && slot <= slotInfo.o2();
            if (!isInput && !isOutput) {
                return 64;
            }
            // Never go below existing count (preserve overstack from previous configs)
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (!existing.isEmpty()) {
                return Math.max(cap, existing.getCount());
            }
            return cap;
        };
    }

    /**
     * Returns the effective output slot limit for a given slot and item stack.
     * This is a public safe helper for external query (e.g. UI display).
     *
     * @param slot  the slot index
     * @param stack the candidate stack
     * @return the effective slot limit
     */
    public int getEffectiveSlotLimit(int slot, ItemStack stack) {
        if (itemHandler == null) return 64;
        return itemHandler.getEffectiveSlotLimit(slot, stack);
    }

    // ───── Cooking / capacity pre-check ─────

    @Override
    public boolean cooking() {
        if (currentRecipe == null) return true;

        // B > 1 时使用批处理感知的 cooking 检查
        if (getLockedBatchSize() > 1) {
            return checkBatchCooking();
        }

        // B=1: existing single-craft check
        if (!canFitAllOutputs()) return true;
        var outputFluids = currentRecipe.allOutputFluids();
        for (var ing : outputFluids) {
            if (!canFitFluidOutput(ing)) return true;
        }
        return false;
    }

    /**
     * Joint capacity pre-check: simulates placing ALL output stacks into
     * output slots using dynamic limits, ensuring no item is lost and
     * that two outputs for the same item don't individually fit but
     * together exceed capacity.
     *
     * @return true if all outputs fit, false if any would overflow
     */
    protected boolean canFitAllOutputs() {
        if (currentRecipe == null) return true;
        var itemOutputs = currentRecipe.allOutputItems();
        if (itemOutputs.isEmpty()) return true;

        // Build simulated slot state: for each output slot, track item type and count
        int outSlotCount = slotInfo.o2() - slotInfo.o1() + 1;
        ItemStack[] simulated = new ItemStack[outSlotCount];
        for (int i = 0; i < outSlotCount; i++) {
            ItemStack existing = itemHandler.getStackInSlot(slotInfo.o1() + i);
            simulated[i] = existing.isEmpty() ? ItemStack.EMPTY : existing.copy();
        }

        // Process each output ingredient in order
        for (var ing : itemOutputs) {
            if (ing.isAllowAll()) continue;
            if (ing.chance() <= 0) continue;
            ItemStack sym = ing.symbolItem();
            if (sym.isEmpty()) continue;
            Item item = sym.getItem();

            // Compute max possible output for this ingredient
            int maxCount = Math.multiplyExact(ing.amountOrCount(), ing.rolls());
            if (maxCount <= 0) continue;

            // Pre-compute the slot limit for this item
            int limitPerSlot = computeItemSlotLimit(item);

            int remaining = maxCount;
            for (int i = 0; i < outSlotCount && remaining > 0; i++) {
                ItemStack slot = simulated[i];
                if (!slot.isEmpty() && slot.getItem() != item) continue; // wrong item

                int existingCount = slot.isEmpty() ? 0 : slot.getCount();
                int slotLimit = Math.max(limitPerSlot, existingCount);
                int space = slotLimit - existingCount;
                int toAdd = Math.min(space, remaining);

                if (toAdd > 0) {
                    if (slot.isEmpty()) {
                        simulated[i] = new ItemStack(item, toAdd);
                    } else {
                        slot.grow(toAdd);
                    }
                    remaining -= toAdd;
                }
            }

            if (remaining > 0) {
                return false; // Not enough space even with dynamic limits
            }
        }
        return true;
    }

    /**
     * Single-ingredient capacity check (legacy signature preserved for
     * backward-compatible reflection access). Uses proper joint check
     * internally. External callers should prefer {@link #canFitAllOutputs()}.
     */
    protected boolean canFitOutput(FormsCombinedIngredient ing) {
        // Delegate to joint check for correctness
        return canFitAllOutputs();
    }

    /**
     * Computes the slot limit for a specific item using an explicit batch size.
     * Slot limit = 64 × batchSize（简单固定倍率，与配方 amount/rolls 无关）。
     * roll 类型配方不再影响堆叠。
     *
     * @param item      the output item（保留签名，不再参与计算）
     * @param batchSize the batch size to use (locked B for running, candidate for simulation)
     * @return the slot limit for this item at the given batch size
     */
    private int computeItemSlotLimit(Item item, int batchSize) {
        if (item == null) return 64;
        long limitWithB = 64L * Math.max(1, batchSize);
        return (int) Math.min(limitWithB, Integer.MAX_VALUE);
    }

    /**
     * Computes the slot limit for a specific item using the current locked batch size.
     * Delegates to {@link #computeItemSlotLimit(Item, int)} with the global lockedB.
     */
    private int computeItemSlotLimit(Item item) {
        return computeItemSlotLimit(item, getLockedBatchSize());
    }

    /**
     * Computes the effective slot limit for a specific item, considering existing
     * items in the slot (never truncate existing overstack), using an explicit
     * batch size. Used by candidate simulation where the batch is not yet locked.
     *
     * @param item          the output item
     * @param existingCount the current count in the slot
     * @param batchSize     the batch size to compute the limit for
     * @return the effective slot limit, never below existingCount
     */
    private int computeEffectiveSlotLimit(Item item, int existingCount, int batchSize) {
        int limit = computeItemSlotLimit(item, batchSize);
        return Math.max(limit, existingCount);
    }

    /**
     * Computes the effective slot limit for a specific item using the current
     * locked batch size. Delegates to {@link #computeEffectiveSlotLimit(Item, int, int)}.
     */
    private int computeEffectiveSlotLimit(Item item, int existingCount) {
        return computeEffectiveSlotLimit(item, existingCount, getLockedBatchSize());
    }

    protected boolean canFitFluidOutput(FormsCombinedIngredient ing) {
        FluidStack stack = ing.symbolFluid();
        if (stack.isEmpty()) return true;
        for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size(); i++) {
            if (tankType(i).canOut() && tanks.get(i).fill(stack, IFluidHandler.FluidAction.SIMULATE) >= stack.getAmount()) {
                return true;
            }
        }
        return false;
    }

    // ───── Output ─────

    @Override
    public void onCookFinish() {
        if (currentRecipe == null) return;

        int B = getLockedBatchSize();

        // ── Phase 1: Build InputConsumptionPlan (M2: dry-run with lockedB) ──
        InputConsumptionPlan plan = InputConsumptionPlan.build(
                currentRecipe, slotInfo, itemHandler, tanks,
                this::slotType, this::tankType, B);
        if (plan == null) {
            // Inputs insufficient for lockedB at completion time — abort
            return;
        }

        // ── Phase 2: Collect actual outputs in memory (pre-mutation) ──
        // Any generation exception (overflow, invalid amount) happens BEFORE
        // any consumption, guaranteeing zero loss on generation failure.
        List<ItemStack> pendingItems = new java.util.ArrayList<>();
        List<FluidStack> pendingFluids = new java.util.ArrayList<>();
        if (!collectBatchOutputs(pendingItems, pendingFluids, B)) {
            // Collection failed (invalid amount/count/overflow) → no consumption
            return;
        }

        // ── Phase 3: Pre-validate output fit (structured OutputPlan check) ──
        if (!validatePendingOutputsFit(pendingItems, pendingFluids)) {
            // Output capacity insufficient — abort before consumption
            return;
        }

        // ── Phase 4: Save output snapshots for unified rollback ──
        ItemStack[] outputSlotSnapshots = snapshotOutputSlots();
        FluidStack[] outputTankSnapshots = snapshotOutputTanks();

        // ── Phase 5: Execute consumption (plan.execute has internal rollback) ──
        if (!plan.execute(itemHandler, tanks)) {
            // Consumption failed internally — already rolled back by plan
            TEN.LOGGER.error("InputConsumptionPlan.execute failed for recipe=" + currentRecipe.getId());
            return;
        }

        // ── Phase 6: Commit outputs (into real handler/tanks) ──
        if (!commitCollectedItems(pendingItems) || !commitCollectedFluids(pendingFluids)) {
            // Output commit failed — rollback inputs + output snapshots
            plan.restore(itemHandler, tanks);
            restoreOutputSlots(outputSlotSnapshots);
            restoreOutputTanks(outputTankSnapshots);
            TEN.LOGGER.error("Output commit failed for recipe=" + currentRecipe.getId() + " — inputs and outputs rolled back");
            return;
        }

        // ── Phase 7: Clear lock on successful completion ──
        clearLockedBatch();
    }

    /**
     * Generates batch outputs for the current recipe.
     * <p>
     * For deterministic outputs (chance >= 1.0): output amount × lockedB.
     * For probability outputs (chance < 1.0): calls genItem/genFluid B times,
     * each with internal rolls, preserving the original chance semantics.
     * <p>
     * Fluid outputs always have rolls=1 per API constraint, so only
     * deterministic (chance=1.0) or single-roll probability.
     * <p>
     * This method directly mutates the handler — it is called from subclasses
     * that need to override the default atomic flow. The base
     * {@link #onCookFinish()} uses {@link #collectBatchOutputs} then commit
     * for atomicity instead.
     */
    protected void generateBatchOutputs(int B) {
        List<ItemStack> items = new java.util.ArrayList<>();
        List<FluidStack> fluids = new java.util.ArrayList<>();
        collectBatchOutputs(items, fluids, B);
        commitCollectedItems(items);
        commitCollectedFluids(fluids);
    }

    /**
     * Collects batch outputs into the given lists without mutating the handler.
     * <p>
     * Deterministic outputs (chance ≥ 1.0): amountOrCount × B.
     * Probability outputs (chance < 1.0): B calls to genItem/genFluid,
     * each with internal rolls.
     * <p>
     * Returns false if any output has an invalid amount (overflow, zero after
     * generation, or exceeds safety limits). No handler mutation occurs on
     * failure — the caller can safely abort.
     *
     * @param itemCollector  list to receive item outputs
     * @param fluidCollector list to receive fluid outputs
     * @param B              batch size
     * @return true if collection succeeded, false on invalid amount/overflow
     */
    protected boolean collectBatchOutputs(List<ItemStack> itemCollector, List<FluidStack> fluidCollector, int B) {
        if (currentRecipe == null) return false;
        try {
            for (var ing : currentRecipe.output()) {
                if (ing.isAllowAll()) continue;

                if ("item".equals(ing.form())) {
                    if (ing.chance() >= 1.0d - 1e-12) {
                        // Deterministic: output amount × B
                        ItemStack base = ing.symbolItem();
                        if (base.isEmpty()) continue;
                        long totalCount = (long) base.getCount() * B;
                        // 上限对齐 64×B 堆叠（不再按 99 拆），溢出上限由输出容量检查拦截
                        long cap = 64L * Math.max(1, B);
                        if (totalCount > cap) return false;
                        if (totalCount > 0) {
                            itemCollector.add(new ItemStack(base.getItem(), (int) totalCount));
                        }
                    } else {
                        // Probability: B calls to genItem, each with internal rolls
                        for (int b = 0; b < B; b++) {
                            ItemStack result = ing.genItem();
                            if (!result.isEmpty()) {
                                if (result.getCount() > ABSOLUTE_MAX_STACK) return false;
                                itemCollector.add(result);
                            }
                        }
                    }
                } else if ("fluid".equals(ing.form())) {
                    if (ing.chance() >= 1.0d - 1e-12) {
                        // Deterministic: output amount × B
                        FluidStack base = ing.symbolFluid();
                        if (base.isEmpty()) continue;
                        long totalAmount = (long) base.getAmount() * B;
                        if (totalAmount > Integer.MAX_VALUE) return false;
                        fluidCollector.add(new FluidStack(base.getFluid(), (int) totalAmount));
                    } else {
                        // Probability fluid: B calls to genFluid
                        for (int b = 0; b < B; b++) {
                            FluidStack result = ing.genFluid();
                            if (!result.isEmpty()) {
                                if (result.getAmount() < 0) return false;
                                fluidCollector.add(result);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            // Generation exception before any consumption — safe to abort
            return false;
        }
    }

    /**
     * Validates that all pending output stacks can fit in output slots/tanks.
     * Uses the same slot-aware simulation as {@link #canFitOutputsForBatch(int)}
     * but operates on the actual collected stacks rather than worst-case estimates.
     * <p>
     * Returns false if any stack cannot be placed (overflow, incompatible item type).
     * No handler mutation occurs on failure.
     */
    private boolean validatePendingOutputsFit(List<ItemStack> items, List<FluidStack> fluids) {
        // Item output validation using simulated slot state
        if (!items.isEmpty() && itemHandler != null) {
            int outSlotCount = slotInfo.o2() - slotInfo.o1() + 1;
            ItemStack[] simulated = new ItemStack[outSlotCount];
            for (int i = 0; i < outSlotCount; i++) {
                ItemStack existing = itemHandler.getStackInSlot(slotInfo.o1() + i);
                simulated[i] = existing.isEmpty() ? ItemStack.EMPTY : existing.copy();
            }

            for (ItemStack stack : items) {
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                int remaining = stack.getCount();
                int limitPerSlot = computeItemSlotLimit(item);

                for (int i = 0; i < outSlotCount && remaining > 0; i++) {
                    ItemStack slot = simulated[i];
                    if (!slot.isEmpty() && slot.getItem() != item) continue;

                    int existingCount = slot.isEmpty() ? 0 : slot.getCount();
                    int slotLimit = Math.max(limitPerSlot, existingCount);
                    int space = slotLimit - existingCount;
                    int toAdd = Math.min(space, remaining);

                    if (toAdd > 0) {
                        if (slot.isEmpty()) {
                            simulated[i] = new ItemStack(item, toAdd);
                        } else {
                            slot.grow(toAdd);
                        }
                        remaining -= toAdd;
                    }
                }

                if (remaining > 0) return false;
            }
        }

        // Fluid output validation
        if (!fluids.isEmpty() && !tanks.isEmpty()) {
            // Create working copies of tank fluids for simulation
            Map<Integer, Integer> tankSpace = new HashMap<>();
            for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size(); i++) {
                if (!tankType(i).canOut()) continue;
                FluidStack existing = tanks.get(i).getFluid();
                int existingAmount = existing.isEmpty() ? 0 : existing.getAmount();
                int capacity = tanks.get(i).getCapacity();
                tankSpace.put(i, capacity - existingAmount);
            }

            for (FluidStack stack : fluids) {
                if (stack.isEmpty()) continue;
                int remaining = stack.getAmount();
                for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && remaining > 0 && i < tanks.size(); i++) {
                    if (!tankType(i).canOut()) continue;
                    int space = tankSpace.getOrDefault(i, 0);
                    if (space <= 0) continue;
                    int toFill = Math.min(space, remaining);
                    tankSpace.put(i, space - toFill);
                    remaining -= toFill;
                }
                if (remaining > 0) return false;
            }
        }

        return true;
    }

    /**
     * Commits collected item stacks to output slots using direct placement.
     * Returns false if any item cannot be fully placed (overflow).
     * Does NOT call {@link #onRemainingOutput} — overflow is treated as a
     * commit failure, not a drop event.
     */
    private boolean commitCollectedItems(List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            int remaining = stack.getCount();
            Item item = stack.getItem();

            for (int i = slotInfo.o1(); i <= slotInfo.o2() && remaining > 0; i++) {
                ItemStack existing = itemHandler.getStackInSlot(i);
                int existingCount = existing.isEmpty() ? 0 : existing.getCount();
                int slotLimit = computeEffectiveSlotLimit(item, existingCount);
                int space = slotLimit - existingCount;

                if (space <= 0) continue;
                if (!existing.isEmpty() && existing.getItem() != item) continue;

                int toAdd = Math.min(space, remaining);
                if (existing.isEmpty()) {
                    itemHandler.setStackInSlot(i, new ItemStack(item, toAdd));
                } else {
                    existing.grow(toAdd);
                }
                remaining -= toAdd;
            }

            if (remaining > 0) return false;
        }
        return true;
    }

    /**
     * Commits collected fluid stacks to output tanks.
     * Returns false if any fluid cannot be fully placed (tank full).
     */
    private boolean commitCollectedFluids(List<FluidStack> fluids) {
        for (FluidStack stack : fluids) {
            if (stack == null || stack.isEmpty()) continue;
            int remaining = stack.getAmount();
            for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && remaining > 0 && i < tanks.size(); i++) {
                if (!tankType(i).canOut()) continue;
                FluidStack toFill = new FluidStack(stack.getFluid(), remaining);
                int filled = tanks.get(i).fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                remaining -= filled;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    /**
     * Snapshots output item slots for potential rollback.
     * Only slots with existing items are captured.
     */
    private ItemStack[] snapshotOutputSlots() {
        int count = slotInfo.o2() - slotInfo.o1() + 1;
        ItemStack[] snapshots = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            ItemStack existing = itemHandler.getStackInSlot(slotInfo.o1() + i);
            snapshots[i] = existing.isEmpty() ? ItemStack.EMPTY : existing.copy();
        }
        return snapshots;
    }

    /**
     * Restores output item slots from snapshots.
     */
    private void restoreOutputSlots(ItemStack[] snapshots) {
        if (snapshots == null) return;
        for (int i = 0; i < snapshots.length; i++) {
            if (snapshots[i] != null) {
                itemHandler.setStackInSlot(slotInfo.o1() + i, snapshots[i]);
            }
        }
    }

    /**
     * Snapshots output fluid tanks for potential rollback.
     */
    private FluidStack[] snapshotOutputTanks() {
        int count = Math.min(tanks.size(), slotInfo.fo2() + 1);
        FluidStack[] snapshots = new FluidStack[count];
        for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size(); i++) {
            snapshots[i] = tanks.get(i).getFluid().copy();
        }
        return snapshots;
    }

    /**
     * Restores output fluid tanks from snapshots.
     */
    private void restoreOutputTanks(FluidStack[] snapshots) {
        if (snapshots == null) return;
        for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size() && i < snapshots.length; i++) {
            if (snapshots[i] != null) {
                tanks.get(i).setFluid(snapshots[i]);
            }
        }
    }

    /**
     * Re-validate that inputs are still sufficient for the current recipe at
     * the locked batch size. Uses {@link InputConsumptionPlan#build} as a
     * dry-run — if a plan can be constructed, inputs are sufficient.
     * <p>
     * Unlike the old {@link FormsCombinedRecipe#matches} (which checks at
     * single-craft level), this method checks against B × consumable amount,
     * ensuring alignment with the actual consumption that will happen in
     * {@link #onCookFinish()}. Catalyst (chance ≤ 0) items are skipped
     * (not multiplied by B).
     * <p>
     * Called at completion time (onCookFinish) before any mutation.
     *
     * @return true if inputs still satisfy the recipe for lockedB batch units
     */
    protected boolean revalidateInputs() {
        if (currentRecipe == null || itemHandler == null || tanks == null) return false;
        int B = getLockedBatchSize();
        var plan = InputConsumptionPlan.build(
                currentRecipe, slotInfo, itemHandler, tanks,
                this::slotType, this::tankType, B);
        return plan != null;
    }

    // ───── InputConsumptionPlan ─────

    /**
     * A consumption plan that atomically validates and deducts item and fluid
     * inputs for a recipe. Built before any mutation; execution is atomic with
     * rollback on failure.
     * <p>
     * Order-sensitive: item deductions allocate sequentially per ingredient,
     * never counting the same quantity for multiple ingredients. Fluid deductions
     * use {@link IFluidHandler.FluidAction#EXECUTE} to trigger tank change listeners.
     * Item deductions use {@link net.neoforged.neoforge.items.IItemHandler#extractItem}
     * semantics via {@code setStackInSlot} to trigger handler change listeners.
     */
    protected static class InputConsumptionPlan {

        private final int[] itemDeductions;   // per-slot deduction amount
        private final int[] fluidDeductions;  // per-tank deduction amount
        private ItemStack[] itemSnapshots;    // pre-execution snapshots for rollback
        private FluidStack[] fluidSnapshots;  // pre-execution snapshots for rollback
        private boolean executed;

        private InputConsumptionPlan(int slots, int tanks) {
            this.itemDeductions = new int[slots];
            this.fluidDeductions = new int[tanks > 0 ? tanks : 0];
        }

        /**
         * Build a consumption plan from the current recipe and inventory state.
         * Allocates deductions per slot/tank — sequential allocation per ingredient,
         * no double-counting. If any ingredient cannot be fully satisfied, returns null.
         * <p>
         * When {@code lockedB > 1}, consumable (chance > 0) item/fluid amounts are
         * multiplied by lockedB. Catalyst (chance &le; 0) items are NOT multiplied —
         * only a single copy is consumed regardless of batch size.
         *
         * @param recipe      the current recipe
         * @param slotInfo    slot layout
         * @param itemHandler the item handler
         * @param tanks       the fluid tanks
         * @param slotType    slot type getter (for canIn check)
         * @param tankType    tank type getter (for canIn check)
         * @param lockedB     batch size (1 for single-craft, >1 for batch)
         * @return the plan, or null if any ingredient can't be satisfied
         */
        static InputConsumptionPlan build(
                                          FormsCombinedRecipe recipe,
                                          SlotInfo slotInfo,
                                          MachineItemHandler itemHandler,
                                          List<MachineFluidTank> tanks,
                                          FormsCombinedIngredient.IngredientTypeGetter slotType,
                                          FormsCombinedIngredient.IngredientTypeGetter tankType,
                                          int lockedB) {
            int itemSlots = itemHandler != null ? itemHandler.getSlots() : 0;
            int tankCount = tanks != null ? tanks.size() : 0;
            InputConsumptionPlan plan = new InputConsumptionPlan(itemSlots, tankCount);

            // Plan item deductions: sequential per ingredient, no double-count
            for (var ing : recipe.allInputItems()) {
                if (ing.chance() <= 0) {
                    // Catalyst: not consumed — skip (remains in slot)
                    continue;
                }
                int baseNeeded = ing.amountOrCount();
                // Consumable: needs baseNeeded × lockedB
                long totalNeededLong = (long) baseNeeded * lockedB;
                if (totalNeededLong > Integer.MAX_VALUE) return null;
                int needed = (int) totalNeededLong;
                for (int i = slotInfo.i1(); i <= slotInfo.i2() && needed > 0; i++) {
                    if (i >= itemSlots) break;
                    if (!slotType.get(i).canIn()) continue;
                    ItemStack stack = itemHandler.getStackInSlot(i);
                    if (!ing.contains(stack.getItem())) continue;
                    // Subtract already-planned deductions for this slot
                    int available = Math.max(0, stack.getCount() - plan.itemDeductions[i]);
                    if (available <= 0) continue;
                    int toTake = Math.min(needed, available);
                    plan.itemDeductions[i] += toTake;
                    needed -= toTake;
                }
                if (needed > 0) {
                    return null; // cannot satisfy this ingredient
                }
            }

            // Plan fluid deductions: sequential per ingredient, no double-count
            for (var ing : recipe.allInputFluids()) {
                int baseNeeded = ing.amountOrCount();
                // All fluid inputs are consumable: × lockedB
                long totalNeededLong = (long) baseNeeded * lockedB;
                if (totalNeededLong > Integer.MAX_VALUE) return null;
                int needed = (int) totalNeededLong;
                for (int i = slotInfo.fi1(); i <= slotInfo.fi2() && needed > 0; i++) {
                    if (i >= tankCount) break;
                    if (!tankType.get(i).canIn()) continue;
                    FluidStack fluid = tanks.get(i).getFluid();
                    if (!ing.contains(fluid.getFluid())) continue;
                    // Subtract already-planned deductions for this tank
                    int available = Math.max(0, fluid.getAmount() - plan.fluidDeductions[i]);
                    if (available <= 0) continue;
                    int toTake = Math.min(needed, available);
                    plan.fluidDeductions[i] += toTake;
                    needed -= toTake;
                }
                if (needed > 0) {
                    return null;
                }
            }

            return plan;
        }

        /**
         * Execute the consumption plan atomically.
         * <p>
         * Snapshot current state, execute fluid drains via drain(EXECUTE), then
         * item reductions via setStackInSlot. If any step fails, roll back all
         * changes and return false.
         *
         * @param itemHandler the item handler
         * @param tanks       the fluid tanks
         * @return true if all consumptions succeeded, false on failure (rolled back)
         */
        boolean execute(MachineItemHandler itemHandler, List<MachineFluidTank> tanks) {
            if (isEmpty()) return true;
            if (executed) throw new IllegalStateException("InputConsumptionPlan already executed");

            // 1. Snapshot current state for rollback
            snapshot(itemHandler, tanks);

            try {
                // 2. Execute fluid deductions first (drain with EXECUTE triggers listener)
                for (int i = 0; i < fluidDeductions.length; i++) {
                    if (fluidDeductions[i] <= 0) continue;
                    FluidStack drained = tanks.get(i).drain(fluidDeductions[i], IFluidHandler.FluidAction.EXECUTE);
                    if (drained.getAmount() != fluidDeductions[i]) {
                        throw new IllegalStateException(
                                "Fluid drain expected " + fluidDeductions[i] +
                                        " but got " + drained.getAmount() + " for tank " + i);
                    }
                }

                // 3. Execute item deductions via setStackInSlot (triggers onContentsChanged → markDirty)
                for (int i = 0; i < itemDeductions.length; i++) {
                    if (itemDeductions[i] <= 0) continue;
                    ItemStack slot = itemHandler.getStackInSlot(i);
                    if (slot.getCount() < itemDeductions[i]) {
                        throw new IllegalStateException(
                                "Item slot " + i + " has " + slot.getCount() +
                                        " but plan needs " + itemDeductions[i]);
                    }
                    ItemStack reduced = slot.copy();
                    reduced.shrink(itemDeductions[i]);
                    itemHandler.setStackInSlot(i, reduced);
                }

                executed = true;
                return true;

            } catch (Exception e) {
                TEN.LOGGER.error("InputConsumptionPlan execution failed: " + e.getMessage());
                rollback(itemHandler, tanks);
                return false;
            }
        }

        /**
         * @return true if this plan has no deductions (nothing to consume)
         */
        boolean isEmpty() {
            for (int d : itemDeductions) if (d > 0) return false;
            for (int d : fluidDeductions) if (d > 0) return false;
            return true;
        }

        /**
         * Restore state to pre-execution snapshots.
         * Can be called after a successful {@link #execute} to undo the
         * consumption (e.g. when an output commit fails downstream).
         * <p>
         * After this call, the plan can be reused (executed flag is reset).
         */
        public void restore(MachineItemHandler itemHandler, List<MachineFluidTank> tanks) {
            if (!executed || itemSnapshots == null) return;
            rollback(itemHandler, tanks);
            executed = false;
        }

        private void snapshot(MachineItemHandler itemHandler, List<MachineFluidTank> tanks) {
            itemSnapshots = new ItemStack[itemDeductions.length];
            for (int i = 0; i < itemDeductions.length; i++) {
                if (itemDeductions[i] > 0) {
                    itemSnapshots[i] = itemHandler.getStackInSlot(i).copy();
                }
            }
            fluidSnapshots = new FluidStack[fluidDeductions.length];
            for (int i = 0; i < fluidDeductions.length; i++) {
                if (fluidDeductions[i] > 0) {
                    fluidSnapshots[i] = tanks.get(i).getFluid().copy();
                }
            }
        }

        private void rollback(MachineItemHandler itemHandler, List<MachineFluidTank> tanks) {
            // Restore fluid tanks first (reverse order from execute)
            for (int i = 0; i < fluidSnapshots.length; i++) {
                if (fluidSnapshots[i] != null) {
                    tanks.get(i).setFluid(fluidSnapshots[i]);
                }
            }
            // Restore item slots
            for (int i = 0; i < itemSnapshots.length; i++) {
                if (itemSnapshots[i] != null) {
                    itemHandler.setStackInSlot(i, itemSnapshots[i]);
                }
            }
        }
    }

    /**
     * Distributes an output stack across output slots using dynamic slot limits.
     * Uses the machine's dynamic limit instead of
     * {@link ItemStack#getMaxStackSize()} to allow overstacking.
     * <p>
     * If a remainder exists after all slots are filled, it is logged
     * as an error (not silently dropped) to catch recipe/miscalculation bugs.
     */
    protected void giveOutput(ItemStack stack, int start, int end) {
        if (stack == null || stack.isEmpty()) return;
        int remaining = stack.getCount();
        Item item = stack.getItem();

        for (int i = start; i <= end && remaining > 0; i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            int existingCount = existing.isEmpty() ? 0 : existing.getCount();

            // Compute dynamic effective limit for this slot
            int slotLimit = computeEffectiveSlotLimit(item, existingCount);
            int space = slotLimit - existingCount;

            if (space <= 0) continue;

            // Check item compatibility
            if (!existing.isEmpty() && existing.getItem() != item) continue;

            int toAdd = Math.min(space, remaining);

            if (existing.isEmpty()) {
                itemHandler.setStackInSlot(i, new ItemStack(item, toAdd));
            } else {
                existing.grow(toAdd);
            }
            remaining -= toAdd;
        }

        if (remaining > 0) {
            TEN.LOGGER.error("giveOutput: " + remaining + " items of " + stack.getCount() + " could not be placed for item=" + item + " in outputs [" + start + "," + end + "]");
            onRemainingOutput(stack.copyWithCount(remaining));
        }
    }

    /**
     * Called when output items could not fully fit in available output slots.
     * Default implementation drops the remainder at the machine's position
     * on the server side. Client-side callers silently return — server is
     * authoritative for item drops.
     * <p>
     * Protected so subclasses and test stubs can override to verify behavior
     * without requiring a running game environment.
     *
     * @param remainder the items that could not be placed (never empty)
     * @throws IllegalStateException if level is null (fail-fast, not silent loss)
     */
    protected void onRemainingOutput(ItemStack remainder) {
        if (level == null) {
            throw new IllegalStateException(
                    "Cannot drop remaining output items: level is null. Remainder: " + remainder);
        }
        if (level.isClientSide()) {
            return;
        }
        Containers.dropItemStack(level,
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                remainder);
    }

    protected void giveFluidOutput(FluidStack stack) {
        for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size(); i++) {
            if (!tankType(i).canOut()) continue;
            int filled = tanks.get(i).fill(stack, IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(filled);
            if (stack.isEmpty()) break;
        }
    }
}
