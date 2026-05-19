package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineEnergyStorage;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.HashSet;
import java.util.Set;

public class CableBlockEntity extends CmBlockEntity {

    public MachineEnergyStorage storage;

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.storage = new MachineEnergyStorage(capacityFor(state), transferFor(state), transferFor(state));
        this.storage.setChangeListener(this::markDirty);
    }

    public IEnergyStorage getEnergy(Direction side) {
        return storage;
    }

    public boolean hasUi() {
        return false;
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide() || getAliveTime() % 5 != 0) {
            return;
        }
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof CableBlockEntity);
        if (!TransferNetworks.isRoot(network, worldPosition)) {
            return;
        }
        Set<BlockPos> pulledFrom = new HashSet<>();
        int moved = pullIntoNetwork(network, pulledFrom);
        moved += pushOutOfNetwork(network, pulledFrom);
        setActive(moved > 0);
    }

    private int pullIntoNetwork(Set<BlockPos> network, Set<BlockPos> pulledFrom) {
        int moved = 0;
        for (BlockPos cablePos : network) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(direction);
                if (level.getBlockEntity(neighborPos) instanceof CableBlockEntity) {
                    continue;
                }
                IEnergyStorage source = TransferNetworks.getEnergy(level, neighborPos, direction.getOpposite());
                if (source == null || !source.canExtract()) {
                    continue;
                }
                int accepted = fillNetwork(network, Math.min(transferFor(getBlockState()), source.extractEnergy(Integer.MAX_VALUE, true)), true);
                if (accepted > 0) {
                    int drained = source.extractEnergy(accepted, false);
                    moved += fillNetwork(network, drained, false);
                    if (drained > 0) {
                        pulledFrom.add(neighborPos);
                    }
                }
            }
        }
        return moved;
    }

    private int pushOutOfNetwork(Set<BlockPos> network, Set<BlockPos> pulledFrom) {
        int moved = 0;
        for (BlockPos cablePos : network) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(direction);
                if (level.getBlockEntity(neighborPos) instanceof CableBlockEntity || pulledFrom.contains(neighborPos)) {
                    continue;
                }
                IEnergyStorage sink = TransferNetworks.getEnergy(level, neighborPos, direction.getOpposite());
                if (sink == null || !sink.canReceive()) {
                    continue;
                }
                int drained = drainNetwork(network, transferFor(getBlockState()), true);
                if (drained <= 0) {
                    continue;
                }
                int accepted = sink.receiveEnergy(drained, false);
                if (accepted > 0) {
                    moved += drainNetwork(network, accepted, false);
                }
            }
        }
        return moved;
    }

    private int fillNetwork(Set<BlockPos> network, int amount, boolean simulate) {
        int remaining = amount;
        for (BlockPos pos : network) {
            if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
                continue;
            }
            int received = cable.storage.receiveEnergy(remaining, simulate);
            remaining -= received;
            if (remaining <= 0) {
                break;
            }
        }
        return amount - remaining;
    }

    private int drainNetwork(Set<BlockPos> network, int amount, boolean simulate) {
        int remaining = amount;
        for (BlockPos pos : network) {
            if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
                continue;
            }
            int extracted = cable.storage.extractEnergy(remaining, simulate);
            remaining -= extracted;
            if (remaining <= 0) {
                break;
            }
        }
        return amount - remaining;
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.GUI_HANDLER);
        root.addChild(label(8, 8, "Energy Cable"));
        root.addChild(label(8, 20, "Stored: " + storage.getEnergyStored() + " FE"));
        root.addChild(label(8, 32, "Rate: " + transferFor(getBlockState()) + " FE/t"));
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        storage.setEnergy(tag.getInt("energy"));
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("energy", storage.getEnergyStored());
    }

    private static int capacityFor(BlockState state) {
        String name = SafeOperationHelper.regNameOf(state.getBlock());
        if ("cable_star".equals(name)) {
            return Integer.MAX_VALUE;
        }
        if ("cable_azure".equals(name)) {
            return 50_000;
        }
        if ("cable_quartz".equals(name)) {
            return 20_000;
        }
        return 1_000;
    }

    private static int transferFor(BlockState state) {
        String name = SafeOperationHelper.regNameOf(state.getBlock());
        if ("cable_star".equals(name)) {
            return 200_000;
        }
        if ("cable_azure".equals(name)) {
            return 4_000;
        }
        if ("cable_quartz".equals(name)) {
            return 1_000;
        }
        return 200;
    }

    private static Label label(int x, int y, String text) {
        Label label = new Label();
        label.setText(Component.literal(text));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
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
