package com.modularmc.ten.lib.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RadiusMachineBlockEntity extends EffectMachineBlockEntity {

    public int radius;
    public int initialRadius;

    public RadiusMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
