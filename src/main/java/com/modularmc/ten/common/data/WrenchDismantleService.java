package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.fluids.FluidStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles wrench-based dismantling of functional blocks.
 * <p>
 * Transaction order (server-side only):
 * <ol>
 * <li>Guard: distance, mayBuild, target tag check</li>
 * <li>Snapshot BE data (inventory, upgrades, energy, tanks, face config) — read-only</li>
 * <li>Fire {@link CommonHooks#fireBlockBreak} — cancel → return false (no state changed)</li>
 * <li><b>Encode config data</b> to {@link TagValueOutput} with {@link FailingReporter}.
 * If any codec encoding fails, {@link FailingReporter#report} throws a RuntimeException
 * immediately — no world state was modified yet.</li>
 * <li>Call {@code block.playerWillDestroy}</li>
 * <li>Extract inventory/upgrade contents (actual move via {@code extractItem})</li>
 * <li>Call 6-param {@code state.onDestroyedByPlayer} — false → restore handlers, return false</li>
 * <li>Mark {@code destroyDropsHandled} to prevent BaseMachineBlock double-drop</li>
 * <li>Call {@code block.destroy} (handlers already cleared)</li>
 * <li>Build drops: BlockItem via {@link BlockItem#setBlockEntityData} with the
 * pre-encoded output, plus separate stacks for extracted inventory/upgrade contents</li>
 * <li>Deliver to player inventory; remainder drops at player feet</li>
 * </ol>
 * <p>
 * The machine's BlockItem carries only configuration data (energy, tanks, face config,
 * redstone mode, facing, active state) via the standard {@code BLOCK_ENTITY_DATA} component.
 * Inventory and upgrade contents are transferred as independent item stacks — never
 * duplicated onto the BlockItem.
 * <p>
 * If any critical step fails, the original block is restored and no items are emitted.
 * Encoding errors or handler extraction failure produce a {@link RuntimeException}.
 */
public final class WrenchDismantleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TEN.MOD_ID);

    private WrenchDismantleService() {}

    /**
     * A {@link ProblemReporter} that fails immediately on any encoding error.
     * Used with {@link TagValueOutput#createWithContext} to ensure that
     * codec or serialization failures are never silently discarded —
     * the entire dismantle operation is aborted before any world mutation.
     */
    private static final class FailingReporter implements ProblemReporter {

        static final FailingReporter INSTANCE = new FailingReporter();

        @Override
        public ProblemReporter forChild(ProblemReporter.PathElement path) {
            return this; // propagate the same failing instance
        }

        @Override
        public void report(ProblemReporter.Problem problem) {
            throw new RuntimeException(
                    "WrenchDismantle encoding error: " + problem.description());
        }
    }

    private record MachineSnapshot(
                                   BlockState state,
                                   List<ItemStack> inventorySnap,
                                   List<ItemStack> upgradeSnap,
                                   int energy,
                                   List<FluidStack> tanks,
                                   int[] energyFaceData,
                                   int[] itemFaceData,
                                   int[] fluidFaceData,
                                   int redstoneMode,
                                   int facingVal,
                                   boolean active,
                                   int upgradeSize) {}

    /**
     * Attempt to dismantle the block at {@code pos}.
     *
     * @return true if the block was dismantled, false if rejected.
     */
    public static boolean dismantle(Level level, BlockPos pos, ServerPlayer player) {
        if (level.isClientSide()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        if (player.distanceToSqr(pos.getCenter()) > 81.0) {
            return false;
        }
        if (!player.getAbilities().mayBuild && !player.isCreative()) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        Block block = state.getBlock();

        // ── 1. Snapshot mutable data (read-only, no mutations yet) ──────
        MachineSnapshot snapshot = null;
        boolean extracted = false;

        if (be instanceof CmMachineBlockEntity machine) {
            machine.initMachine();

            List<ItemStack> invSnap = new ArrayList<>();
            if (machine.itemHandler != null) {
                for (int i = 0; i < machine.itemHandler.getSlots(); i++) {
                    invSnap.add(machine.itemHandler.getStackInSlot(i).copy());
                }
            }

            List<ItemStack> upgSnap = new ArrayList<>();
            if (machine.upgradeHandler != null) {
                for (int i = 0; i < machine.upgradeHandler.getSlots(); i++) {
                    upgSnap.add(machine.upgradeHandler.getStackInSlot(i).copy());
                }
            }

            int energy = (machine.energyStorage != null) ? machine.energyStorage.getEnergyStored() : 0;

            List<FluidStack> tankSnap = new ArrayList<>();
            for (var tank : machine.tanks) {
                tankSnap.add(tank.getFluid().copy());
            }

            int[] energyFace = new int[6];
            int[] itemFace = new int[6];
            int[] fluidFace = new int[6];
            for (Direction dir : Direction.values()) {
                int idx = dir.get3DDataValue();
                energyFace[idx] = machine.energyFaceMode.getOrDefault(dir, machine.initialFaceModeEnergy());
                itemFace[idx] = machine.itemFaceMode.getOrDefault(dir, machine.initialFaceModeItem());
                fluidFace[idx] = machine.fluidFaceMode.getOrDefault(dir, machine.initialFaceModeFluid());
            }

            snapshot = new MachineSnapshot(
                    state, invSnap, upgSnap, energy, tankSnap,
                    energyFace, itemFace, fluidFace,
                    machine.redstoneMode, machine.facingVal, machine.active,
                    machine.upgradeSize);
        }

        // ── 2. Fire break event ─────────────────────────────────────────
        // No state has been modified yet at this point.
        GameType gameType = player.gameMode.getGameModeForPlayer();
        var breakEvent = CommonHooks.fireBlockBreak(level, gameType, player, pos, state);
        if (breakEvent.isCanceled()) {
            return false;
        }

        // ── 3. Pre-encode config data BEFORE any mutation ──────────────
        // If encoding fails, the FailingReporter throws immediately.
        // The block and its handlers are still intact — no world state changed.
        TagValueOutput preEncodedOutput = null;

        if (be instanceof CmMachineBlockEntity machine && snapshot != null) {
            HolderLookup.Provider registries = level.registryAccess();
            preEncodedOutput = TagValueOutput.createWithContext(FailingReporter.INSTANCE, registries);

            // WARNING: "inventory" and "upgrades" keys are intentionally omitted.
            // readTileData uses .ifPresent guards, so missing keys leave handlers empty.

            preEncodedOutput.putInt("energy", snapshot.energy);

            for (int i = 0; i < snapshot.tanks.size(); i++) {
                preEncodedOutput.store("tank" + i, FluidStack.OPTIONAL_CODEC, snapshot.tanks.get(i));
            }

            for (Direction dir : Direction.values()) {
                int idx = dir.get3DDataValue();
                preEncodedOutput.putInt("direEnergy" + idx, snapshot.energyFaceData[idx]);
                preEncodedOutput.putInt("direItem" + idx, snapshot.itemFaceData[idx]);
                preEncodedOutput.putInt("direFluid" + idx, snapshot.fluidFaceData[idx]);
            }

            preEncodedOutput.putInt("upgrade_size", snapshot.upgradeSize);
            preEncodedOutput.putInt("redstoneMode", snapshot.redstoneMode);
            preEncodedOutput.putInt("facingVal", snapshot.facingVal);
            preEncodedOutput.putBoolean("active", snapshot.active);

            // If any `store` call above triggered FailingReporter.report(),
            // we never reach here — the method has already thrown.
        }

        // ── 4. playerWillDestroy ────────────────────────────────────────
        BlockState adjustedState = block.playerWillDestroy(level, pos, state, player);

        // ── 5. Extract inventory/upgrade contents (actual move) ─────────
        List<ItemStack> inventoryDrops = new ArrayList<>();
        List<ItemStack> upgradeDrops = new ArrayList<>();

        if (be instanceof CmMachineBlockEntity machine) {
            if (machine.itemHandler != null) {
                for (int i = 0; i < machine.itemHandler.getSlots(); i++) {
                    ItemStack slotStack = machine.itemHandler.extractItem(i, Integer.MAX_VALUE, false);
                    if (!slotStack.isEmpty()) {
                        inventoryDrops.add(slotStack);
                    }
                }
            }

            if (machine.upgradeHandler != null) {
                for (int i = 0; i < machine.upgradeHandler.getSlots(); i++) {
                    ItemStack slotStack = machine.upgradeHandler.extractItem(i, Integer.MAX_VALUE, false);
                    if (!slotStack.isEmpty()) {
                        upgradeDrops.add(slotStack);
                    }
                }
            }
            extracted = true;
        }

        // ── 6. 6-param onDestroyedByPlayer ──────────────────────────────
        boolean removed = state.onDestroyedByPlayer(level, pos, player,
                player.getMainHandItem(), adjustedState.canHarvestBlock(level, pos, player),
                level.getFluidState(pos));

        if (!removed) {
            if (be instanceof CmMachineBlockEntity machine && snapshot != null) {
                restoreMachine(machine, snapshot, extracted);
            }
            return false;
        }

        // ── 7. block.destroy callback ───────────────────────────────────
        if (be instanceof CmMachineBlockEntity machine) {
            machine.markDestroyDropsHandled();
        }
        block.destroy(level, pos, state);

        // ── 8. Build drop list ─────────────────────────────────────────
        List<ItemStack> drops = new ArrayList<>();

        if (be instanceof CmMachineBlockEntity machine && snapshot != null && preEncodedOutput != null) {
            // Apply pre-encoded output to the BlockItem stack
            ItemStack blockStack = new ItemStack(block);
            BlockItem.setBlockEntityData(blockStack, machine.getType(), preEncodedOutput);
            drops.add(blockStack);

            drops.addAll(inventoryDrops);
            drops.addAll(upgradeDrops);

            machine.markDestroyDropsHandled();
        } else {
            drops.add(new ItemStack(block));
        }

        // ── 9. Pre-consolidate drops before delivery ────────────────────
        List<ItemStack> consolidated = consolidateDrops(drops);

        // ── 10. Deliver consolidated drops to player ────────────────────
        for (ItemStack stack : consolidated) {
            if (stack.isEmpty()) continue;
            if (!player.addItem(stack)) {
                var drop = stack.copy();
                level.addFreshEntity(new ItemEntity(level,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        drop, 0.0, 0.0, 0.0));
            }
        }

        return true;
    }

    /**
     * Pre-consolidates dismantled item stacks into the fullest legal stacks
     * before passing them to {@code player.addItem}.
     * <p>
     * Rules:
     * <ul>
     * <li>Preserves first-occurrence order.</li>
     * <li>Ignores {@link ItemStack#EMPTY} entries.</li>
     * <li>Copies input stacks — originals are never mutated.</li>
     * <li>Only merges when {@link ItemStack#isSameItemSameComponents} returns
     * {@code true} AND both target and source are
     * {@link ItemStack#isStackable stackable}.</li>
     * <li>Each output count does not exceed {@link ItemStack#getMaxStackSize}.</li>
     * <li>Stacks with count &gt; maxStackSize are split into full stacks + remainder.</li>
     * <li>Different {@code BLOCK_ENTITY_DATA}, durability, enchantments, custom names,
     * or maxStackSize=1 items remain separate.</li>
     * <li>Total item count is conserved across each distinct (item+components) group.</li>
     * </ul>
     *
     * @param drops the raw dismantle drops (not modified)
     * @return a new list with stacks consolidated
     */
    private static List<ItemStack> consolidateDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>();

        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;

            ItemStack source = stack.copy();

            // ── Non-stackable (maxStackSize == 1): keep individual ──────
            if (!source.isStackable()) {
                int count = source.getCount();
                for (int i = 0; i < count; i++) {
                    ItemStack single = source.copy();
                    single.setCount(1);
                    result.add(single);
                }
                continue;
            }

            // ── Stackable: merge into existing, then add remainder ─────
            while (!source.isEmpty()) {
                for (ItemStack existing : result) {
                    if (existing.isEmpty()) continue;
                    if (!ItemStack.isSameItemSameComponents(existing, source)) continue;

                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space <= 0) continue;

                    int toTransfer = Math.min(space, source.getCount());
                    existing.setCount(existing.getCount() + toTransfer);
                    source.setCount(source.getCount() - toTransfer);

                    if (source.isEmpty()) break;
                    // Partially transferred — continue outer while to find next slot
                    break;
                }

                if (source.isEmpty()) break;

                // No existing slot could accept more — add as new entry
                if (source.getCount() > source.getMaxStackSize()) {
                    ItemStack fullStack = source.copy();
                    fullStack.setCount(source.getMaxStackSize());
                    result.add(fullStack);
                    source.setCount(source.getCount() - source.getMaxStackSize());
                    // Loop again to try merging or add remainder
                } else {
                    result.add(source);
                    break;
                }
            }
        }

        return result;
    }

    private static void restoreMachine(CmMachineBlockEntity machine, MachineSnapshot snap, boolean wasExtracted) {
        try {
            machine.initMachine();

            if (wasExtracted && machine.itemHandler != null) {
                for (int i = 0; i < Math.min(snap.inventorySnap.size(), machine.itemHandler.getSlots()); i++) {
                    machine.itemHandler.setStackInSlot(i, snap.inventorySnap.get(i).copy());
                }
            }

            if (wasExtracted && machine.upgradeHandler != null) {
                for (int i = 0; i < Math.min(snap.upgradeSnap.size(), machine.upgradeHandler.getSlots()); i++) {
                    machine.upgradeHandler.setStackInSlot(i, snap.upgradeSnap.get(i).copy());
                }
            }

            if (machine.energyStorage != null) {
                machine.energyStorage.setEnergy(snap.energy);
            }

            for (int i = 0; i < Math.min(snap.tanks.size(), machine.tanks.size()); i++) {
                machine.tanks.get(i).setFluid(snap.tanks.get(i).copy());
            }

            for (Direction dir : Direction.values()) {
                int idx = dir.get3DDataValue();
                machine.energyFaceMode.put(dir, snap.energyFaceData[idx]);
                machine.itemFaceMode.put(dir, snap.itemFaceData[idx]);
                machine.fluidFaceMode.put(dir, snap.fluidFaceData[idx]);
            }

            machine.redstoneMode = snap.redstoneMode;
            machine.facingVal = snap.facingVal;
            machine.active = snap.active;
            machine.upgradeSize = snap.upgradeSize;

            machine.setChanged();
            LOGGER.warn("Restored machine at {} from snapshot after failed dismantle", machine.getBlockPos());
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to restore machine at {} after failed dismantle. " + "Contents may be lost!", machine.getBlockPos(), e);
            throw new RuntimeException("Failed to restore machine after failed dismantle", e);
        }
    }
}
