package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;

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
     * 2-arg constructor for Minecraft 1.21.1 BlockEntitySupplier compatibility.
     * This is called during BlockEntityType creation; the actual type is passed
     * by the block entity type registration framework.
     * <p>
     * WARNING: This constructor leaves {@code getType()} temporarily null during
     * construction. The proper type is set either by {@link #setBlockEntityType}
     * or by the {@link BlockEntityType#create} method.
     */
    // NOP: this base class still uses 3-arg constructor.
    // Subclasses with 2-arg constructors should call super(TENBlockEntities.XXX.get(), pos, state)

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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        readTileData(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writeTileData(output);
    }

    protected void readTileData(ValueInput input) {}

    protected void writeTileData(ValueOutput output) {}
}
