package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;

/**
 * 全部方块实体的同步/持久化基座：桥接 LDLib2 的字段同步（FieldManagedStorage）、
 * NBT 持久化与 RPC 通道，并提供主线程守卫的被动同步（passivelySync）。
 */
public abstract class CmBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    public Component component;
    public String id;

    private int globalTimer = 0;
    private boolean initialised = false;

    public CmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 禁用异步 sync 线程，避免 @DescSynced 轮询与主线程写字段竞争。
     * 关闭后必须在主线程 tick 末尾调用 passivelySync() 驱动增量同步。
     */
    @Override
    public boolean useAsyncThread() {
        return false;
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
            // 主线程统一同步：字段全部写完后一次性推送脏字段。
            passivelySync();
        }
        onClientTick();
    }

    protected void onEndTick() {}

    protected void onPlaced() {}

    protected void onClientTick() {}

    protected void tick() {}

    protected void markDirty() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public Component getDisplayName() {
        return component;
    }

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
