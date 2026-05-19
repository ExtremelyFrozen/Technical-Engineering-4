package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractChannelBlockEntity extends CmMachineBlockEntity {

    protected final List<BlockPos> outputs = new ArrayList<>();
    protected final List<BlockPos> inputs = new ArrayList<>();
    protected int currentOutputIndex;
    protected int currentInputIndex;

    public AbstractChannelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setEfficiency(0);
    }

    @Override
    public boolean hasUpgrade() {
        return false;
    }

    @Override
    public boolean hasSideBar() {
        return false;
    }

    @Override
    public int initialFaceModeEnergy() {
        return 3;
    }

    @Override
    public int initialFaceModeItem() {
        return 3;
    }

    @Override
    public int initialFaceModeFluid() {
        return 3;
    }

    public boolean sameChannelType(AbstractChannelBlockEntity other) {
        return other != null && other.getClass() == getClass();
    }

    public boolean linkOut(BlockPos pos) {
        if (outputs.contains(pos) || pos.equals(getBlockPos())) {
            return false;
        }
        outputs.add(pos);
        currentOutputIndex = 0;
        markDirty();
        return true;
    }

    public boolean linkIn(BlockPos pos) {
        if (inputs.contains(pos) || pos.equals(getBlockPos())) {
            return false;
        }
        inputs.add(pos);
        currentInputIndex = 0;
        markDirty();
        return true;
    }

    public boolean unlink(BlockPos pos) {
        boolean changed = outputs.remove(pos) | inputs.remove(pos);
        if (changed) {
            currentOutputIndex = 0;
            currentInputIndex = 0;
            markDirty();
        }
        return changed;
    }

    public boolean hasOutputLink(BlockPos pos) {
        return outputs.contains(pos);
    }

    public boolean hasInputLink(BlockPos pos) {
        return inputs.contains(pos);
    }

    protected List<AbstractChannelBlockEntity> resolveOutputs() {
        return resolveChannels(outputs, false);
    }

    protected List<AbstractChannelBlockEntity> resolveInputs() {
        return resolveChannels(inputs, true);
    }

    private List<AbstractChannelBlockEntity> resolveChannels(List<BlockPos> positions, boolean input) {
        List<AbstractChannelBlockEntity> resolved = new ArrayList<>();
        if (level == null) {
            return resolved;
        }
        List<BlockPos> invalid = new ArrayList<>();
        for (BlockPos linked : positions) {
            if (level.getBlockEntity(linked) instanceof AbstractChannelBlockEntity channel && sameChannelType(channel)) {
                resolved.add(channel);
            } else {
                invalid.add(linked);
            }
        }
        if (!invalid.isEmpty()) {
            positions.removeAll(invalid);
            if (input) {
                currentInputIndex = 0;
            } else {
                currentOutputIndex = 0;
            }
            markDirty();
        }
        return resolved;
    }

    protected <T> T nextRoundRobin(List<T> values, boolean input) {
        if (values.isEmpty()) {
            return null;
        }
        int index = input ? currentInputIndex : currentOutputIndex;
        if (index >= values.size()) {
            index = 0;
        }
        T value = values.get(index);
        if (input) {
            currentInputIndex = (index + 1) % values.size();
        } else {
            currentOutputIndex = (index + 1) % values.size();
        }
        return value;
    }

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readTileData(tag, registries);
        inputs.clear();
        outputs.clear();
        int inputSize = tag.getInt("inputCount");
        int outputSize = tag.getInt("outputCount");
        for (int i = 0; i < inputSize; i++) {
            inputs.add(BlockPos.of(tag.getLong("input_" + i)));
        }
        for (int i = 0; i < outputSize; i++) {
            outputs.add(BlockPos.of(tag.getLong("output_" + i)));
        }
        currentInputIndex = tag.getInt("currentInputIndex");
        currentOutputIndex = tag.getInt("currentOutputIndex");
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeTileData(tag, registries);
        tag.putInt("inputCount", inputs.size());
        tag.putInt("outputCount", outputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            tag.putLong("input_" + i, inputs.get(i).asLong());
        }
        for (int i = 0; i < outputs.size(); i++) {
            tag.putLong("output_" + i, outputs.get(i).asLong());
        }
        tag.putInt("currentInputIndex", currentInputIndex);
        tag.putInt("currentOutputIndex", currentOutputIndex);
    }

    protected ModularUI buildChannelUI(BlockUIMenuType.BlockUIHolder holder, ResourceLocation background, Consumer<UIElement> body) {
        var root = TENMachineBlockUIFactory.createRoot(background);
        root.addChild(label(8, 8, "IN: " + inputs.size()));
        root.addChild(label(8, 20, "OUT: " + outputs.size()));
        root.addChild(label(8, 32, holder.blockState.getBlock().getName().getString()));
        body.accept(root);
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    protected UIElement label(int x, int y, String text) {
        TextElement label = new Label().setText(Component.literal(text));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
    }

    protected UIElement translatedLabel(int x, int y, String key, String suffix) {
        TextElement label = new Label().setText(ComponentHelper.translated(ComponentHelper.getKey(key)).append(Component.literal(suffix)));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
    }

    @Override
    public boolean hasFaceCapabilityEnergy(Direction side) {
        return false;
    }

    @Override
    public boolean hasFaceCapabilityItem(Direction side) {
        return false;
    }

    @Override
    public boolean hasFaceCapabilityFluid(Direction side) {
        return false;
    }
}
