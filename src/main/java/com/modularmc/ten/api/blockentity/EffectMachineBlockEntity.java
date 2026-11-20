package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 周期效果型机器基类：按 effectInterval 周期触发一次 {@code applyEffect}
 * （施加药水/伤害/冷却/收割/破坏/放置等），不走逐 tick 配方流程。
 */
public abstract class EffectMachineBlockEntity extends CmMachineBlockEntity {

    public EffectMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 效果机按 lockedB 并行处理，并行运行升级有效。 */
    @Override
    public boolean supportsBatch() {
        return true;
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

        // Syn 光合注能在所有 condition/signal/energyAllowRun 门禁之前执行。
        // 即使 conditionStart() == false（无任务），有光向本机补能也应允许注入。
        tryInjectPhotosynEnergy();

        boolean start = conditionStart();
        if (start && signalAllowRun() && energyAllowRun()) {
            setActive(true);

            // ── New cycle: lock batch and maxProgress when no lock exists ──
            if (!hasLockedBatch()) {
                int B_theory = getTheoreticalBatchSize();
                // For EffectMachine, B is primarily constrained by energy dimension
                // (baseFePerTick from efficientIn, not multiplied by B to avoid circular dep)
                int baseFePerTick = Math.max(1, efficientIn);
                int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFePerTick : 0;
                int B_actual = Math.min(B_theory, Math.min(B_byEnergy, BatchMath.B_HARD_MAX));
                B_actual = Math.max(0, B_actual);
                if (B_actual < 1) {
                    setActive(false);
                    return;
                }
                lockBatchForNewOperation(B_actual);

                // Lock maxProgress at cycle start: the cycle length in ticks, scaled by the
                // duration channel. 运行速度另按 N 推进，故耗时 = 总量 ÷ N。
                int mp = (int) Math.ceil(effectInterval() * 20.0);
                maxProgress = scaledDuration(mp);
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
                        "Energy under-extraction: expected " + fePerTick + " FE but extracted less. Machine state may be inconsistent.");
            }
            recordEnergyUse(fePerTick);

            // ── Step 6: 进度按运行速度推进（基准 1）──
            advanceProgressBySpeed();

            // ── Step 7: Completion check — >= maxProgress ──
            if (progress >= maxProgress) {
                applyEffect();
                progress = 0;
                clearLockedBatch();
            }
        } else {
            setActive(false);
            // 原料消失守卫：仅当任务条件不满足（conditionStart()==false，如输入被取出/耗尽）
            // 且信号放行时，进行中的进度作废归零——进度条立即清空，不保留半途进度。
            // 信号关闭（start 仍 true，等待恢复）与能量不足（Step 3 内 return）不受影响，
            // 仍走停机语义保留 progress 等待恢复。
            if (!start && progress > 0) {
                progress = 0;
                clearLockedBatch();
                markDirty("progress");
            }
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
