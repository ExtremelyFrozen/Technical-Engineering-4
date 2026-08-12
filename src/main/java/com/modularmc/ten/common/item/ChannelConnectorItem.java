package com.modularmc.ten.common.item;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity;
import com.modularmc.ten.common.data.TENDataComponents;
import com.modularmc.ten.common.data.TENDataComponents.ChannelConfigComponent;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 频道连接器：频道方块面配置的复制/应用工具。
 * <p>
 * 数据经自定义 DataComponent {@link ChannelConfigComponent} 存储
 * （26.1.2 规范，禁止 NBT/CompoundTag 物品栈存储）：
 * <ul>
 * <li>潜行右键频道方块且未持有组件 → <b>复制</b>：读源 BE 面配置 Maps + 频道名，存入组件</li>
 * <li>潜行右键频道方块且持有组件 → <b>应用</b>：写目标 BE 面配置 Maps + 同步客户端
 * int[6] DescSynced 数组 + 推送同步；channelId 非空则目标 join（失败捕获提示不崩溃）</li>
 * <li>潜行右键空气/非频道方块 → <b>清空</b>组件</li>
 * <li>非潜行右键 → PASS（不拦截 GUI 等原交互）</li>
 * </ul>
 */
public class ChannelConnectorItem extends TENBaseItem {

    public ChannelConnectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        // 非潜行 → PASS：不拦截频道方块 GUI 等原交互
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());

        // 潜行右键非频道方块（含空气）→ 清空组件
        if (!(be instanceof AbstractChannelBlockEntity channel)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            stack.remove(TENDataComponents.CHANNEL_CONFIG);
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GREEN,
                    ComponentHelper.getKey("channel_connector.cleared")));
            return InteractionResult.CONSUME;
        }

        // 潜行右键频道方块且未持有组件 → 复制
        if (!stack.has(TENDataComponents.CHANNEL_CONFIG)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            stack.set(TENDataComponents.CHANNEL_CONFIG, copyFrom(channel));
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GREEN,
                    ComponentHelper.getKey("channel_connector.copied")));
            return InteractionResult.CONSUME;
        }

        // 潜行右键频道方块且持有组件 → 应用
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ChannelConfigComponent config = stack.get(TENDataComponents.CHANNEL_CONFIG);
        if (config != null) {
            boolean applied = applyTo(channel, config);
            if (!applied) {
                // 面配置数据非法（size!=6 或含越界值）→ 不写入，提示不崩溃
                player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.RED,
                        ComponentHelper.getKey("channel_connector.invalid_config")));
                return InteractionResult.CONSUME;
            }
            // 面配置已写入；channelId 非空则目标 join（失败捕获提示不崩溃）
            boolean joined = true;
            if (config.channelId() != null && !config.channelId().isEmpty()) {
                try {
                    joined = channel.join(config.channelId());
                } catch (RuntimeException ex) {
                    joined = false;
                }
            }
            if (!joined) {
                player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.RED,
                        ComponentHelper.getKey("channel_connector.applied_join_failed")));
            } else {
                player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GREEN,
                        ComponentHelper.getKey("channel_connector.applied")));
            }
        }
        return InteractionResult.CONSUME;
    }

    /** 潜行右键空气：清空组件（useOn 不覆盖空气点击，需在 use() 兜底）。非潜行不拦截。 */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(TENDataComponents.CHANNEL_CONFIG)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            stack.remove(TENDataComponents.CHANNEL_CONFIG);
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GREEN,
                    ComponentHelper.getKey("channel_connector.cleared")));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /**
     * 复制：读源 BE 面配置 Maps（energyFaceMode/itemFaceMode/fluidFaceMode 按
     * Direction 序转 int[6]）+ 当前频道名，组装组件载荷。
     */
    private static ChannelConfigComponent copyFrom(AbstractChannelBlockEntity source) {
        List<Integer> energy = new ArrayList<>(6);
        List<Integer> item = new ArrayList<>(6);
        List<Integer> fluid = new ArrayList<>(6);
        for (Direction d : Direction.values()) {
            int idx = d.get3DDataValue();
            energy.add(source.energyFaceMode.getOrDefault(d, source.initialFaceModeEnergy()));
            item.add(source.itemFaceMode.getOrDefault(d, source.initialFaceModeItem()));
            fluid.add(source.fluidFaceMode.getOrDefault(d, source.initialFaceModeFluid()));
        }
        return new ChannelConfigComponent(energy, item, fluid, source.joinedChannelName());
    }

    /**
     * 应用：入口校验三组面模式列表（每组 size==6 且每值 0<=v&lt;{@link FaceOption#size()}），
     * 全部合法才写入（避免部分写入/IndexOutOfBounds），不合法返回 false。
     * 写入目标 BE 面配置 Maps（更新 + 同步客户端 int[6] DescSynced 数组 +
     * markDirty/setChanged + 推送同步，参考 {@code rpcCycleFaceMode} 模式）。
     *
     * @return true 表示校验通过且已写入
     */
    private static boolean applyTo(AbstractChannelBlockEntity target, ChannelConfigComponent config) {
        if (!isValidFaceList(config.energyFaces()) || !isValidFaceList(config.itemFaces()) || !isValidFaceList(config.fluidFaces())) {
            return false;
        }
        for (Direction d : Direction.values()) {
            int idx = d.get3DDataValue();
            int energyMode = config.energyFaces().get(idx);
            int itemMode = config.itemFaces().get(idx);
            int fluidMode = config.fluidFaces().get(idx);
            target.energyFaceMode.put(d, energyMode);
            target.itemFaceMode.put(d, itemMode);
            target.fluidFaceMode.put(d, fluidMode);
            // 同步客户端 int[6] DescSynced 数组（rpcSyncFaceInfo 客户端写入）
            target.energyFaceData[idx] = energyMode;
            target.itemFaceData[idx] = itemMode;
            target.fluidFaceData[idx] = fluidMode;
            // 推送同步到 tracking 玩家
            target.rpcToTracking("rpcSyncFaceInfo", idx, energyMode, itemMode, fluidMode);
        }
        target.setChanged();
        return true;
    }

    /** 入口校验：面模式列表必须为固定 6 项（Direction 序）且每值在 FaceOption 合法区间。 */
    private static boolean isValidFaceList(List<Integer> faces) {
        if (faces == null || faces.size() != 6) {
            return false;
        }
        for (Integer v : faces) {
            if (v == null || v < 0 || v >= FaceOption.size()) {
                return false;
            }
        }
        return true;
    }
}
