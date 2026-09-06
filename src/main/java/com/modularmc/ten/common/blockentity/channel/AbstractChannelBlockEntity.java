package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.channel.ChannelKey;
import com.modularmc.ten.common.channel.ChannelRegistry;
import com.modularmc.ten.common.channel.ChannelType;
import com.modularmc.ten.common.channel.SharedStorage;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 频道方块基类（末影箱模式）。
 * <p>
 * 接入（{@link #join}）后与同名称+同类型的其他频道方块共享同一虚拟存储
 * （{@link SharedStorage}，全局跨维度）；零 tick 传输——接入后槽位直接绑定
 * 共享存储，不再有逐 tick 轮询搬运。退出（{@link #leave}）时本地缓冲内容
 * 优先回流频道共享存储，频道满则留本地缓冲。
 * <p>
 * P0-8 移植说明：RPC 语义与 26.1.2 一致——C→S 经 {@code rpcToServer} 发起，服务端
 * 处理器以 {@code sender.isRemote()} 守卫；S→C 单播经 {@code rpcToPlayer}、广播经
 * {@code rpcToTracking}，客户端处理器以 {@code sender.isServer()} 守卫。
 * 26.1.2 的 Identifier 在 1.21.1 以 {@link ResourceLocation} 替代。
 */
public abstract class AbstractChannelBlockEntity extends CmMachineBlockEntity {

    /** 频道名输入合法字符（排除目录分隔符 ':' ';'，最长 16 字符）。 */
    private static final Pattern CHANNEL_NAME_PATTERN = Pattern.compile("^[\\w\\u4e00-\\u9fa5\\- ]{1,16}$");

    /** 已接入的频道名；空串表示未接入。@Persisted 存档持久化，@DescSynced 客户端同步。 */
    @Persisted
    @DescSynced
    protected String channelId = "";

    /** 已接入频道的当前成员数（服务端 tick 刷新）；@DescSynced 同步客户端推算共享槽位上限（64×成员数）。 */
    @Persisted
    @DescSynced
    protected int joinedMemberCount = 0;

    /** 客户端 UI 频道目录缓存（服务端 RPC 推送，格式 name:count;name:count）。 */
    private String directoryData = "";

    /** 物品 UI 门面 handler（服务端接入态动态委托共享存储，客户端委托本地缓冲）。 */
    private ChannelItemHandlerFacade itemFacade;

    private boolean chunkUnloadingLocal = false;

    public AbstractChannelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setEfficiency(0);
    }

    @Override
    public boolean hasUpgrade() {
        return false;
    }

    @Override
    public boolean supportsUpgradeSlots() {
        return false;
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

    // ───── 频道接入状态 ─────

    /** 该频道方块的存储类型（与子类一一对应）。 */
    protected abstract ChannelType channelType();

    public boolean isJoined() {
        return channelId != null && !channelId.isEmpty();
    }

    public String joinedChannelName() {
        return channelId == null ? "" : channelId;
    }

    /** 已接入频道的成员数（客户端同步值；未接入恒 0）。 */
    public int joinedMemberCount() {
        return joinedMemberCount;
    }

    protected ChannelKey channelKey(String name) {
        return new ChannelKey(name, channelType());
    }

    protected ChannelKey joinedKey() {
        return isJoined() ? channelKey(channelId) : null;
    }

    /** 成员唯一标识：维度 + 坐标（跨维度可区分）。 */
    public String memberId() {
        return level != null ? level.dimension().location().toString() + ":" + worldPosition.toShortString() : worldPosition.toShortString();
    }

    protected ChannelRegistry registry() {
        if (level == null || level.isClientSide()) {
            return null;
        }
        return ChannelRegistry.get(level.getServer());
    }

    /** 接入态下当前频道的共享存储；未接入或客户端返回 null。 */
    public SharedStorage sharedStorage() {
        ChannelRegistry reg = registry();
        ChannelKey key = joinedKey();
        return reg == null || key == null ? null : reg.get(key);
    }

    // ───── join / leave ─────

    /**
     * 接入频道。本地内容并入共享存储（频道满留本地）；同一成员重复接入同一
     * 频道幂等返回 false。已接入其他频道时先退出旧频道。
     *
     * @return true 表示接入成功
     */
    public boolean join(String name) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (!isValidChannelName(name)) {
            return false;
        }
        if (isJoined() && channelId.equals(name)) {
            return false;
        }
        ChannelRegistry reg = registry();
        if (reg == null) {
            return false;
        }
        if (isJoined()) {
            leave();
        }
        ChannelKey key = channelKey(name);
        reg.join(key, memberId());
        channelId = name;
        joinedMemberCount = reg.memberCount(key);
        // join 本地内容并入共享（满留本地）
        pushLocalToShared();
        markDirty();
        setActive(true);
        return true;
    }

    /**
     * 退出频道。本地缓冲内容优先回流频道共享存储，频道满则留本地缓冲；
     * 共享容量按成员数重算（拒绝缩容由 SharedStorage 保证，内容不丢）。
     *
     * @return true 表示退出成功
     */
    public boolean leave() {
        if (!isJoined()) {
            return false;
        }
        if (level == null || level.isClientSide()) {
            return false;
        }
        ChannelRegistry reg = registry();
        ChannelKey key = joinedKey();
        if (reg != null && key != null) {
            pushLocalToShared();
            reg.leave(key, memberId());
        }
        channelId = "";
        joinedMemberCount = 0;
        markDirty();
        setActive(false);
        return true;
    }

    /**
     * 本地缓冲内容并入共享存储（满留本地）。子类按存储类型实现。
     * join 与 leave 共用：join 时并入本地已有内容，leave 时回流断开。
     */
    protected abstract void pushLocalToShared();

    private static boolean isValidChannelName(String name) {
        return name != null && CHANNEL_NAME_PATTERN.matcher(name).matches();
    }

    // ───── tick：零 tick 传输 ─────

    @Override
    public void tick() {
        doBaseData();
        setActive(isJoined());
        refreshJoinedMemberCount();
    }

    /** 服务端每 tick 刷新已接入频道的成员数（@DescSynced 同步客户端，驱动共享槽位上限显示）。 */
    private void refreshJoinedMemberCount() {
        if (level == null || level.isClientSide() || !isJoined()) {
            return;
        }
        ChannelRegistry reg = registry();
        ChannelKey key = joinedKey();
        if (reg == null || key == null) {
            return;
        }
        int count = reg.memberCount(key);
        if (joinedMemberCount != count) {
            joinedMemberCount = count;
        }
    }

    // ───── 生命周期：方块破坏时断开频道（chunk unload 不断开）─────

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        chunkUnloadingLocal = true;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        chunkUnloadingLocal = false;
    }

    @Override
    public void setRemoved() {
        if (!chunkUnloadingLocal && level != null && !level.isClientSide() && isJoined()) {
            leave();
        }
        super.setRemoved();
    }

    // ───── 面能力：未接入不暴露；接入态由子类指向共享 handler ─────

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

    // ───── 物品 UI 门面（零 tick：接入后槽位直接绑定共享存储）─────

    public ChannelItemHandlerFacade getChannelItemFacade() {
        if (itemFacade == null) {
            itemFacade = new ChannelItemHandlerFacade(this);
        }
        return itemFacade;
    }

    // ───── RPC：频道目录同步 + 创建/接入/退出 ─────
    // LDLib2 语义：C→S 包在服务端执行时 sender=RPCSender.ofClient(player)（isRemote()=true、asPlayer()=玩家）；
    // S→C 包在客户端执行时 sender=RPCSender.ofServer()（isServer()=true）。

    /** C→S：请求频道目录（UI 打开时）。 */
    @RPCMethod
    public void rpcRequestChannelDirectory(RPCSender sender) {
        if (sender.isRemote() && sender.asPlayer() != null) {
            rpcToPlayer(sender.asPlayer(), "rpcSyncChannelDirectory", encodeDirectory());
        }
    }

    /** S→C：推送频道目录（name:count;name:count，按类型过滤）。 */
    @RPCMethod
    public void rpcSyncChannelDirectory(RPCSender sender, String directory) {
        if (sender.isServer()) {
            this.directoryData = directory == null ? "" : directory;
        }
    }

    /** C→S：创建频道（命名）。 */
    @RPCMethod
    public void rpcCreateChannel(RPCSender sender, String name) {
        if (!sender.isRemote() || !isValidChannelName(name)) {
            return;
        }
        ChannelRegistry reg = registry();
        if (reg == null) {
            return;
        }
        reg.getOrCreate(channelKey(name));
        pushDirectory(sender.asPlayer());
    }

    /** C→S：接入频道。 */
    @RPCMethod
    public void rpcJoinChannel(RPCSender sender, String name) {
        if (!sender.isRemote() || !isValidChannelName(name)) {
            return;
        }
        if (join(name)) {
            pushDirectory(sender.asPlayer());
        }
    }

    /** C→S：退出当前频道。 */
    @RPCMethod
    public void rpcLeaveChannel(RPCSender sender) {
        if (!sender.isRemote()) {
            return;
        }
        if (leave()) {
            pushDirectory(sender.asPlayer());
        }
    }

    /**
     * C→S：删除当前接入频道。仅空频道可删（共享内容全空且无其他成员，操作者自身除外）；
     * 非空频道不执行、频道保留。操作者自身也是成员——先退出清空成员集再删；成功后推送目录同步。
     */
    @RPCMethod
    public void rpcDeleteChannel(RPCSender sender) {
        if (!sender.isRemote()) {
            return;
        }
        ChannelRegistry reg = registry();
        ChannelKey key = joinedKey();
        if (reg == null || key == null) {
            return;
        }
        SharedStorage storage = reg.get(key);
        // 非空频道拒绝：共享内容残留或其他成员仍在（操作者不退出、频道保留）
        if (storage == null || !storage.isEmpty() || reg.memberCount(key) > 1) {
            return;
        }
        // 操作者自身也是成员：先退出清空成员集（本地缓冲不回流量——频道整体删除），再删频道
        reg.leave(key, memberId());
        if (reg.remove(key)) {
            channelId = "";
            joinedMemberCount = 0;
            markDirty();
            setActive(false);
            pushDirectory(sender.asPlayer());
        }
    }

    private void pushDirectory(ServerPlayer player) {
        if (player != null) {
            rpcToPlayer(player, "rpcSyncChannelDirectory", encodeDirectory());
        }
    }

    private String encodeDirectory() {
        ChannelRegistry reg = registry();
        if (reg == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChannelKey key : reg.listChannels(channelType())) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(key.name()).append(':').append(reg.memberCount(key));
        }
        return sb.toString();
    }

    private List<ChannelEntry> parseDirectory() {
        List<ChannelEntry> entries = new ArrayList<>();
        if (directoryData == null || directoryData.isEmpty()) {
            return entries;
        }
        for (String part : directoryData.split(";")) {
            if (part.isEmpty()) {
                continue;
            }
            int idx = part.lastIndexOf(':');
            if (idx <= 0 || idx >= part.length() - 1) {
                continue;
            }
            try {
                entries.add(new ChannelEntry(part.substring(0, idx), Integer.parseInt(part.substring(idx + 1))));
            } catch (NumberFormatException ignored) {
                // 防御性解析：跳过损坏片段（服务端编码受频道名正则约束，正常不会出现）
            }
        }
        return entries;
    }

    private record ChannelEntry(String name, int members) {}

    /** 1.21.1 HoverTooltips 构造兼容（26.1.2 的 HoverTooltips.create 在 2.2.37 不存在）。 */
    private static HoverTooltips tooltip(Component... components) {
        return new HoverTooltips(List.of(components), null, null, null);
    }

    // ───── UI ─────

    protected ModularUI buildChannelUI(BlockUIMenuType.BlockUIHolder holder,
                                       ResourceLocation background,
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

    /**
     * 频道目录列表左缘：两频道统一为 61 —— 与玩家物品栏第 4 槽（0 基 index 3）左缘对齐
     * （addPlayerInventory 中 InventorySlots 左缘 7、槽 18px 无间隙：7+3×18=61）。
     */
    protected int channelListX() {
        return 61;
    }

    /** 条目背景宽度：两频道统一 46（完整采样 channel_entry_* 素材 46×13 不裁切）。 */
    protected int channelListEntryWidth() {
        return 46;
    }

    /**
     * 频道目录 UI：全局频道列表（按类型过滤，显示名称+成员数）+ 右侧创建按钮 +
     * 频道名输入框 + 点击条目接入 + 接入态「退出」按钮 + 当前频道标识。
     */
    private void addChannelEntryWidgets(UIElement root, BlockUIMenuType.BlockUIHolder holder) {
        var state = ChannelUIState.of(holder);
        int listX = channelListX();
        int entryWidth = channelListEntryWidth();
        int entryX = listX + 1;

        // UI 打开时向服务端请求当前频道目录
        if (holder.player.level().isClientSide()) {
            rpcToServer("rpcRequestChannelDirectory");
        }

        // 列表容器底图：channel_list_bg（69x73 深色容器）
        int listBgWidth = 69;
        var listContainer = absolute(new UIElement(), listX, 5, listBgWidth, 73);
        listContainer.style(style -> style.backgroundTexture(
                SpriteTexture.of(TENConstants.CHANNEL_LIST_BG).setSprite(0, 0, listBgWidth, 73)));
        root.addChild(listContainer);

        var entryBackgrounds = new UIElement[5];
        var entryLabels = new Label[5];
        var miniButtons = new UIElement[5];
        boolean[] entryHovered = new boolean[5];
        boolean[] miniHovered = new boolean[5];
        boolean[] currentFlags = new boolean[5];
        for (int i = 0; i < 5; i++) {
            int entryY = 7 + i * 14;
            int miniY = 6 + i * 14;
            int row = i;
            var background = absolute(new UIElement(), entryX, entryY, entryWidth, 13)
                    .style(style -> style.backgroundTexture(SpriteTexture.of(TENConstants.CHANNEL_ENTRY_BG_NORMAL).setSprite(0, 0, entryWidth, 13)));
            background.addEventListener(UIEvents.MOUSE_ENTER, event -> {
                entryHovered[row] = true;
                applyEntryBackground(background, entryWidth, true);
            });
            background.addEventListener(UIEvents.MOUSE_LEAVE, event -> {
                entryHovered[row] = false;
                applyEntryBackground(background, entryWidth, false);
            });
            background.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    var entries = parseDirectory();
                    int entryIndex = state.cursorFrom + row;
                    if (entryIndex < entries.size() && !entries.get(entryIndex).name().equals(channelId)) {
                        rpcToServer("rpcJoinChannel", entries.get(entryIndex).name());
                    }
                }
            });
            background.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
                var entries = parseDirectory();
                int entryIndex = state.cursorFrom + row;
                if (entryIndex < entries.size()) {
                    var entry = entries.get(entryIndex);
                    event.hoverTooltips = tooltip(
                            Component.literal(entry.name()).withStyle(ChatFormatting.WHITE),
                            Component.literal("(" + entry.members() + ")").withStyle(ChatFormatting.GRAY));
                }
            });
            var label = new Label();
            label.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(entryX + 6);
                layout.top(entryY + 2);
            });
            label.textStyle(style -> style.textShadow(false).fontSize(8));
            var miniButton = absolute(new UIElement(), entryX + entryWidth, miniY, 10, 15)
                    .style(style -> style.backgroundTexture(miniSprite(TENConstants.MINI_OUT_NORMAL)));
            miniButton.addEventListener(UIEvents.MOUSE_ENTER, event -> {
                miniHovered[row] = true;
                applyMiniIcon(miniButton, currentFlags[row], true);
            });
            miniButton.addEventListener(UIEvents.MOUSE_LEAVE, event -> {
                miniHovered[row] = false;
                applyMiniIcon(miniButton, currentFlags[row], false);
            });
            miniButton.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    var entries = parseDirectory();
                    int entryIndex = state.cursorFrom + row;
                    if (entryIndex < entries.size()) {
                        String name = entries.get(entryIndex).name();
                        if (isJoined() && name.equals(channelId)) {
                            rpcToServer("rpcLeaveChannel");
                        } else {
                            rpcToServer("rpcJoinChannel", name);
                        }
                    }
                }
            });
            entryBackgrounds[i] = background;
            entryLabels[i] = label;
            miniButtons[i] = miniButton;
            root.addChild(background);
            root.addChild(label);
            root.addChild(miniButton);
        }

        // 翻页按钮（列表容器右上/右下角）：▲上翻右上、▼下翻右下。
        int scrollButtonX = listX + 56;
        var scrollUpButton = channelSheetButton(scrollButtonX, 6, TENConstants.SCROLL_UP_NORMAL, TENConstants.SCROLL_UP_HOVER);
        scrollUpButton.setOnClick(event -> state.scrollUp());
        root.addChild(scrollUpButton);

        var scrollDownButton = channelSheetButton(scrollButtonX, 65, TENConstants.SCROLL_DOWN_NORMAL, TENConstants.SCROLL_DOWN_HOVER);
        scrollDownButton.setOnClick(event -> state.scrollDown(parseDirectory().size()));
        root.addChild(scrollDownButton);

        // 右侧操作区：频道名输入框 + 创建按钮 + 当前频道标识 + 退出按钮。
        var nameField = new TextField();
        nameField.setText("");
        nameField.setTextRegexValidator("^[\\w\\u4e00-\\u9fa5\\- ]+$");
        nameField.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(130);
            layout.top(5);
            layout.width(40);
            layout.height(12);
        });

        var createButton = channelSheetButton(130, 21, TENConstants.CREATE_NORMAL, TENConstants.CREATE_HOVER);
        createButton.setOnClick(event -> {
            String name = nameField.getValue() == null ? "" : nameField.getValue().trim();
            if (!name.isEmpty()) {
                rpcToServer("rpcCreateChannel", name);
            }
        });
        createButton.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = tooltip(ComponentHelper.translated(ComponentHelper.getKey("channel.create"))));

        var currentLabel = new Label();
        currentLabel.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(130);
            layout.top(69);
        });
        currentLabel.textStyle(style -> style.textShadow(false).fontSize(8));
        currentLabel.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (isJoined()) {
                event.hoverTooltips = tooltip(
                        ComponentHelper.translated(ComponentHelper.getKey("channel.current"))
                                .append(Component.literal(channelId))
                                .withStyle(ChatFormatting.GREEN));
            } else {
                event.hoverTooltips = tooltip(
                        ComponentHelper.translated(ComponentHelper.getKey("channel.current"))
                                .append(ComponentHelper.translated(ComponentHelper.getKey("channel.none"))));
            }
        });

        var leaveButton = channelSheetButton(130, 53, TENConstants.DISCONNECT_NORMAL, TENConstants.DISCONNECT_HOVER);
        leaveButton.setOnClick(event -> rpcToServer("rpcLeaveChannel"));
        leaveButton.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = tooltip(ComponentHelper.translated(ComponentHelper.getKey("channel.leave"))));

        var deleteButton = channelSheetButton(130, 37, TENConstants.DELETE_NORMAL, TENConstants.DELETE_HOVER);
        deleteButton.setOnClick(event -> rpcToServer("rpcDeleteChannel"));
        deleteButton.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = tooltip(ComponentHelper.translated(ComponentHelper.getKey("channel.delete"))));
        deleteButton.setDisplay(false);

        root.addChild(nameField);
        root.addChild(createButton);
        root.addChild(leaveButton);
        root.addChild(deleteButton);
        root.addChild(currentLabel);

        Runnable update = () -> {
            var entries = parseDirectory();
            boolean joined = isJoined();
            for (int i = 0; i < 5; i++) {
                int entryIndex = state.cursorFrom + i;
                boolean visible = entryIndex < entries.size();
                entryBackgrounds[i].setDisplay(visible);
                entryLabels[i].setDisplay(visible);
                miniButtons[i].setDisplay(visible);
                if (!visible) {
                    entryHovered[i] = false;
                    miniHovered[i] = false;
                    continue;
                }
                var entry = entries.get(entryIndex);
                boolean current = joined && entry.name().equals(channelId);
                currentFlags[i] = current;
                applyEntryBackground(entryBackgrounds[i], entryWidth, entryHovered[i]);
                entryLabels[i].setText(entryText(entry, current, entryWidth));
                applyMiniIcon(miniButtons[i], current, miniHovered[i]);
            }
            if (joined) {
                String name = channelId;
                currentLabel.setText(Component.literal(
                        approxWidth(name) > CURRENT_LABEL_MAX_WIDTH ? truncateToWidth(name, CURRENT_LABEL_MAX_WIDTH - 5) + "…" : name)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                currentLabel.setText(ComponentHelper.translated(ComponentHelper.getKey("channel.not_joined"))
                        .withStyle(ChatFormatting.GRAY));
            }
            leaveButton.setDisplay(joined);
            boolean deletable = joined && entries.stream()
                    .anyMatch(e -> e.name().equals(channelId) && e.members() <= 1);
            deleteButton.setDisplay(deletable);
        };
        update.run();
        root.addEventListener(UIEvents.TICK, event -> update.run());
    }

    /** 操作区「当前频道」label 文本预算：x=130..172（GUI 176 宽，右缘留边），8px 字体近似宽。 */
    private static final int CURRENT_LABEL_MAX_WIDTH = 42;

    /** 条目行文本：名字 + 成员数，名字按近似宽度截断 + 省略号；当前频道绿色标识。 */
    private static Component entryText(ChannelEntry entry, boolean current, int entryWidth) {
        String name = entry.name();
        int members = entry.members();
        int suffixW = 8 + 6 * String.valueOf(members).length();
        int nameBudget = entryWidth - 6 - suffixW;
        boolean showSuffix = nameBudget >= 12;
        if (!showSuffix) {
            nameBudget = entryWidth - 6;
        }
        String shown = approxWidth(name) > nameBudget ? truncateToWidth(name, nameBudget - 5) + "…" : name;
        Component nameComp = Component.literal(shown)
                .withStyle(current ? ChatFormatting.GREEN : ChatFormatting.WHITE);
        if (showSuffix) {
            return nameComp.copy().append(Component.literal(" (" + members + ")").withStyle(ChatFormatting.GRAY));
        }
        return nameComp;
    }

    /** 8px 字体近似显示宽度：CJK 全角 8px，其余半角按 5px 估算。 */
    private static int approxWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += Character.UnicodeScript.of(s.charAt(i)) == Character.UnicodeScript.HAN ? 8 : 5;
        }
        return w;
    }

    /** 按 {@link #approxWidth} 截断到 maxWidth 内（调用方已为省略号留出预算）。 */
    private static String truncateToWidth(String s, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int cw = Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN ? 8 : 5;
            if (w + cw > maxWidth) {
                return s.substring(0, i);
            }
            w += cw;
        }
        return s;
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

    /** channel_buttons sheet 图标按钮：LDLib2 Button 组件 + buttonStyle 三态。 */
    private static Button channelSheetButton(int x, int y, TENConstants.SheetUV normal, TENConstants.SheetUV hover) {
        var button = new Button();
        button.noText();
        button.buttonStyle(style -> {
            style.baseTexture(scrollSprite(normal));
            style.hoverTexture(scrollSprite(hover));
            style.pressedTexture(scrollSprite(hover));
        });
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(12);
            layout.height(12);
        });
        return button;
    }

    /** channel_buttons sheet 精灵（按 {@link TENConstants.SheetUV} 像素裁剪）。 */
    private static SpriteTexture scrollSprite(TENConstants.SheetUV uv) {
        return SpriteTexture.of(TENConstants.CHANNEL_BUTTONS).setSprite(uv.u(), uv.v(), uv.width(), uv.height());
    }

    /** channel_entry_state sheet 精灵（按 {@link TENConstants.SheetUV} 像素裁剪）。 */
    private static SpriteTexture miniSprite(TENConstants.SheetUV uv) {
        return SpriteTexture.of(TENConstants.CHANNEL_ENTRY_STATE).setSprite(uv.u(), uv.v(), uv.width(), uv.height());
    }

    /** 条目行背景按 hover 态切 in/out。 */
    private static void applyEntryBackground(UIElement background, int entryWidth, boolean hovered) {
        background.style(style -> style.backgroundTexture(
                SpriteTexture.of(hovered ? TENConstants.CHANNEL_ENTRY_BG_HOVER : TENConstants.CHANNEL_ENTRY_BG_NORMAL)
                        .setSprite(0, 0, entryWidth, 13)));
    }

    /** mini 状态按钮按接入态 × hover 态选四格之一（channel_entry_state sheet）。 */
    private static void applyMiniIcon(UIElement mini, boolean current, boolean hovered) {
        TENConstants.SheetUV uv = hovered ? (current ? TENConstants.MINI_IN_HOVER : TENConstants.MINI_OUT_HOVER) : (current ? TENConstants.MINI_IN_NORMAL : TENConstants.MINI_OUT_NORMAL);
        mini.style(style -> style.backgroundTexture(miniSprite(uv)));
    }

    private static final class ChannelUIState {

        private int cursorFrom;

        static ChannelUIState of(BlockUIMenuType.BlockUIHolder holder) {
            // UI 实例局部状态：每次打开 GUI 滚动位置从 0 开始
            return new ChannelUIState();
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

    /**
     * 该频道是否需要在内部维持能量存储。
     * 仅能量频道返回 true（构造时经 setCapacity 建立真实储能）；
     * 物品/流体频道无能量需求，返回 false 使能量基础设施完全惰性化。
     */
    protected boolean needsEnergyStorage() {
        return false;
    }

    @Override
    public void initMachine() {
        super.initMachine();
        if (needsEnergyStorage()) {
            return;
        }
        // 物品/流体频道不需要能量存储：把能量基础设施清零，避免 initMachine/doBaseData
        // 仍创建并逐 tick 缩放默认 10000FE 的内部缓冲。hasFaceCapabilityEnergy=false 已阻断
        // 外部能量访问，此处进一步确保容量/收发速率恒为 0（doBaseData 依赖 energyStorage
        // 非空才继续，因此保留对象本身而非置 null）。
        initialEnergyStorage = 0;
        initialEnergyReceive = 0;
        initialEnergyExtract = 0;
        maxStorageEnergy = 0;
        maxReceiveEnergy = 0;
        maxExtractEnergy = 0;
        if (energyStorage != null) {
            energyStorage.setCapacity(0);
            energyStorage.setMaxReceive(0);
            energyStorage.setMaxExtract(0);
        }
    }
}
