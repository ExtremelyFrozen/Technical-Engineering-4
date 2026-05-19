package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.TransferNetworks;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class ChannelItemBlockEntity extends AbstractChannelBlockEntity {

    public ChannelItemBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int inventorySize() {
        return 4;
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.BOTH;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasFaceCapabilityItem(Direction side) {
        return side == null || side == getFacing();
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildChannelUI(holder, TEN.id("textures/gui/channel_item.png"), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 61, 28));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 79, 28));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 2, 61, 46));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 3, 79, 46));
            root.addChild(label(44, 82, "Wireless item relay"));
        });
    }

    @Override
    public void tick() {
        doBaseData();
        if (!signalAllowRun() || itemHandler == null) {
            setActive(false);
            return;
        }

        int moved = 0;
        var input = nextRoundRobin(resolveInputs(), true);
        if (input != null && input.itemHandler != null) {
            moved += TransferNetworks.moveItems(input.itemHandler, itemHandler, maxReceiveItem, stack -> true, false);
        }
        var output = nextRoundRobin(resolveOutputs(), false);
        if (output != null && output.itemHandler != null) {
            moved += TransferNetworks.moveItems(itemHandler, output.itemHandler, maxExtractItem, stack -> true, false);
        }
        setActive(moved > 0);
    }
}
