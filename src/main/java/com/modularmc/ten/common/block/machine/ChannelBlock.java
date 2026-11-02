package com.modularmc.ten.common.block.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * 频道方块（末影箱模式）：定制 VoxelShape 对齐 channel.json 模型元素。
 * <p>
 * 形状旋转由 {@link #FACING}（DirectionalMachineBlock）驱动，静态表按六个朝向预计算。
 */
public class ChannelBlock extends DirectionalMachineBlock {

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Rotations derived from channel/channel.json elements (2 elements, both angle=0)
        // Blockstate rotations: y=90/180/270, x=90/270 around block center [8,8,8]

        // NORTH: no rotation — elements as-is from model
        SHAPES.put(Direction.NORTH, Shapes.or(
                Block.box(1, 1, 2, 15, 15, 3),
                Block.box(2, 2, -1, 14, 14, 2)));

        // SOUTH: y=180 — x' = 16-x, z' = 16-z
        SHAPES.put(Direction.SOUTH, Shapes.or(
                Block.box(1, 1, 13, 15, 15, 14),
                Block.box(2, 2, 14, 14, 14, 17)));

        // EAST: y=90 — x' = 16-z, z' = x
        SHAPES.put(Direction.EAST, Shapes.or(
                Block.box(13, 1, 1, 14, 15, 15),
                Block.box(14, 2, 2, 17, 14, 14)));

        // WEST: y=270 — x' = z, z' = 16-x
        SHAPES.put(Direction.WEST, Shapes.or(
                Block.box(2, 1, 1, 3, 15, 15),
                Block.box(-1, 2, 2, 2, 14, 14)));

        // UP: x=270 — y' = 16-z, z' = y (applied after Y=0)
        SHAPES.put(Direction.UP, Shapes.or(
                Block.box(1, 13, 1, 15, 14, 15),
                Block.box(2, 14, 2, 14, 17, 14)));

        // DOWN: x=90 — y' = z, z' = 16-y (applied after Y=0)
        SHAPES.put(Direction.DOWN, Shapes.or(
                Block.box(1, 2, 1, 15, 3, 15),
                Block.box(2, -1, 2, 14, 2, 14)));
    }

    public ChannelBlock() {
        super();
    }

    public ChannelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
