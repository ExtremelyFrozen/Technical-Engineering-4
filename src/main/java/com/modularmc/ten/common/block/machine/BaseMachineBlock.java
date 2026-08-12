package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.SpannerItem;
import com.modularmc.ten.common.item.upgrades.UpgradeInstallHelper;
import com.modularmc.ten.common.item.upgrades.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
        // ── Wrench first: holding SpannerItem → rotate/dismantle, never open GUI ──
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof SpannerItem) {
            InteractionResult result = mainHand.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, mainHand, hit));
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // ── Item-first: 先尊重物品自身的右键行为 ──
        // （Spanner 旋转/拆卸、ChannelConnector 复制/应用/清空等）。
        // 仅 PASS/FAIL 回落：PASS 走默认流程；FAIL 保留既有回落
        // （BlockItem 放置失败仍可达机器 GUI，满桶放置失败仍可达 tank 交互）。
        InteractionResult itemResult = stack.useOn(new UseOnContext(level, player, hand, stack, hit));
        if (itemResult != InteractionResult.PASS && itemResult != InteractionResult.FAIL) {
            return itemResult;
        }
        BlockEntity be = level.getBlockEntity(pos);

        // ── Quick-install: shift-right-click with an UpgradeItem on an upgradable machine ──
        if (player.isShiftKeyDown() && be instanceof CmMachineBlockEntity machine && stack.getItem() instanceof UpgradeItem) {
            // Machine must have upgrade UI slots; Cell/CreativeCell/Channel return false
            if (!machine.supportsUpgradeSlots()) {
                return InteractionResult.FAIL;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            // Server-side installation
            machine.initMachine();
            boolean installed = UpgradeInstallHelper.tryInstall(
                    machine.upgradeHandler, stack,
                    machine::validUpgrade);
            if (installed) {
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                machine.setChanged();
                return InteractionResult.CONSUME;
            }
            // Installation failed (full handler or incompatible upgrade)
            return InteractionResult.FAIL;
        }

        // ── Fluid interaction (non-shift) ──
        if (be instanceof CmMachineBlockEntity machine && !player.isShiftKeyDown() && !machine.tanks.isEmpty()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, machine.getFluidHandler(hit.getDirection()))) {
                return InteractionResult.SUCCESS;
            }
        }
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
        return InteractionResult.SUCCESS;
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
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level lvl) {
            BlockEntity be = lvl.getBlockEntity(pos);
            if (be instanceof CmMachineBlockEntity machine) {
                machine.dropAllContents();
                // Prevent double-drop from CmMachineBlockEntity.setRemoved()
                // which also fires during block replacement.
                machine.markDestroyDropsHandled();
            }
        }
        super.destroy(level, pos, state);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }
}
