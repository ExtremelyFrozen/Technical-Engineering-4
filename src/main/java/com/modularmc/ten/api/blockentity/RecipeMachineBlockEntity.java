package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
import java.util.logging.Logger;

public abstract class RecipeMachineBlockEntity extends ProcessingMachineBlockEntity {

    private static final Logger LOG = Logger.getLogger("RecipeMachineBlockEntity");
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
        // Comparison uses stable recipe registry ID (Identifier) via Objects.equals:
        // - null↔recipe: IDs differ → reset
        // - recipe A↔recipe B: IDs differ → reset
        // - same recipe (same ID, possibly different instance): IDs equal → preserve
        Identifier prevId = previousRecipe != null ? previousRecipe.getId() : null;
        Identifier nextId = nextRecipe != null ? nextRecipe.getId() : null;
        if (!Objects.equals(prevId, nextId)) {
            progress = 0;
        }

        currentRecipe = nextRecipe;
        installDynamicSlotLimit();
        return currentRecipe != null;
    }

    // ───── Dynamic slot limit support ─────

    /**
     * Installs a dynamic slot limit provider on the item handler for output slots.
     * Called when a new recipe starts (in {@link #conditionStart()}).
     * <p>
     * The slot limit for each output slot is computed as:
     * {@code min(N + 63, 99)} where N is the aggregate max possible output
     * for a given item across all output ingredients with chance > 0.
     * This allows the machine's own {@link #giveOutput} to place items
     * beyond the normal 64 max stack size, while still respecting the
     * Minecraft ItemStack codec absolute limit of 99.
     */
    protected void installDynamicSlotLimit() {
        if (itemHandler == null) return;
        if (currentRecipe == null) {
            itemHandler.setDynamicSlotLimit(null);
            return;
        }

        // Pre-compute N per unique item across all item-form outputs with chance > 0
        var itemOutputs = currentRecipe.allOutputItems();
        if (itemOutputs.isEmpty()) {
            itemHandler.setDynamicSlotLimit(null);
            return;
        }

        itemHandler.setDynamicSlotLimit(createOutputSlotLimitProvider(itemOutputs));
    }

    /**
     * Creates a {@link ToIntBiFunction} that computes per-slot limits
     * for the given list of output ingredients.
     */
    private ToIntBiFunction<Integer, ItemStack> createOutputSlotLimitProvider(
                                                                              List<FormsCombinedIngredient> itemOutputs) {
        return (slot, candidate) -> {
            if (slot < slotInfo.o1() || slot > slotInfo.o2()) {
                // Not an output slot — use default 64
                return 64;
            }

            // Determine which item we're dealing with
            Item item;
            if (candidate == null || candidate.isEmpty()) {
                // Empty slot, no candidate — return default 64 (canFitOutput/giveOutput
                // will compute explicitly for the specific item)
                return 64;
            }
            item = candidate.getItem();

            // Aggregate N for this item across all outputs with chance > 0
            int totalN = 0;
            for (var ing : itemOutputs) {
                if (ing.isAllowAll()) continue;
                if (ing.chance() <= 0) continue;
                ItemStack sym = ing.symbolItem();
                if (sym.isEmpty()) continue;
                if (sym.getItem() != item) continue;
                int n = Math.multiplyExact(ing.amountOrCount(), ing.rolls());
                totalN += n;
            }

            if (totalN <= 0) return 64;

            int limit = Math.min(totalN + 63, ABSOLUTE_MAX_STACK);
            // Never go below existing count (preserve overstack from previous configs)
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (!existing.isEmpty()) {
                limit = Math.max(limit, existing.getCount());
            }
            return limit;
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
        if (currentRecipe == null) return false;

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
     * Computes the slot limit for a specific item based on current recipe outputs.
     * Formula: min(N + 63, 99) where N = Σ(amountOrCount × rolls) across all
     * outputs for this item with chance > 0.
     */
    private int computeItemSlotLimit(Item item) {
        if (currentRecipe == null || item == null) return 64;
        int totalN = 0;
        for (var ing : currentRecipe.allOutputItems()) {
            if (ing.isAllowAll()) continue;
            if (ing.chance() <= 0) continue;
            ItemStack sym = ing.symbolItem();
            if (sym.isEmpty() || sym.getItem() != item) continue;
            int n = Math.multiplyExact(ing.amountOrCount(), ing.rolls());
            totalN += n;
        }
        if (totalN <= 0) return 64;
        return Math.min(totalN + 63, ABSOLUTE_MAX_STACK);
    }

    /**
     * Computes the slot limit for a specific item, considering existing
     * items in the slot (never truncate existing overstack).
     */
    private int computeEffectiveSlotLimit(Item item, int existingCount) {
        int limit = computeItemSlotLimit(item);
        return Math.max(limit, existingCount);
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

        // 1. Re-validate inputs using the same matching strategy as conditionStart
        if (!revalidateInputs()) return;

        // 2. Build consumption plan — atomically validates all inputs are sufficient
        InputConsumptionPlan plan = InputConsumptionPlan.build(
                currentRecipe, slotInfo, itemHandler, tanks, this::slotType, this::tankType);
        if (plan == null) {
            // Plan build failed: some input can't be fully satisfied — abort
            return;
        }

        // 3. Execute consumption (atomic: if any step fails, everything rolls back)
        if (!plan.execute(itemHandler, tanks)) {
            // Execution failed after partial consumption — rolled back already
            LOG.severe("InputConsumptionPlan.execute failed for recipe=" + currentRecipe.getId());
            return;
        }

        // 4. Generate random outputs and place them
        var items = currentRecipe.generateItems();
        var fluids = currentRecipe.generateFluids();

        for (ItemStack s : items) {
            giveOutput(s, slotInfo.o1(), slotInfo.o2());
        }
        for (FluidStack s : fluids) {
            giveFluidOutput(s);
        }
    }

    /**
     * Re-validate that inputs are still sufficient for the current recipe.
     * Called at completion time (onCookFinish) before any mutation.
     * <p>
     * Default uses {@link FormsCombinedRecipe#matches} (tolerant — allows
     * extra items in input slots). Subclasses may override to use
     * {@link FormsCombinedRecipe#matchesExactInputs} for strict matching
     * (e.g. {@link com.modularmc.ten.common.blockentity.machine.IndfurBlockEntity}).
     *
     * @return true if inputs still satisfy the recipe
     */
    protected boolean revalidateInputs() {
        if (currentRecipe == null || itemHandler == null) return false;
        return currentRecipe.matches(itemHandler, tanks, this::slotType, this::tankType);
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
         *
         * @param recipe     the current recipe
         * @param slotInfo   slot layout
         * @param itemHandler the item handler
         * @param tanks       the fluid tanks
         * @param slotType    slot type getter (for canIn check)
         * @param tankType    tank type getter (for canIn check)
         * @return the plan, or null if any ingredient can't be satisfied
         */
        static InputConsumptionPlan build(
                FormsCombinedRecipe recipe,
                SlotInfo slotInfo,
                MachineItemHandler itemHandler,
                List<MachineFluidTank> tanks,
                FormsCombinedIngredient.IngredientTypeGetter slotType,
                FormsCombinedIngredient.IngredientTypeGetter tankType
        ) {
            int itemSlots = itemHandler != null ? itemHandler.getSlots() : 0;
            int tankCount = tanks != null ? tanks.size() : 0;
            InputConsumptionPlan plan = new InputConsumptionPlan(itemSlots, tankCount);

            // Plan item deductions: sequential per ingredient, no double-count
            for (var ing : recipe.allInputItems()) {
                if (ing.chance() <= 0) continue;
                int needed = ing.amountOrCount();
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
                int needed = ing.amountOrCount();
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
                LOG.severe("InputConsumptionPlan execution failed: " + e.getMessage());
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
            LOG.severe("giveOutput: " + remaining + " items of " + stack.getCount() + " could not be placed for item=" + item + " in outputs [" + start + "," + end + "]");
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
