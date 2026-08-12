package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;

import java.util.Set;

public class PipeBlockEntity extends CmBlockEntity {

    private final MachineItemHandler filterInventory = new MachineItemHandler(9);
    private final Container filterContainer = new Container() {

        @Override
        public int getContainerSize() {
            return filterInventory.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < filterInventory.getSlots(); i++) {
                if (!filterInventory.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return filterInventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return filterInventory.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = filterInventory.getStackInSlot(slot);
            filterInventory.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            filterInventory.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            markDirty();
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && !isRemoved() && player.distanceToSqr(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < filterInventory.getSlots(); i++) {
                filterInventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    };
    private final IItemHandler transportHandler = new IItemHandler() {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (level == null || stack.isEmpty() || !isItemAllowed(stack)) {
                return stack;
            }
            Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                    (lvl, pos) -> lvl.getBlockEntity(pos) instanceof PipeBlockEntity);
            ItemStack remaining = stack.copy();
            for (BlockPos pipePos : network) {
                for (Direction direction : Direction.values()) {
                    BlockPos targetPos = pipePos.relative(direction);
                    if (level.getBlockEntity(targetPos) instanceof PipeBlockEntity) {
                        continue;
                    }
                    IItemHandler sink = TransferNetworks.getItems(level, targetPos, direction.getOpposite());
                    if (sink == null) {
                        continue;
                    }
                    remaining = TransferNetworks.insertItem(sink, remaining, simulate);
                    if (remaining.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (level == null || amount <= 0) {
                return ItemStack.EMPTY;
            }
            Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                    (lvl, pos) -> lvl.getBlockEntity(pos) instanceof PipeBlockEntity);
            for (BlockPos pipePos : network) {
                for (Direction direction : Direction.values()) {
                    BlockPos targetPos = pipePos.relative(direction);
                    if (level.getBlockEntity(targetPos) instanceof PipeBlockEntity) {
                        continue;
                    }
                    IItemHandler source = TransferNetworks.getItems(level, targetPos, direction.getOpposite());
                    if (source == null) {
                        continue;
                    }
                    for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
                        ItemStack simulated = source.extractItem(sourceSlot, amount, true);
                        if (simulated.isEmpty() || !isItemAllowed(simulated)) {
                            continue;
                        }
                        return source.extractItem(sourceSlot, amount, simulate);
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemAllowed(stack);
        }
    };

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        filterInventory.setChangeListener(this::markDirty);
    }

    public IItemHandler getTransportHandler(Direction side) {
        return transportHandler;
    }

    public MachineItemHandler getFilterInventory() {
        return filterInventory;
    }

    public boolean hasUi() {
        return isFiltered();
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide() || getAliveTime() % 5 != 0) {
            return;
        }
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof PipeBlockEntity);
        if (!TransferNetworks.isRoot(network, worldPosition)) {
            return;
        }
        processNetwork(network);
    }

    private void processNetwork(Set<BlockPos> network) {
        for (BlockPos sourcePipePos : network) {
            PipeBlockEntity sourcePipe = (PipeBlockEntity) level.getBlockEntity(sourcePipePos);
            for (Direction sourceDir : Direction.values()) {
                BlockPos sourcePos = sourcePipePos.relative(sourceDir);
                if (level.getBlockEntity(sourcePos) instanceof PipeBlockEntity) {
                    continue;
                }
                IItemHandler source = TransferNetworks.getItems(level, sourcePos, sourceDir.getOpposite());
                if (source == null) {
                    continue;
                }
                for (BlockPos sinkPipePos : network) {
                    for (Direction sinkDir : Direction.values()) {
                        BlockPos sinkPos = sinkPipePos.relative(sinkDir);
                        if (sinkPos.equals(sourcePos) || level.getBlockEntity(sinkPos) instanceof PipeBlockEntity) {
                            continue;
                        }
                        IItemHandler sink = TransferNetworks.getItems(level, sinkPos, sinkDir.getOpposite());
                        if (sink == null) {
                            continue;
                        }
                        int moved = TransferNetworks.moveItems(source, sink, 1, sourcePipe::isItemAllowed, false);
                        if (moved > 0) {
                            setActive(true);
                            return;
                        }
                    }
                }
            }
        }
        setActive(false);
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.MACHINE_GUI);
        if (isFiltered()) {
            for (int i = 0; i < filterInventory.getSlots(); i++) {
                int x = 7 + (i % 9) * 18;
                int y = 35;
                root.addChild(filterSlot(i, x, y));
            }
        }
        TENMachineBlockUIFactory.addPlayerInventory(root);
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    @Override
    protected void readTileData(ValueInput input) {
        super.readTileData(input);
        // TODO: Re-enable filter inventory NBT persistence when ItemStackHandler
        // serializeNBT/deserializeNBT are restored or replaced in NeoForge 26.1.2 API.
    }

    @Override
    protected void writeTileData(ValueOutput output) {
        super.writeTileData(output);
        // TODO: Re-enable filter inventory NBT persistence.
    }

    private ItemSlot filterSlot(int index, int x, int y) {
        Slot slot = new Slot(filterContainer, index, 0, 0) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        };
        return TENMachineBlockUIFactory.itemSlotModular(slot, x, y);
    }

    private boolean isFiltered() {
        return isWhitelist() || isBlacklist();
    }

    private boolean isWhitelist() {
        return "pipe_white".equals(SafeOperationHelper.regNameOf(getBlockState().getBlock()));
    }

    private boolean isBlacklist() {
        return "pipe_black".equals(SafeOperationHelper.regNameOf(getBlockState().getBlock()));
    }

    private boolean isItemAllowed(ItemStack stack) {
        if (!isFiltered()) {
            return true;
        }
        boolean matched = false;
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack filter = filterInventory.getStackInSlot(i);
            if (!filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack)) {
                matched = true;
                break;
            }
        }
        return isWhitelist() ? matched : !matched;
    }

    private void setActive(boolean active) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE) && state.getValue(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE, active), 3);
        }
    }
}
