package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.wrapper.SyncedIntArray;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class CmBlockEntity extends BlockEntity {

    public SyncedIntArray data = new SyncedIntArray(40);
    public Component component;
    public String id;

    private int globalTimer = 0;
    private boolean initialised = false;

    public CmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getAliveTime() {
        return globalTimer;
    }

    public void serverTick() {
        if (level == null) return;

        if (!level.isClientSide()) {
            globalTimer++;
            if (!initialised) {
                initialised = true;
                onPlaced();
            }
            tick();
            onEndTick();
        }
        onClientTick();
    }

    protected void onEndTick() {}

    protected void onPlaced() {}

    protected void onClientTick() {}

    protected void tick() {}

    public Component getDisplayName() {
        return component;
    }

    // NBT: use HolderLookup.Provider in 1.21.1
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readTileData(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeTileData(tag, registries);
    }

    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {}
}
