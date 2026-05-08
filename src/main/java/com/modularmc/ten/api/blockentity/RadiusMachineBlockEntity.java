package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RadiusMachineBlockEntity extends EffectMachineBlockEntity {

    public int radius;
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
}
