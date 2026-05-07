package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class PipeBlockEntity extends CmBlockEntity {

    public MachineItemHandler itemHandler = new MachineItemHandler(1);

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        itemHandler.deserializeNBT(registries, tag.getCompound("items"));
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("items", itemHandler.serializeNBT(registries));
    }
}
