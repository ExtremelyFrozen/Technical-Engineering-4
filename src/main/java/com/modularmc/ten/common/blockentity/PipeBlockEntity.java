package com.modularmc.ten.common.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class PipeBlockEntity extends CmBlockEntity {

    @Persisted
    public MachineItemHandler itemHandler = new MachineItemHandler(1);

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        itemHandler.setChangeListener(this::markDirty);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }
}
