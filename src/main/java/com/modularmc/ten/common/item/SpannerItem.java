package com.modularmc.ten.common.item;

import com.modularmc.ten.common.data.TENTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class SpannerItem extends TENBaseItem {

    public SpannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(TENTags.MACHINES)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            // Shift + right-click: silk-touch break to inventory
            Block block = state.getBlock();
            ItemStack drop = new ItemStack(block);
            level.destroyBlock(pos, false, player);
            if (!player.addItem(drop)) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
            }
            return InteractionResult.CONSUME;
        }

        // Right-click: rotate clockwise
        rotateMachine(level, pos, state);
        return InteractionResult.CONSUME;
    }

    private void rotateMachine(Level level, BlockPos pos, BlockState state) {
        EnumProperty<Direction> prop = findFacingProperty(state);
        if (prop == null) return;

        Direction current = state.getValue(prop);
        boolean isAllDir = prop.getPossibleValues().contains(Direction.UP) && prop.getPossibleValues().contains(Direction.DOWN);
        Direction next = isAllDir ? rotate6(current) : current.getClockWise();

        level.setBlock(pos, state.setValue(prop, next), 3);
    }

    private static Direction rotate6(Direction dir) {
        return switch (dir) {
            case DOWN -> Direction.UP;
            case UP -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.DOWN;
        };
    }

    private static EnumProperty<Direction> findFacingProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return BlockStateProperties.FACING;
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return BlockStateProperties.HORIZONTAL_FACING;
        }
        for (var prop : state.getProperties()) {
            if (prop instanceof EnumProperty<?> ep && ep.getName().equals("facing") && ep.getPossibleValues().stream().allMatch(v -> v instanceof Direction)) {
                @SuppressWarnings("unchecked")
                EnumProperty<Direction> dp = (EnumProperty<Direction>) ep;
                return dp;
            }
        }
        return null;
    }
}
