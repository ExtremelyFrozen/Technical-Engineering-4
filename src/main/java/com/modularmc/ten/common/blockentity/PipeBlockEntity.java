package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
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
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
 * 物品管道方块实体：持有 {@link ItemTransmitter}，组网/拆网由
 * TransmitterNetworkRegistry 延迟驱动；网络搬运在 ItemNetwork.onUpdate 内
 * 由 root 节点统一执行（本 BE 不再自带 BFS 扫描）。
 * 白/黑名单过滤语义、transportHandler 外部插入入口、GUI 均沿袭旧版。
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
            // 必须 setChanged：幽灵槽交互经此写入，不通知则不发 BE 更新包，
            // 客户端槽视图残留交互前数据（表现：标记交互后消失，重开 GUI 才恢复）
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

    /** 外部插入入口（保持旧签名）：经网络找可达 sink 即时投递，过滤不匹配则拒收。 */
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
        // 沿袭旧版语义：仅白/黑名单变体（pipe_white/pipe_black）有过滤 UI，普通 pipe 无界面
        return isFiltered();
    }

    // ───── ITransmitterProvider：网络接入生命周期 ─────

    @Override
    public void onLoad() {
        super.onLoad();
        joinNetwork();
    }

    private void joinNetwork() {
        // 延迟到服务端首 tick：注册回调（onLoad）期间查询邻居会触发
        // 邻居 BE 创建→邻居 join→互查的无限递归（GameTest StackOverflow 已验证）。
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
        // 不走 markDirty/sendBlockUpdated：那会触发邻居 neighborChanged → refreshConnections 递归。
        // 直接传 transmitter 引用刷新外观（查自身 BE 会在注册流程中再创建 BE，见 updateConnectionState 注释）。
        setChanged();
        if (level != null && !level.isClientSide()) {
            if (getBlockState().getBlock() instanceof com.modularmc.ten.common.block.machine.CableBased cable) {
                cable.updateConnectionState(level, worldPosition, transmitter);
            }
            refreshPullFacesMask();
        }
    }

    // ───── 持久化 ─────

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        transmitter.read(registries, tag);
        // 空 tag + Size 迁移双重保护：LDLib2 GUI 同步以空 tag 反复触发 readTileData（空 tag 清空客户端残留）；
        // 旧存档（1 行 9 槽弃用时代）filter tag Size=9，deserializeNBT 按旧 Size 重建数组会把 27 槽
        // 缩回 9（GUI 只挂 9 个真实槽、二三排成纹理假槽、过滤/去重只管前 9 格——探针实证
        // filterInventorySlots=9），故 Size≠27 时逐槽读入不重建，对齐 CmMachineBlockEntity itemHandler 迁移模式
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
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        transmitter.write(registries, tag);
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

    // ───── GUI（沿袭旧版过滤槽布局）─────

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.MACHINE_GUI);
        if (isFiltered()) {
            // 白/黑名单模式标题（对齐 26.1.2 参照）
            String modeKey = isWhitelist() ? "kenergyengineering.pipe.filter.whitelist" : "kenergyengineering.pipe.filter.blacklist";
            Label modeLabel = new Label();
            modeLabel.setText(ComponentHelper.translated(modeKey));
            modeLabel.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(8);
                layout.top(6);
            });
            root.addChild(modeLabel);
            // 三排 3×9=27 过滤槽：统一 ghostFilterSlot 纯展示槽（mayPlace/mayPickup 全 false）；
            // 标记写入走 MOUSE_DOWN 事件 + rpcToServer，服务端 setFilterMark 统一去重后广播：
            // 左键+持物=标记到点击槽（去重时移动标记）、右键=清空点击槽、shift+左键+持物=标记到最小空槽；
            // acceptQuickMove(false) 保持阻断 quickMove（物品不再进转移流程，无短暂遮盖）
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
                        // 3.1 右键：清空点击槽标记
                        rpcToServer("rpcSetFilterMark", idx, ItemStack.EMPTY);
                    } else if (event.button == 0 && !carried.isEmpty()) {
                        if (Screen.hasShiftDown()) {
                            // 3.3 shift+左键：标记到最小序号空槽（去重拒绝语义在服务端）
                            rpcToServer("rpcSetFilterMark", -1, carried.copy());
                        } else {
                            // 3.2 左键：标记到/更新到点击槽（同物品已在其他槽时移动标记）
                            rpcToServer("rpcSetFilterMark", idx, carried.copy());
                        }
                    }
                });
                root.addChild(filterSlot);
            }
        }
        TENMachineBlockUIFactory.addPlayerInventory(root);
        // 3.3 shift+点击背包物品 → 标记到最小序号空槽：
        // 过滤 GUI 上下文接管背包 quickMove——背包槽 acceptQuickMove(false) 阻断原版转移，
        // MOUSE_DOWN 拦截 shift+左键 rpcToServer 标记（LDLib2 InventorySlots 组件 36 槽逐个挂）
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
        // GUI 打开请求全量标记（C→S）：服务端 createUI 也会执行（ModularUIContainerMenu 构建链），
        // rpcToServer 经 PacketDistributor.sendToServer 在专用服务器（dist=SERVER）会抛
        // IllegalStateException——必须加客户端守卫（对齐 AbstractChannelBlockEntity 目录同步模式）
        if (holder.player.level().isClientSide()) {
            rpcToServer("rpcRequestFilterSync");
        }
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    /**
     * 幽灵标记槽（纯展示）：展示服务端 filterInventory 的标记副本，不参与任何物品转移。
     * mayPlace/mayPickup/remove 全 false 零副作用——标记写入/清除全部走 GUI 层 MOUSE_DOWN 事件
     * + RPC（rpcSetFilterMark），服务端统一去重后广播。彻底规避 quickMove 试探副作用
     * （shift+点击只写第一格）与原版转移乐观更新残留（shift+连点短暂遮盖）。
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
