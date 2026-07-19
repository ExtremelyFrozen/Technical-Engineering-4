package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

public abstract class RadiusMachineBlockEntity extends EffectMachineBlockEntity {

    @Persisted
    @DescSynced
    public int radius;

    @Persisted
    @DescSynced
    public int initialRadius;

    public RadiusMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isType(String type) {
        if ("MACHINE_EFFECT".equals(type)) {
            return true;
        }
        return super.isType(type);
    }

    @Override
    public int getCurrentRadius() {
        return radius;
    }

    @Override
    public void setCurrentRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public int getInitialRadius() {
        return initialRadius;
    }

    /**
     * Reset radius to initialRadius before each upgrade apply cycle,
     * preventing {@link com.modularmc.ten.common.item.upgrades.LevelupRg#effect}
     * from accumulating radius across ticks.
     * <p>
     * Called by {@link CmMachineBlockEntity#doBaseData()} before the single
     * {@link CmMachineBlockEntity#applyUpgradeEffects()} call.
     */
    @Override
    protected void resetUpgradeEffects() {
        super.resetUpgradeEffects();
        this.radius = this.initialRadius;
    }
}
