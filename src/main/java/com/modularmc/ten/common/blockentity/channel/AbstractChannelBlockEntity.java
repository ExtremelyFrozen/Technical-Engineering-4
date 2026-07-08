package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractChannelBlockEntity extends CmMachineBlockEntity {

    private static final Identifier CHANNEL_HANDLER = TEN.id("textures/gui/channel.png");

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
        return true;
    }

    @Override
    public boolean hasSideBar() {
        return true;
    }

    @Override
    public int initialFaceModeEnergy() {
        return FaceOption.OFF;
    }

    @Override
    public int initialFaceModeItem() {
        return FaceOption.OFF;
    }

    @Override
    public int initialFaceModeFluid() {
        return FaceOption.OFF;
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
    protected void readTileData(ValueInput input) {
        super.readTileData(input);
        inputs.clear();
        outputs.clear();
        int inputSize = input.getIntOr("inputCount", 0);
        int outputSize = input.getIntOr("outputCount", 0);
        for (int i = 0; i < inputSize; i++) {
            inputs.add(BlockPos.of(input.getLongOr("input_" + i, 0L)));
        }
        for (int i = 0; i < outputSize; i++) {
            outputs.add(BlockPos.of(input.getLongOr("output_" + i, 0L)));
        }
        currentInputIndex = input.getIntOr("currentInputIndex", 0);
        currentOutputIndex = input.getIntOr("currentOutputIndex", 0);
    }

    @Override
    protected void writeTileData(ValueOutput output) {
        super.writeTileData(output);
        output.putInt("inputCount", inputs.size());
        output.putInt("outputCount", outputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            output.putLong("input_" + i, inputs.get(i).asLong());
        }
        for (int i = 0; i < outputs.size(); i++) {
            output.putLong("output_" + i, outputs.get(i).asLong());
        }
        output.putInt("currentInputIndex", currentInputIndex);
        output.putInt("currentOutputIndex", currentOutputIndex);
    }

    protected ModularUI buildChannelUI(BlockUIMenuType.BlockUIHolder holder,
                                       Identifier background,
                                       Consumer<UIElement> inventoryBuilder,
                                       Consumer<UIElement> contentBuilder) {
        return buildMachineUI(holder, background, root -> {
            inventoryBuilder.accept(root);
        }, root -> {
            addChannelEntryWidgets(root, holder);
            contentBuilder.accept(root);
        });
    }

    protected UIElement label(int x, int y, String text) {
        Label label = new Label();
        label.setText(Component.literal(text));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
    }

    protected UIElement translatedLabel(int x, int y, String key, String suffix) {
        Label label = new Label();
        label.setText(ComponentHelper.translated(ComponentHelper.getKey(key)).append(Component.literal(suffix)));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
    }

    private void addChannelEntryWidgets(UIElement root, BlockUIMenuType.BlockUIHolder holder) {
        var state = ChannelUIState.of(holder);
        var entryBackgrounds = new UIElement[5];
        var entryLabels = new Label[5];

        for (int i = 0; i < 5; i++) {
            int y = 7 + i * 14;
            var background = absolute(new UIElement(), 53, y, 48, 13)
                    .style(style -> style.backgroundTexture(SpriteTexture.of(CHANNEL_HANDLER).setSprite(0, 166, 48, 13)));
            var label = new Label();
            label.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(59);
                layout.top(y + 2);
            });
            label.textStyle(style -> style.textShadow(false).fontSize(8));
            entryBackgrounds[i] = background;
            entryLabels[i] = label;
            root.addChild(background);
            root.addChild(label);
        }

        root.addChild(absolute(new UIElement(), 107, 5, 12, 12)
                .style(style -> style.backgroundTexture(SpriteTexture.of(CHANNEL_HANDLER).setSprite(84, 166, 12, 12)))
                .addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, event -> {
                    if (event.button == 0) {
                        state.scrollUp();
                    }
                }));
        root.addChild(absolute(new UIElement(), 107, 64, 12, 12)
                .style(style -> style.backgroundTexture(SpriteTexture.of(CHANNEL_HANDLER).setSprite(96, 166, 12, 12)))
                .addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, event -> {
                    if (event.button == 0) {
                        state.scrollDown(combinedEntryCount());
                    }
                }));

        Runnable update = () -> {
            var entries = buildEntries();
            for (int i = 0; i < 5; i++) {
                int entryIndex = state.cursorFrom + i;
                boolean visible = entryIndex < entries.size();
                entryBackgrounds[i].setDisplay(visible);
                entryLabels[i].setDisplay(visible);
                if (!visible) {
                    continue;
                }
                var entry = entries.get(entryIndex);
                entryBackgrounds[i].style(style -> style.backgroundTexture(
                        SpriteTexture.of(CHANNEL_HANDLER).setSprite(0, entry.isInput ? 166 : 179, 48, 13)));
                entryLabels[i].setText(ComponentHelper.translated(ComponentHelper.getKey("channel"))
                        .append(ComponentHelper.make("#", String.valueOf(entry.index)))
                        .withStyle(entry.isInput ? ChatFormatting.RED : ChatFormatting.GREEN));
            }
        };
        update.run();
        root.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, event -> update.run());
    }

    private int combinedEntryCount() {
        return inputs.size() + outputs.size();
    }

    private List<ChannelEntryData> buildEntries() {
        List<ChannelEntryData> entries = new ArrayList<>();
        int index = 0;
        for (BlockPos input : inputs) {
            entries.add(new ChannelEntryData(index++, input, true));
        }
        for (BlockPos output : outputs) {
            entries.add(new ChannelEntryData(index++, output, false));
        }
        return entries;
    }

    private static <T extends UIElement> T absolute(T element, int x, int y, int width, int height) {
        element.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
            layout.height(height);
        });
        return element;
    }

    private record ChannelEntryData(int index, BlockPos pos, boolean isInput) {}

    private static final class ChannelUIState {

        private static final Map<String, ChannelUIState> CACHE = new ConcurrentHashMap<>();
        private final String key;
        private int cursorFrom;

        private ChannelUIState(String key) {
            this.key = key;
        }

        static ChannelUIState of(BlockUIMenuType.BlockUIHolder holder) {
            String key = holder.player.getUUID() + "@channel@" + holder.pos.asLong();
            return CACHE.computeIfAbsent(key, ChannelUIState::new);
        }

        void scrollUp() {
            cursorFrom = Math.max(0, cursorFrom - 1);
        }

        void scrollDown(int size) {
            if (size <= 5) {
                cursorFrom = 0;
            } else {
                cursorFrom = Math.min(size - 5, cursorFrom + 1);
            }
        }
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
