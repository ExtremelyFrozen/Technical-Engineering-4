package com.modularmc.ten.common.item;

import com.modularmc.ten.common.data.TENTags;
import com.modularmc.ten.common.data.WrenchDismantleService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (player.isShiftKeyDown()) {
            // ── Sneak + right-click: dismantle ─────────────────────────
            if (!state.is(TENTags.WRENCH_DISMANTLEABLE)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            boolean dismantled = WrenchDismantleService.dismantle(level, pos, (ServerPlayer) player);
            return dismantled ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        // ── Right-click (no sneak): rotate ─────────────────────────────
        if (!state.is(TENTags.MACHINES)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
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
