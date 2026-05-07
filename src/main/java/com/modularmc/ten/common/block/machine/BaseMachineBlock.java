package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class BaseMachineBlock extends Block implements EntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    @Nullable
    protected Supplier<BlockEntityType<?>> blockEntityType;

    public BaseMachineBlock() {
        this(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3, 5)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(ACTIVE) ? 6 : 0));
    }

    public BaseMachineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    public BaseMachineBlock beType(Supplier<BlockEntityType<?>> type) {
        this.blockEntityType = type;
        return this;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (blockEntityType != null && blockEntityType.get() != null) {
            return blockEntityType.get().create(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof CmBlockEntity cm) {
                cm.serverTick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CmMachineBlockEntity machine) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new com.modularmc.ten.client.gui.CmContainerMachine(null, id, inv, machine, pos),
                        machine.getDisplayName()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CmMachineBlockEntity machine) {
                for (int i = 0; i < machine.itemHandler.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), machine.itemHandler.getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }
}
