package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

public class CableBlockEntity extends CmBlockEntity {

    @Persisted
    public EnergyStorage storage = new EnergyStorage(1000, 100, 100);

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IEnergyStorage getEnergy() {
        return storage;
    }
}
