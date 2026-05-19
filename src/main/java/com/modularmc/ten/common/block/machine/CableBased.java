package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Map;

public class CableBased extends HorizontalMachineBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, IntegerProperty> CONNECTION = Map.of(
            Direction.NORTH, IntegerProperty.create("north", 0, 2),
            Direction.EAST, IntegerProperty.create("east", 0, 2),
            Direction.SOUTH, IntegerProperty.create("south", 0, 2),
            Direction.WEST, IntegerProperty.create("west", 0, 2),
            Direction.UP, IntegerProperty.create("up", 0, 2),
            Direction.DOWN, IntegerProperty.create("down", 0, 2));

    protected static final VoxelShape CORE_SHAPE = Block.box(3, 3, 3, 13, 13, 13);

    public CableBased(Properties props) {
        super(props);
    }

    public CableBased() {
        this(Properties.of()
                .mapColor(MapColor.NONE)
                .strength(1, 3)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isViewBlocking((s, l, p) -> false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
        CONNECTION.values().forEach(builder::add);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return CORE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return CORE_SHAPE;
    }

    public void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTION.get(dir), getConnectionType(level, pos, dir));
        }
        level.setBlock(pos, state, 3);
    }

    protected int getConnectionType(Level level, BlockPos pos, Direction dir) {
        BlockPos neighbor = pos.relative(dir);
        BlockEntity be = level.getBlockEntity(pos);
        BlockEntity neighborBe = level.getBlockEntity(neighbor);

        if (be == null || neighborBe == null) return 0;

        boolean isPipe = be instanceof PipeBlockEntity;
        boolean isCable = be instanceof CableBlockEntity;
        if (isPipe && neighborBe instanceof PipeBlockEntity) {
            return 1;
        }
        if (isCable && neighborBe instanceof CableBlockEntity) {
            return 1;
        }
        if (isPipe && level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, dir.getOpposite()) != null) {
            return 2;
        }
        if (isCable && level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite()) != null) {
            return 2;
        }
        if (neighborBe instanceof CmMachineBlockEntity) {
            return 2;
        }
        return 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState();
        state = state.setValue(ACTIVE, false)
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTION.get(dir), 0);
        }
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moved);
        if (!level.isClientSide()) {
            updateConnections(level, pos);
        }
    }
}
