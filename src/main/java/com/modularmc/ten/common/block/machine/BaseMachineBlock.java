package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.common.data.TENMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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

import org.jetbrains.annotations.Nullable;

public class BaseMachineBlock extends Block implements EntityBlock {

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
                if (be != null) return be;
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
        TEN.LOGGER.info("[TEN-GUI] useWithoutItem called at {} side={} hand=EMPTY", pos, level.isClientSide() ? "CLIENT" : "SERVER");
        if (!level.isClientSide()) {
            openGui(level, pos, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        TEN.LOGGER.info("[TEN-GUI] useItemOn called at {} side={} hand={} item={}", pos, level.isClientSide() ? "CLIENT" : "SERVER", hand, stack.getItem());
        if (!level.isClientSide()) {
            openGui(level, pos, player);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private void openGui(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        TEN.LOGGER.info("[TEN-GUI] openGui BE at {} = {}", pos, be != null ? be.getClass().getSimpleName() : "null");
        if (be instanceof CmMachineBlockEntity machine && player instanceof ServerPlayer sp) {
            var menuType = TENMenuTypes.MACHINE.get();
            int mType = machine.machineType();
            int slots = machine.itemHandler != null ? machine.itemHandler.getSlots() : 0;
            TEN.LOGGER.info("[TEN-GUI] openGui machine={} menuType={} itemHandler={} displayName={}",
                    machine.getClass().getSimpleName(),
                    menuType != null ? "OK" : "NULL", slots,
                    machine.getDisplayName().getString());
            sp.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> {
                        TEN.LOGGER.info("[TEN-GUI] Creating CmContainerMachine id={} player={}", id, p.getGameProfile().getName());
                        return new CmContainerMachine(
                                menuType, id, inv, machine, pos);
                    },
                    machine.getDisplayName()),
                    buf -> {
                        buf.writeInt(mType);
                        buf.writeInt(slots);
                    });
        } else {
            TEN.LOGGER.warn("[TEN-GUI] BE is not CmMachineBlockEntity: {}", be != null ? be.getClass().getName() : "null");
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CmMachineBlockEntity machine) {
                if (machine.itemHandler != null) {
                    for (int i = 0; i < machine.itemHandler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), machine.itemHandler.getStackInSlot(i));
                    }
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }
}
