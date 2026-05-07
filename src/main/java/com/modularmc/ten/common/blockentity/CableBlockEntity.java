package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class CableBlockEntity extends CmBlockEntity {

    public EnergyStorage storage = new EnergyStorage(1000, 100, 100);

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IEnergyStorage getEnergy() {
        return storage;
    }

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        int stored = storage.getEnergyStored();
        if (stored > 0) {
            storage.extractEnergy(stored, false);
        }
        storage.receiveEnergy(tag.getInt("energy"), false);
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("energy", storage.getEnergyStored());
    }
}
