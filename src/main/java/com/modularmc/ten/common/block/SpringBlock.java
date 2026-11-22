package com.modularmc.ten.common.block;

import com.modularmc.ten.common.blockentity.SpringBlockEntity;
import com.modularmc.ten.common.data.TENBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SpringBlock extends Block implements EntityBlock {
    public static final BooleanProperty FACING_AIR = BooleanProperty.create("facing_air");
    private final Supplier<Fluid> fluid;

    public SpringBlock(Properties properties, Supplier<Fluid> fluid) {
        super(properties);
        this.fluid = fluid;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING_AIR);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return validBlockState(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return validBlockState(state, level, pos);
    }

    private static BlockState validBlockState(BlockState state, LevelReader level, BlockPos pos) {
        return state.setValue(FACING_AIR, level.getBlockState(pos.above()).isEmpty());
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpringBlockEntity(TENBlockEntities.SPRING.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof SpringBlockEntity cm) {
                cm.serverTick();
            }
        };
    }

    public Fluid getFluid(){
        return fluid.get();
    }
}
