package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.data.TENTags;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.UpgradeInstallHelper;
import com.modularmc.ten.common.item.upgrades.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.fluids.FluidUtil;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import org.jetbrains.annotations.Nullable;

public class BaseMachineBlock extends Block implements EntityBlock, BlockUIMenuType.BlockUI {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (type.isValid(state)) {
                BlockEntity be = type.create(pos, state);
                if (be != null) {
                    return be;
                }
            }
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
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PipeBlockEntity pipe && !pipe.hasUi()) {
            return InteractionResult.PASS;
        }
        if (be instanceof CableBlockEntity cable && !cable.hasUi()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else if (player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        // 扳手（SPANNER tag）右键：不打开机器 GUI——1.21.1 调用链 Block.useItemOn 先于 Item.useOn，
        // 若此处 CONSUME（打开 GUI）则 SpannerItem.useOn（旋转/拆解）永不执行；
        // 放行到 Item.useOn，由 SpannerItem 分流（管道连接模式切换在 CableBased.useItemOn 已处理）。
        if (stack.is(TENTags.SPANNER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 潜行+右键快捷安装：手持升级插件对准支持升级槽的机器，插入第一个空槽（无空位/不兼容则提示并阻止）
        if (be instanceof CmMachineBlockEntity machine && player.isShiftKeyDown() && stack.getItem() instanceof UpgradeItem && machine.supportsUpgradeSlots()) {
            if (level.isClientSide()) {
                return ItemInteractionResult.SUCCESS;
            }
            if (machine.upgradeHandler == null) {
                return ItemInteractionResult.CONSUME;
            }
            if (UpgradeInstallHelper.tryInstall(machine.upgradeHandler, stack, machine::validUpgrade)) {
                // 消耗手持 1 个（副本插入 + 原物 shrink 成对，防复制漏洞；创造不消耗对齐原版惯例）
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.empty()
                        .append(stack.getHoverName())
                        .append(net.minecraft.network.chat.Component.translatable("kenergyengineering.info.upgrade_successfully")), true);
            } else {
                // 无空位 → 槽满提示；兼容性拒绝（互斥/canApply/不支持） → 不支持提示；均阻止放入与开 UI
                boolean noEmptySlot = UpgradeInstallHelper.findFirstEmptySlot(machine.upgradeHandler) < 0;
                String key = noEmptySlot ? "kenergyengineering.info.too_much_upgrades" : "kenergyengineering.info.not_support_upgrade";
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (be instanceof CmMachineBlockEntity machine && !player.isShiftKeyDown() && !machine.tanks.isEmpty()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, machine.getFluidHandler(hit.getDirection()))) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        if (be instanceof PipeBlockEntity pipe && !pipe.hasUi()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (be instanceof CableBlockEntity cable && !cable.hasUi()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.sidedSuccess(true);
        } else if (player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof CmMachineBlockEntity machine) {
            return machine.createUI(holder);
        }
        if (holder.player.level().getBlockEntity(holder.pos) instanceof PipeBlockEntity pipe && pipe.hasUi()) {
            return pipe.createUI(holder);
        }
        if (holder.player.level().getBlockEntity(holder.pos) instanceof CableBlockEntity cable && cable.hasUi()) {
            return cable.createUI(holder);
        }
        return TENMachineBlockUIFactory.createFallback(holder);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CmMachineBlockEntity machine) {
                machine.dropAllContents();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }
}
