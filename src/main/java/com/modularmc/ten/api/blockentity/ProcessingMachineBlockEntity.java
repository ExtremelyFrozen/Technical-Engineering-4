package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 逐 tick 加工机器基类：实现 process() 主循环（门禁检查 → Syn 注能 →
 * maxProgress 锁定 → 能量扣减 → 进度推进 → 完成钩子）与停机语义，
 * 配方查找/产出等细节留给子类。
 */
public abstract class ProcessingMachineBlockEntity extends CmMachineBlockEntity {

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 加工机按 lockedB 并行处理，并行运行升级有效（自带并行槽位的子类可覆写回 false）。 */
    @Override
    public boolean supportsBatch() {
        return true;
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_PROCESS;
    }

    /**
     * Base processing time in ticks at 1 FE/t energy input.
     * Higher energy input reduces processing time proportionally.
     */
    public abstract int baseTickTime();

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

            // ── Step 1: 期望总 FE/t（fePerTick = 实际效率 × 锁定并行运行 B）──
            long expected = Math.round((double) getActualEfficiency() * getLockedBatchSize());

            // ── Step 2: 越界保护——超出 int 范围 → 停机 ──
            if (expected <= 0 || expected > Integer.MAX_VALUE) {
                setActive(false);
                return;
            }
            int fePerTick = (int) expected;

            // ── Step 3: 能量不足 → 暂停（保留 progress 等待恢复）──
            if (energyStorage == null || energyStorage.getEnergyStored() < fePerTick) {
                setActive(false);
                return;
            }

            // ── Step 4: 输出满 → 暂停 ──
            if (cooking()) {
                setActive(false);
                return;
            }

            // ── Step 4.5: maxProgress 兜底锁定 ──
            // 总量 = 配方耗时 × 耗时倍率（工序精简），速度另按 N 推进：耗时 = 总量 ÷ N。
            if (!hasLockedMaxProgress()) {
                maxProgress = scaledDuration(baseTickTime());
                lockMaxProgressForNewOperation(maxProgress);
            }

            // ── Step 5: 原子能量扣减——simulate 验证全量可提取 ──
            // Simulate: 验证期望全量可提取（maxExtract 或储能不足时返回不足）
            if (energyStorage.extractEnergy(fePerTick, true) != fePerTick) {
                setActive(false);
                return;
            }
            // Execute: 真实提取必须与期望一致（单线程上下文）
            if (energyStorage.extractEnergy(fePerTick, false) != fePerTick) {
                // Fail-fast: 不变量被破坏——不能静默少扣（机器状态可能不一致）
                throw new IllegalStateException(
                        "Energy under-extraction: expected " + fePerTick + " FE but extracted less. Machine state may be inconsistent.");
            }
            recordEnergyUse(fePerTick);

            // ── Step 6: 进度按运行速度推进（基准 1，可被升级与全局倍率调整）──
            advanceProgressBySpeed();

            // 每 tick 处理钩子——仅在真正处理（能量消耗、进度推进）时调用
            onProcessTick();

            // ── Step 7: 完成判断——>= maxProgress ──
            if (progress >= maxProgress) {
                onCookFinish();
                // 完成后清并行运行锁（lockedB + lockedMaxProgress），下周期重新锁定
                clearLockedBatch();
                progress = 0;
            }
        } else {
            setActive(false);
            // 原料消失守卫（对齐 EffectMachineBlockEntity）：仅当任务条件不满足
            // （conditionStart()==false，如输入被取出/配方失配）且信号/能量未阻断时，
            // 进行中的进度作废归零——进度条立即清空，不保留半途进度。
            // 信号关闭/能量不足/输出满时 start 仍 true（或早于 else 提前 return），
            // 不进此分支，仍走停机语义保留 progress 等待恢复。
            if (!start && progress > 0) {
                progress = 0;
                clearLockedBatch();
                markDirty("progress");
            }
        }
    }

    public boolean cooking() {
        return false;
    }

    public void onCookFinish() {}

    public boolean conditionStart() {
        return true;
    }

    /**
     * 每 tick 处理钩子：仅在真正处理（能量消耗、进度推进）时调用，
     * 完成判断之前执行。子类可覆盖用于周期逻辑（如冷凝器催化剂消耗）。
     * 停机（能量不足/输出满/条件不满足）时不调用。
     */
    protected void onProcessTick() {
        // Default: no-op. Subclasses override as needed.
    }
}
