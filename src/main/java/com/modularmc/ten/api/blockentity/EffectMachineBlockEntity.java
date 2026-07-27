package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EffectMachineBlockEntity extends CmMachineBlockEntity {

    public EffectMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    @Override
    public void tick() {
        doBaseData();
        process();
    }

    public void process() {
        // Client guard — process/injection runs only on the server logical side
        if (level != null && level.isClientSide()) return;

        // P2-T7: Syn光合注能在所有condition/signal/energyAllowRun门禁之前执行。
        // 即使 conditionStart() == false（无任务），有光向本机补能也应允许注入。
        tryInjectPhotosynEnergy();

        if (conditionStart() && signalAllowRun() && energyAllowRun()) {
            setActive(true);

            // ── New cycle: lock batch and maxProgress when no lock exists ──
            if (!hasLockedBatch()) {
                int B_theory = 1 + batch;
                // For EffectMachine, B is primarily constrained by energy dimension
                // (baseFePerTick from efficientIn, not multiplied by B to avoid circular dep)
                int baseFePerTick = Math.max(1, efficientIn);
                int B_byEnergy = energyStorage != null
                        ? energyStorage.getEnergyStored() / baseFePerTick : 0;
                int B_actual = Math.min(B_theory, Math.min(B_byEnergy, 19));
                B_actual = Math.max(0, B_actual);
                if (B_actual < 1) {
                    setActive(false);
                    return;
                }
                lockBatchForNewOperation(B_actual);

                // Lock maxProgress at cycle start: ceil(effectInterval * 20 * durationMultiplier)
                int mp = (int) Math.ceil(effectInterval() * 20.0 * durationMultiplier);
                maxProgress = Math.max(1, mp);
                lockMaxProgressForNewOperation(maxProgress);
            }

            int B = getLockedBatchSize();

            // ── Step 1: Expected total FE/t, calculated as long ──
            long expected = Math.round((double) getActualEfficiency() * B);

            // ── Step 2: Guard — out of valid int range → stagnate ──
            if (expected <= 0 || expected > Integer.MAX_VALUE) {
                setActive(false);
                return;
            }
            int fePerTick = (int) expected;

            // ── Step 3: Energy check — insufficient stored → pause ──
            if (energyStorage == null || energyStorage.getEnergyStored() < fePerTick) {
                setActive(false);
                return;
            }

            // ── Step 4: Output check — output full → pause (subclasses override) ──
            if (cooking()) {
                setActive(false);
                return;
            }

            // ── Step 5: Atomic energy extraction — simulate then execute ──
            if (energyStorage.extractEnergy(fePerTick, true) != fePerTick) {
                setActive(false);
                return;
            }
            if (energyStorage.extractEnergy(fePerTick, false) != fePerTick) {
                // Fail-fast: invariant violation
                throw new IllegalStateException(
                        "Energy under-extraction: expected " + fePerTick
                        + " FE but extracted less. Machine state may be inconsistent.");
            }

            // ── Step 6: Advance progress by exactly 1 tick ──
            progress++;

            // ── Step 7: Completion check — >= maxProgress ──
            if (progress >= maxProgress) {
                applyEffect();
                progress = 0;
                clearLockedBatch();
            }
        } else {
            setActive(false);
            // 条件/无配方不满足时保留当前运行态progress，等待身份/条件恢复
        }
    }

    /**
     * Whether the machine's output is full / blocked.
     * EffectMachine subclasses with output capacity (e.g. Quarry, Farm)
     * should override this to return true when output is full, causing
     * the machine to stall (pause without resetting progress).
     * Default: false (no output blocking).
     */
    public boolean cooking() {
        return false;
    }

    public abstract void applyEffect();

    public abstract double effectInterval();

    public boolean conditionStart() {
        return true;
    }
}
