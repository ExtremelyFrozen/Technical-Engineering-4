package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.transmission.CableRenderState;
import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.TransmitterNetworkRegistry;
import com.modularmc.ten.api.transmission.item.ItemNetwork;
import com.modularmc.ten.api.transmission.item.ItemTransmitter;
import com.modularmc.ten.api.transmission.item.TransferUtil;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.items.IItemHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * 物品管道方块实体：持有 {@link ItemTransmitter}，网络所有权归物品网络；
 * 组网/拆网由 {@link TransmitterNetworkRegistry} 延迟驱动，搬运由 {@link ItemNetwork#onUpdate()} 统一执行。
 * 白/黑名单过滤由 {@code filterInventory} 提供；{@code transportHandler} 将外部插入即时投递到网络受端。
 */
public class PipeBlockEntity extends CmBlockEntity implements ITransmitterProvider {

    private final ItemTransmitter transmitter = new ItemTransmitter(this) {

        @Override
        public boolean isItemAllowed(ItemStack stack) {
            return PipeBlockEntity.this.isItemAllowed(stack);
        }
    };

    private boolean joinPending;

    /**
     * 输出面位图镜像（bit d = Direction d 为 PULL）：LDLib2 @DescSynced 自动同步到客户端，
     * 供 Jade/模型读取方向模式（transmitter 本体不经 managed 字段体系，无客户端同步）；
     * 服务端在 notifyChanges 时重写。
     */
    @Persisted
    @DescSynced
    public byte pullFacesMask;

    /**
     * 打包后的连接外观 + active 渲染态，由服务端权威维护，不写入 BlockState。
     * 随方块实体数据包下发（{@link #getUpdateTag} → {@code writeTileData}），客户端在
     * {@link #applyRenderTag} 里写入并请求重绘；{@code @DescSynced} 另走 LDLib2 的字段同步，
     * 供 GUI 等场景读取。
     */
    @Persisted
    @DescSynced
    public int renderPacked;

    /** 过滤槽固定 27 槽（3×9）：readTileData Size 迁移与 GUI 循环统一引用，防旧档 Size 缩容。 */
    private static final int FILTER_SLOTS = 27;

    private final MachineItemHandler filterInventory = new MachineItemHandler(27);
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
            ItemStack stack = filterInventory.extractItem(slot, amount, false);
            if (!stack.isEmpty()) {
                setChanged();
            }
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = filterInventory.getStackInSlot(slot);
            filterInventory.setStackInSlot(slot, ItemStack.EMPTY);
            setChanged();
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            filterInventory.setStackInSlot(slot, stack);
            // 幽灵槽写入后必须通知 BE，否则客户端槽视图不会更新。
            setChanged();
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

    /** 外部插入入口：经网络找可达 sink 即时投递；过滤不匹配则拒收，否则返回未投递的剩余物品。 */
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
            ItemNetwork network = transmitter.getNetwork();
            if (network == null) {
                return stack;
            }
            ItemStack remaining = stack.copy();
            for (BlockPos pipePos : network.positionedTransmitters.keySet()) {
                for (Direction direction : Direction.values()) {
                    BlockPos targetPos = pipePos.relative(direction);
                    if (level.getBlockEntity(targetPos) instanceof PipeBlockEntity || level.getBlockEntity(targetPos) instanceof ITransmitterProvider) {
                        continue;
                    }
                    IItemHandler sink = level.getCapability(
                            net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                            targetPos, direction.getOpposite());
                    if (sink == null) {
                        continue;
                    }
                    remaining = TransferUtil.insertItem(sink, remaining, simulate);
                    if (remaining.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ItemTransmitter getTransmitter() {
        return transmitter;
    }

    public IItemHandler getTransportHandler(Direction side) {
        return transportHandler;
    }

    public MachineItemHandler getFilterInventory() {
        return filterInventory;
    }

    public boolean hasUi() {
        // 仅白/黑名单变体（pipe_white/pipe_black）有过滤 UI，普通 pipe 无界面。
        return isFiltered();
    }

    // ───── ITransmitterProvider：网络接入生命周期 ─────

    @Override
    public void onLoad() {
        super.onLoad();
        joinNetwork();
    }

    private void joinNetwork() {
        // 延迟到服务端首 tick：onLoad 期间查询邻居会触发相邻 BE 创建与互查，造成递归组网。
        if (level != null && !level.isClientSide()) {
            joinPending = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            TransmitterNetworkRegistry.remove(transmitter);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide()) {
            TransmitterNetworkRegistry.remove(transmitter);
        }
        super.onChunkUnloaded();
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    @Override
    public void notifyChanges() {
        // 标记 BE 数据变化并刷新渲染态；方块更新只在渲染态变化时由 markRenderDirty 发出，
        // 且不触发邻居更新，避免 neighborChanged → refreshConnections 递归。
        setChanged();
        if (level != null && !level.isClientSide()) {
            refreshRenderState();
            refreshPullFacesMask();
        }
    }

    @Override
    public int getRenderPacked() {
        return renderPacked;
    }

    @Override
    public void refreshRenderState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean active = new CableRenderState(renderPacked).active();
        int next = CableRenderState.pack(transmitter, active);
        if (renderPacked != next) {
            renderPacked = next;
            markRenderDirty();
        }
    }

    /**
     * 服务端唯一的重绘请求入口：{@code sendBlockUpdated} 既让客户端区块重新烘焙，
     * 又经 {@code ChunkHolder#broadcastBlockEntity} 把 {@link #getUpdateTag} 里的渲染态一并送到。
     * flags=2 不含 UPDATE_NEIGHBORS，不会走 neighborChanged → refreshConnections 递归。
     */
    private void markRenderDirty() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 渲染态必须随方块实体数据包下发。
     * <p>
     * 原版 {@code BlockEntity#getUpdateTag} 默认返回空标签、{@link #getUpdatePacket()} 默认返回 null；
     * 两者都要覆写，区块加载与 {@code sendBlockUpdated} 才能把 {@code renderPacked} 送到客户端。
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 渲染态经 ModelData 交给客户端的动态 BakedModel；每次调用都现算，不缓存旧值。 */
    @Override
    public ModelData getModelData() {
        return ModelData.of(CableRenderState.PROPERTY, new CableRenderState(renderPacked));
    }

    // ───── 持久化 ─────

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        transmitter.read(registries, tag);
        // 空 filter tag 不得反序列化；兼容 filter.Size=9 的旧存档时按槽读入，
        // 避免直接反序列化把 27 槽缩回 9。
        CompoundTag filterTag = tag.getCompound("filter");
        if (!filterTag.isEmpty()) {
            int savedSize = filterTag.contains("Size") ? filterTag.getInt("Size") : FILTER_SLOTS;
            if (savedSize == FILTER_SLOTS) {
                filterInventory.deserializeNBT(registries, filterTag);
            } else {
                var list = filterTag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = 0; i < savedSize && i < list.size() && i < filterInventory.getSlots(); i++) {
                    filterInventory.setStackInSlot(i, ItemStack.parseOptional(registries, list.getCompound(i)));
                }
            }
        }
        applyRenderTag(tag);
    }

    /**
     * 渲染态写入 + 客户端重绘。缺一不可：
     * {@code requestModelDataUpdate()} 只把坐标记进 ModelDataManager 的 needModelDataRefresh，
     * 下次取数时刷新缓存；真正让区块重新烘焙的是客户端
     * {@code ClientLevel#sendBlockUpdated → LevelRenderer#blockChanged → setBlockDirty}。
     * <p>
     * 没有 renderPacked 键时直接跳过：LDLib2 的 GUI 同步会用空 tag 反复触发 readTileData，
     * 不能让它把渲染态清掉、也不能白刷一遍区块。
     */
    private void applyRenderTag(CompoundTag tag) {
        if (!tag.contains("renderPacked")) {
            return;
        }
        renderPacked = tag.getInt("renderPacked");
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 0);
        }
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        transmitter.write(registries, tag);
        // 渲染态随存档与 BE 数据包（getUpdateTag → saveWithoutMetadata）一起走，
        // 客户端建 BE 时就能拿到正确的连接外观，不必等下一次方块更新。
        tag.putInt("renderPacked", renderPacked);
        tag.put("filter", filterInventory.serializeNBT(registries));
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (joinPending) {
            joinPending = false;
            transmitter.refreshConnections();
            TransmitterNetworkRegistry.join(transmitter);
        }
        ItemNetwork net = transmitter.getNetwork();
        setActive(net != null && net.hasLastMoved());
    }

    // ───── GUI（27 槽过滤标记）─────

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.MACHINE_GUI);
        if (isFiltered()) {
            String modeKey = isWhitelist() ? "kenergyengineering.pipe.filter.whitelist" : "kenergyengineering.pipe.filter.blacklist";
            Label modeLabel = new Label();
            modeLabel.setText(ComponentHelper.translated(modeKey));
            modeLabel.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(8);
                layout.top(6);
            });
            root.addChild(modeLabel);
            // 三排 3×9=27 过滤槽使用纯展示槽，标记写入走 MOUSE_DOWN + RPC，
            // 由服务端 setFilterMark 统一去重后广播；acceptQuickMove(false) 阻断 quickMove。
            for (int i = 0; i < FILTER_SLOTS; i++) {
                int x = 7 + (i % 9) * 18;
                int y = 24 + (i / 9) * 18;
                int idx = i;
                var filterSlot = TENMachineBlockUIFactory.itemSlotModular(ghostFilterSlot(i), x, y);
                filterSlot.slotStyle(style -> style.acceptQuickMove(false));
                filterSlot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                    event.stopPropagation();
                    ItemStack carried = ItemStack.EMPTY;
                    var mui = filterSlot.getModularUI();
                    if (mui != null && mui.getMenu() != null) {
                        carried = mui.getMenu().getCarried();
                    }
                    if (event.button == 1) {
                        // 右键：清空点击槽标记。
                        rpcToServer("rpcSetFilterMark", idx, ItemStack.EMPTY);
                    } else if (event.button == 0 && !carried.isEmpty()) {
                        if (Screen.hasShiftDown()) {
                            // Shift+左键：标记到最小序号空槽，去重由服务端处理。
                            rpcToServer("rpcSetFilterMark", -1, carried.copy());
                        } else {
                            // 左键：标记或更新点击槽；同物品在其他槽时移动标记。
                            rpcToServer("rpcSetFilterMark", idx, carried.copy());
                        }
                    }
                });
                root.addChild(filterSlot);
            }
        }
        TENMachineBlockUIFactory.addPlayerInventory(root);
        // Shift+点击背包物品：请求标记到最小序号空槽；背包槽阻断 quickMove，由 MOUSE_DOWN 接管。
        java.util.Deque<UIElement> invDfs = new java.util.ArrayDeque<>();
        for (UIElement child : root.getChildren()) {
            if (child instanceof com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots inv) {
                invDfs.push(inv);
            }
        }
        while (!invDfs.isEmpty()) {
            UIElement e = invDfs.pop();
            for (UIElement c : e.getChildren()) {
                invDfs.push(c);
            }
            if (e instanceof ItemSlot playerSlot) {
                playerSlot.slotStyle(style -> style.acceptQuickMove(false));
                playerSlot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                    if (event.button == 0 && Screen.hasShiftDown()) {
                        ItemStack clicked = playerSlot.getValue();
                        if (clicked != null && !clicked.isEmpty()) {
                            event.stopPropagation();
                            rpcToServer("rpcSetFilterMark", -1, clicked.copy());
                        }
                    }
                });
            }
        }
        // GUI 打开时由客户端请求全量标记；createUI 也会在服务端执行，
        // rpcToServer 必须限制在客户端，专用服上调用会抛 IllegalStateException。
        if (holder.player.level().isClientSide()) {
            rpcToServer("rpcRequestFilterSync");
        }
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    /**
     * 幽灵标记槽仅展示服务端 filterInventory 的标记副本，不参与物品转移。
     * mayPlace/mayPickup/remove 均无副作用；写入与清除统一走 GUI MOUSE_DOWN + RPC，
     * 由服务端去重后广播。
     */
    private Slot ghostFilterSlot(int index) {
        return new Slot(filterContainer, index, 0, 0) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public ItemStack remove(int amount) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getMaxStackSize() {
                return 1; // 标记槽单件语义
            }
        };
    }

    // ───── 标记写入（服务端权威 + RPC 同步）─────

    /**
     * 服务端：写入/清除标记。index=-1 时找最小序号空槽；mark 空即清除该槽。
     * 去重语义：同物品同组件已标记在其他槽时，「点击目标槽」改为移动标记（原槽清空、目标槽写入），
     * shift 快速标记（index=-1）则直接拒绝（不产生移动）。
     */
    public void setFilterMark(int index, ItemStack mark) {
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean quickMark = index == -1;
        int target = index;
        if (quickMark) {
            target = -1;
            for (int i = 0; i < FILTER_SLOTS; i++) {
                if (filterInventory.getStackInSlot(i).isEmpty()) {
                    target = i;
                    break;
                }
            }
            if (target == -1) {
                return; // 无空槽
            }
        }
        if (target < 0 || target >= FILTER_SLOTS) {
            return;
        }
        if (!mark.isEmpty()) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                if (i != target && ItemStack.isSameItemSameComponents(filterInventory.getStackInSlot(i), mark)) {
                    if (quickMark) {
                        return; // shift 快速标记：同物品已存在则拒绝
                    }
                    filterInventory.setStackInSlot(i, ItemStack.EMPTY); // 点击目标槽：移动标记（原槽清空）
                    break;
                }
            }
        }
        ItemStack copy = mark.isEmpty() ? ItemStack.EMPTY : mark.copy();
        if (!copy.isEmpty()) {
            copy.setCount(1);
        }
        filterInventory.setStackInSlot(target, copy);
        syncFilterMarks();
        setChanged();
    }

    /** 服务端：全量广播标记到追踪客户端（变更后）。 */
    private void syncFilterMarks() {
        if (level == null || level.isClientSide()) {
            return;
        }
        rpcToTracking("rpcSyncFilterMarks", filterInventory.serializeNBT(level.registryAccess()));
    }

    /** C→S：标记写入/清除。index=-1 最小空槽；mark 空即清除该槽。 */
    @RPCMethod
    public void rpcSetFilterMark(RPCSender sender, int index, ItemStack mark) {
        if (sender.isRemote()) {
            setFilterMark(index, mark);
        }
    }

    /** C→S：GUI 打开时客户端请求全量（服务端单播回复）。 */
    @RPCMethod
    public void rpcRequestFilterSync(RPCSender sender) {
        if (sender.isRemote() && sender.asPlayer() != null) {
            rpcToPlayer(sender.asPlayer(), "rpcSyncFilterMarks",
                    filterInventory.serializeNBT(level.registryAccess()));
        }
    }

    /** S→C：全量标记写回客户端 filterInventory（展示层）。 */
    @RPCMethod
    public void rpcSyncFilterMarks(RPCSender sender, net.minecraft.nbt.Tag filterTag) {
        if (sender.isServer() && filterTag instanceof CompoundTag ct) {
            filterInventory.deserializeNBT(level.registryAccess(), ct);
        }
    }

    /** 重算输出面位图镜像（服务端；@DescSynced 变化时自动推客户端）。 */
    public void refreshPullFacesMask() {
        byte mask = 0;
        for (Direction d : Direction.values()) {
            if (transmitter.getConnectionTypeRaw(d) == ConnectionType.PULL) {
                mask |= (byte) (1 << d.ordinal());
            }
        }
        pullFacesMask = mask;
    }

    public boolean isFiltered() {
        return isWhitelist() || isBlacklist();
    }

    public boolean isWhitelist() {
        return "pipe_white".equals(SafeOperationHelper.regNameOf(getBlockState().getBlock()));
    }

    public boolean isBlacklist() {
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
        boolean allowed = isWhitelist() ? matched : !matched;
        return allowed;
    }

    /** active 仅修改渲染态位，保持 BlockState.ACTIVE 为 false。 */
    private void setActive(boolean active) {
        int next = CableRenderState.withActive(renderPacked, active);
        if (renderPacked != next) {
            renderPacked = next;
            markRenderDirty();
        }
    }
}
