package com.modularmc.ten.common.item;

import com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.ItemNBTHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ChannelConnectorItem extends TENBaseItem implements IModeChangable {

    private static final String TAG_MODE = "mode";
    private static final String TAG_HAS_LAST = "hasLast";
    private static final String TAG_LAST = "last";

    public ChannelConnectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void change(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (player.isShiftKeyDown()) {
            clearSelection(stack);
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GOLD, ComponentHelper.getKey("channel.pointer_last"))
                    .append(ComponentHelper.make(ChatFormatting.RED, "cleared")));
        } else {
            int next = (ItemNBTHelper.getTag(stack, TAG_MODE) + 1) % Mode.size();
            ItemNBTHelper.setTag(stack, TAG_MODE, next);
            player.sendSystemMessage(modeComponent(stack));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof AbstractChannelBlockEntity target)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!hasSelection(stack)) {
            setSelection(stack, context.getClickedPos());
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GOLD, ComponentHelper.getKey("channel.first_click"))
                    .append(ComponentHelper.make(ChatFormatting.GREEN, formatPos(context.getClickedPos()))));
            return InteractionResult.CONSUME;
        }

        BlockPos selectedPos = getSelection(stack);
        clearSelection(stack);

        if (!(level.getBlockEntity(selectedPos) instanceof AbstractChannelBlockEntity source) || !source.sameChannelType(target)) {
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.RED, ComponentHelper.getKey("channel.not_found"))
                    .append(ComponentHelper.make(ChatFormatting.RED, formatPos(selectedPos))));
            return InteractionResult.CONSUME;
        }

        Mode mode = Mode.parse(stack);
        boolean changed = switch (mode) {
            case OUTPUT -> source.linkOut(target.getBlockPos());
            case INPUT -> source.linkIn(target.getBlockPos());
            case REMOVE -> source.unlink(target.getBlockPos());
        };

        if (changed) {
            String prefix = mode == Mode.REMOVE ? ComponentHelper.getKey("channel.remove") : ComponentHelper.getKey("channel.bind");
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GREEN, prefix)
                    .append(ComponentHelper.make(ChatFormatting.AQUA, formatPos(selectedPos)))
                    .append(ComponentHelper.translated(ComponentHelper.getKey("channel.to")))
                    .append(ComponentHelper.make(ChatFormatting.AQUA, formatPos(target.getBlockPos()))));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            change(player);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean hasSelection(ItemStack stack) {
        return dataTag(stack).getBoolean(TAG_HAS_LAST).orElse(false);
    }

    private static void clearSelection(ItemStack stack) {
        CompoundTag tag = dataTag(stack);
        tag.putBoolean(TAG_HAS_LAST, false);
        tag.remove(TAG_LAST);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void setSelection(ItemStack stack, BlockPos pos) {
        CompoundTag tag = dataTag(stack);
        tag.putBoolean(TAG_HAS_LAST, true);
        tag.putLong(TAG_LAST, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static BlockPos getSelection(ItemStack stack) {
        CompoundTag tag = dataTag(stack);
        return tag.contains(TAG_LAST) ? BlockPos.of(tag.getLong(TAG_LAST).orElse(0L)) : BlockPos.ZERO;
    }

    private static CompoundTag dataTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static Component modeComponent(ItemStack stack) {
        String key = switch (Mode.parse(stack)) {
            case INPUT -> "channel_connector.mode.in";
            case OUTPUT -> "channel_connector.mode.out";
            case REMOVE -> "channel_connector.mode.rem";
        };
        return ComponentHelper.translated(ChatFormatting.GOLD, ComponentHelper.getKey(key));
    }

    public enum Mode {

        OUTPUT,
        INPUT,
        REMOVE;

        public static int size() {
            return values().length;
        }

        public static Mode parse(ItemStack stack) {
            int mode = ItemNBTHelper.getTag(stack, TAG_MODE);
            if (mode < 0 || mode >= values().length) {
                return OUTPUT;
            }
            return values()[mode];
        }
    }
}
