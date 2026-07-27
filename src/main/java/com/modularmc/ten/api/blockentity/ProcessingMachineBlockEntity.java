package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ProcessingMachineBlockEntity extends CmMachineBlockEntity {

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

        // P2-T7: Syn光合注能在所有condition/signal/energyAllowRun门禁之前执行。
        // 即使 conditionStart() == false（无任务），有光向本机补能也应允许注入。
        tryInjectPhotosynEnergy();

        if (conditionStart() && signalAllowRun() && energyAllowRun()) {
            setActive(true);

            // ── Step 1: Expected total FE/t, calculated as long ──
            long expected = Math.round((double) getActualEfficiency() * getLockedBatchSize());

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

            // ── Step 4: Output check — output full → pause ──
            if (cooking()) {
                setActive(false);
                return;
            }

            // ── Step 5: Atomic energy extraction — simulate then execute ──
            // Simulate: verify full expected amount can be extracted
            if (energyStorage.extractEnergy(fePerTick, true) != fePerTick) {
                // Cannot extract full amount (maxExtract or stored insufficient)
                setActive(false);
                return;
            }
            // Execute: real extraction must match expected in single-thread context
            if (energyStorage.extractEnergy(fePerTick, false) != fePerTick) {
                // Fail-fast: invariant violation — can't silently under-deduct
                throw new IllegalStateException(
                        "Energy under-extraction: expected " + fePerTick
                        + " FE but extracted less. Machine state may be inconsistent.");
            }

            // ── Step 6: Advance progress by exactly 1 tick ──
            progress++;

            // Per-tick processing hook — called only when this tick actually
            // processes (energy consumed, progress advanced). Subclasses override
            // for periodic logic such as catalyst consumption (Condenser).
            onProcessTick();

            // ── Step 7: Completion check — >= maxProgress ──
            if (progress >= maxProgress) {
                onCookFinish();
                progress = 0;
            }
        } else {
            setActive(false);
            // 条件/无配方不满足时保留当前运行态progress，等待身份/条件恢复
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
     * Called every tick after progress advancement and energy consumption,
     * but before the completion check. Subclasses can override for per-tick
     * processing logic such as catalyst consumption (Condenser).
     * <p>
     * Only called when the tick actually processes (energy consumed,
     * progress advanced). NOT called when the machine is stalled
     * (energy insufficient, output full, or conditions not met).
     */
    protected void onProcessTick() {
        // Default: no-op. Subclasses override as needed.
    }
}
