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

import java.util.ArrayList;
import java.util.List;
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
        if (!isNetworkRoot()) {
            return;
        }
        int moved = redistribute();
        setActive(moved > 0);
    }

    private boolean isNetworkRoot() {
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof CableBlockEntity);
        return TransferNetworks.isRoot(network, worldPosition);
    }

    private int redistribute() {
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof CableBlockEntity);
        if (network.isEmpty()) return 0;

        int rate = transferFor(getBlockState());
        int moved = 0;

        // Phase 1: Collect all sources (generators) and sinks (consumers)
        List<IEnergyStorage> sources = new ArrayList<>();
        List<IEnergyStorage> sinks = new ArrayList<>();

        for (BlockPos cablePos : network) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = cablePos.relative(dir);
                if (level.getBlockEntity(neighbor) instanceof CableBlockEntity) continue;
                IEnergyStorage cap = TransferNetworks.getEnergy(level, neighbor, dir.getOpposite());
                if (cap == null) continue;
                if (cap.canExtract() && cap.extractEnergy(1, true) > 0) {
                    sources.add(cap);
                } else if (cap.canReceive()) {
                    sinks.add(cap);
                }
            }
        }

        // Phase 2: Source -> sink direct transfer (buffer as overflow)
        for (IEnergyStorage source : sources) {
            int pulled = source.extractEnergy(rate, true);
            if (pulled <= 0) continue;

            // Try direct to sinks first
            int remaining = pulled;
            for (IEnergyStorage sink : sinks) {
                if (remaining <= 0) break;
                int accepted = sink.receiveEnergy(Math.min(remaining, rate), false);
                remaining -= accepted;
                moved += accepted;
            }

            // Overflow into cable buffer
            if (remaining > 0) {
                int intoBuffer = fillNetwork(network, remaining, true);
                if (intoBuffer > 0) {
                    int drained = source.extractEnergy(intoBuffer, false);
                    fillNetwork(network, drained, false);
                    moved += drained;
                } else if (remaining < pulled) {
                    source.extractEnergy(pulled - remaining, false);
                }
            } else {
                source.extractEnergy(pulled, false);
            }
        }

        // Phase 3: Cable buffer -> sinks (for energy left in buffer from previous cycles)
        int bufferEnergy = drainNetwork(network, Integer.MAX_VALUE, true);
        if (bufferEnergy > 0) {
            for (IEnergyStorage sink : sinks) {
                if (bufferEnergy <= 0) break;
                int fromBuffer = Math.min(bufferEnergy, rate);
                int accepted = sink.receiveEnergy(fromBuffer, true);
                if (accepted <= 0) continue;
                int actual = drainNetwork(network, accepted, false);
                if (actual > 0) {
                    sink.receiveEnergy(actual, false);
                    moved += actual;
                    bufferEnergy -= actual;
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
