package com.modularmc.ten.common.block.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class HorizontalMachineBlock extends BaseMachineBlock {

    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            d -> d != Direction.UP && d != Direction.DOWN);

    public HorizontalMachineBlock() {
        super();
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    public HorizontalMachineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 右键放置：正面朝向玩家（FACING 指向玩家视线反向，同原版熔炉惯例）；
        // Shift+右键放置：背面朝向玩家（FACING = 玩家视线方向，工作区朝前延伸，便于背贴墙布置）。
        var player = context.getPlayer();
        Direction facing = player != null && player.isShiftKeyDown() ? context.getHorizontalDirection() : context.getHorizontalDirection().getOpposite();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ACTIVE, false);
    }
}
