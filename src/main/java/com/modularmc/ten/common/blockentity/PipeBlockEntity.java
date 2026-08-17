package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.mojang.serialization.Codec;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.ArrayList;

/**
 * 物品管道（pipe / pipe_white / pipe_black）——逐级传递模型。
 *
 * <p>
 * 设计（化繁为简，参考 Pipez 式独立管道）：
 * <ul>
 * <li><b>逐级传递</b>：每根管道独立 tick，无网络 root 撮合、无 Pull/Push 角色区分、
 * 无主动/被动互补。管道从相邻容器抽取物品到内部缓冲（64/tick），再把缓冲推送到
 * 相邻容器（到达目标）或相邻管道（接力前进）；物品沿管道网络逐跳移动。</li>
 * <li><b>统一速率</b>：所有管道传输速度固定 {@link #TRANSFER_RATE}（64 物品/tick），
 * 无升级系统（已移除 Pull/Push/Speed/Page/Ender 升级物与等级）。</li>
 * <li><b>物品安全</b>：物品只存在于容器或管道缓冲（存档持久化）中，不会消失；
 * 推送失败自然留缓冲等待下 tick，无需掉落兜底。</li>
 * <li><b>过滤</b>：pipe_white（白名单）/ pipe_black（黑名单）变体保留，3×9=27 槽
 * 标记物配置（固定 1 页，无扩写升级）。普通 pipe 无过滤。</li>
 * </ul>
 * <p>
 * 与机器交互：管道只能从机器「被动输出 / 被动双向」面抽取（机器 side 门控拒绝其余面），
 * 只能向「被动输入 / 被动双向」面推入；机器的「主动输入 / 主动输出」由机器自身
 * tick 执行（见 CmMachineBlockEntity 主动 IO），与管道无关。
 */
public class PipeBlockEntity extends CmBlockEntity {

    /** 过滤标记物存储键（readTileData/writeTileData 持久化，BE 存档 CompoundTag 序列化）。 */
    private static final String FILTER_KEY = "filter";
    /** 逐级传递缓冲存储键（管道内物品存档持久化，卸载/存档不丢失）。 */
    private static final String BUFFER_KEY = "buffer";
    /** 过滤槽数 = 3×9 = 27（固定 1 页；扩写升级已移除）。 */
    private static final int FILTER_SLOTS = 27;
    /** 统一传输速度：每根管道每 tick 最多传输 64 个物品（化繁为简：所有管道同一速率）。 */
    public static final int TRANSFER_RATE = 64;
    /** 管道内部缓冲容量（逐级传递中间态；单物品类型，最多一组 64）。 */
    private static final int BUFFER_CAPACITY = 64;

    /** 过滤标记物（pipe_white/pipe_black 的配置槽；普通 pipe 不参与）。 */
    private final MachineItemHandler filterInventory = new MachineItemHandler(FILTER_SLOTS);

    /** 逐级传递缓冲：物品沿管道网络逐跳移动的中间态（单物品类型，容量 64，存档持久化）。 */
    private ItemStack buffer = ItemStack.EMPTY;
    /** 本 tick 的抽取来源方向：推送时跳过（防相邻管道间回传震荡）。 */
    private Direction lastSourceDir;

    /**
     * 抽入点面配置：用扳手右键管道与方块的连接端，将该连接段设为抽入点（管道主动拉取）。
     * 未配置的面仅作为被动 IO（管道不主动拉取，但可作为推送目标被动接收）。
     */
    private final boolean[] pullSides = new boolean[6];

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

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        filterInventory.setChangeListener(this::markDirty);
    }

    public MachineItemHandler getFilterInventory() {
        return filterInventory;
    }

    /** 是否有配置 UI（仅过滤管道 pipe_white/pipe_black 有过滤配置 GUI）。 */
    public boolean hasUi() {
        return isFiltered();
    }

    /** 管道缓冲当前物品（public 供客户端 Jade 状态显示读取）。 */
    public ItemStack getBuffer() {
        return buffer;
    }

    /** 该连接端（面）是否为抽入点（管道主动拉取）。public 供客户端/纹理/Jade 读取。 */
    public boolean isPullSide(Direction side) {
        return pullSides[side.get3DDataValue()];
    }

    /** 切换连接端抽入点状态（扳手右键交互）。返回切换后的状态。 */
    public boolean togglePullSide(Direction side) {
        int idx = side.get3DDataValue();
        pullSides[idx] = !pullSides[idx];
        markDirty();
        setChanged();
        return pullSides[idx];
    }

    // ── 逐级传递（每根管道独立 tick，无需网络 root 撮合）──────────────────

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        lastSourceDir = null;
        // 1. 推送：缓冲 → 相邻容器（到达目标）或相邻管道（接力），每 tick 最多 TRANSFER_RATE
        if (!buffer.isEmpty()) {
            pushBuffer();
        }
        // 2. 抽取：相邻容器 → 缓冲，每 tick 最多 TRANSFER_RATE
        if (buffer.getCount() < BUFFER_CAPACITY) {
            pullFromContainers();
        }
        setActive(!buffer.isEmpty());
    }

    /**
     * 推送缓冲 → 相邻容器（优先，物品到达目标即停止接力）或相邻管道（接力前进）。
     * 跳过本 tick 抽取来源方向（防相邻管道间回传震荡）。每 tick 总推送量 ≤ TRANSFER_RATE。
     */
    private void pushBuffer() {
        int budget = Math.min(buffer.getCount(), TRANSFER_RATE);
        // 1) 相邻容器优先：物品到达目标容器
        for (Direction direction : Direction.values()) {
            if (budget <= 0 || buffer.isEmpty()) {
                break;
            }
            if (direction == lastSourceDir) {
                continue;
            }
            BlockPos targetPos = worldPosition.relative(direction);
            if (level.getBlockEntity(targetPos) instanceof PipeBlockEntity) {
                continue;
            }
            IItemHandler sink = TransferNetworks.getItems(level, targetPos, direction.getOpposite());
            if (sink == null || !canHoldItems(sink)) {
                continue;
            }
            if (!isItemAllowed(buffer)) {
                continue; // 目标侧过滤（AND：本管道过滤标记物）
            }
            int toPush = Math.min(buffer.getCount(), budget);
            ItemStack toInsert = buffer.copy();
            toInsert.setCount(toPush);
            ItemStack leftover = TransferNetworks.insertItem(sink, toInsert, false);
            int accepted = toPush - leftover.getCount();
            if (accepted > 0) {
                buffer.shrink(accepted);
                budget -= accepted;
                markDirty();
            }
        }
        // 2) 相邻管道接力：缓冲剩余传给相邻管道（物品继续沿网络前进）
        if (budget > 0 && !buffer.isEmpty()) {
            for (Direction direction : Direction.values()) {
                if (budget <= 0 || buffer.isEmpty()) {
                    break;
                }
                if (direction == lastSourceDir) {
                    continue;
                }
                BlockPos targetPos = worldPosition.relative(direction);
                if (!(level.getBlockEntity(targetPos) instanceof PipeBlockEntity pipe)) {
                    continue;
                }
                int accepted = pipe.tryReceive(buffer, direction.getOpposite());
                if (accepted > 0) {
                    buffer.shrink(accepted);
                    budget -= accepted;
                    markDirty();
                }
            }
        }
    }

    /**
     * 从相邻容器抽取到缓冲（每 tick 最多 TRANSFER_RATE；源侧过滤）。
     * 机器侧：只有「被动输出 / 被动双向」面允许被抽取（side 门控返回空则跳过）；
     * 普通容器（箱子等）默认允许。
     */
    private void pullFromContainers() {
        int space = BUFFER_CAPACITY - buffer.getCount();
        if (space <= 0) {
            return;
        }
        int remaining = Math.min(space, TRANSFER_RATE);
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                break;
            }
            if (!pullSides[direction.get3DDataValue()]) {
                continue; // 仅抽入点面（扳手配置）才主动拉取；未配置面为被动 IO
            }
            BlockPos sourcePos = worldPosition.relative(direction);
            if (level.getBlockEntity(sourcePos) instanceof PipeBlockEntity) {
                continue;
            }
            IItemHandler source = TransferNetworks.getItems(level, sourcePos, direction.getOpposite());
            if (source == null || !canHoldItems(source)) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots() && remaining > 0; slot++) {
                ItemStack simulated = source.extractItem(slot, remaining, true);
                if (simulated.isEmpty() || !isItemAllowed(simulated)) {
                    continue;
                }
                if (!buffer.isEmpty() && !ItemStack.isSameItemSameComponents(buffer, simulated)) {
                    continue; // 缓冲为不同物品：等待清空后再换类型（单物品类型缓冲）
                }
                ItemStack extracted = source.extractItem(slot, simulated.getCount(), false);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (buffer.isEmpty()) {
                    buffer = extracted.copy();
                } else {
                    buffer.grow(extracted.getCount());
                }
                lastSourceDir = direction;
                remaining -= extracted.getCount();
                markDirty();
            }
        }
    }

    /**
     * 相邻管道请求接收物品（管道间接力）。目标侧过滤 + 缓冲容量限制；
     * 缓冲已有不同物品时拒绝（单物品类型缓冲，等待清空后再接力）。
     * 返回实际接收数量。
     */
    public int tryReceive(ItemStack stack, Direction fromDir) {
        if (stack.isEmpty() || !isItemAllowed(stack)) {
            return 0;
        }
        if (buffer.isEmpty()) {
            int accepted = Math.min(stack.getCount(), BUFFER_CAPACITY);
            buffer = stack.copy();
            buffer.setCount(accepted);
            lastSourceDir = fromDir; // 记录来源方向：本 tick 推送不再回传
            markDirty();
            return accepted;
        }
        if (!ItemStack.isSameItemSameComponents(buffer, stack)) {
            return 0;
        }
        int space = BUFFER_CAPACITY - buffer.getCount();
        if (space <= 0) {
            return 0;
        }
        int accepted = Math.min(space, stack.getCount());
        buffer.grow(accepted);
        markDirty();
        return accepted;
    }

    // ── 过滤（pipe_white / pipe_black 方块变体）─────────────────────────

    /** 该管道是否带过滤（pipe_white / pipe_black；普通 pipe 无过滤）。public 供客户端 Jade 状态显示读取。 */
    public boolean isFiltered() {
        return isWhitelist() || isBlacklist();
    }

    /** 是否白名单管道（pipe_white，标记物匹配才放行）。public 供客户端 Jade 状态显示读取。 */
    public boolean isWhitelist() {
        return "pipe_white".equals(SafeOperationHelper.regNameOf(getBlockState().getBlock()));
    }

    /** 是否黑名单管道（pipe_black，标记物匹配则拦截）。public 供客户端 Jade 状态显示读取。 */
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
        return isWhitelist() ? matched : !matched;
    }

    // ── GUI（过滤配置：3×9 固定 27 槽，1 页；无翻页/无升级）──────────────

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.MACHINE_GUI);
        if (isFiltered()) {
            // 标题标注白/黑名单模式（按方块种类）；操作区 3×9=27 个标记物槽位
            String modeKey = isWhitelist() ? "kenergyengineering.pipe.filter.whitelist" : "kenergyengineering.pipe.filter.blacklist";
            root.addChild(label(8, 6, ComponentHelper.translated(modeKey)));
            for (int i = 0; i < filterInventory.getSlots(); i++) {
                int x = 7 + (i % 9) * 18;
                int y = 24 + (i / 9) * 18;
                root.addChild(filterSlot(i, x, y));
            }
        }
        TENMachineBlockUIFactory.addPlayerInventory(root);
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
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

    private static Label label(int x, int y, Component text) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
    }

    // ── 持久化（过滤槽 + 逐级传递缓冲；升级系统已移除）────────────────────

    @Override
    protected void readTileData(ValueInput input) {
        super.readTileData(input);
        input.read(FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC)).ifPresent(stacks -> {
            for (int i = 0; i < Math.min(stacks.size(), filterInventory.getSlots()); i++) {
                filterInventory.setStackInSlot(i, stacks.get(i));
            }
        });
        // 逐级传递缓冲：卸载/存档时管道内物品不丢失（缺键/旧存档 → 空缓冲）
        buffer = input.read(BUFFER_KEY, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        // 抽入点面配置（位掩码，缺键/旧存档 → 全被动）
        int pullMask = input.getInt("pullSides").orElse(0);
        for (int i = 0; i < 6; i++) {
            pullSides[i] = (pullMask & (1 << i)) != 0;
        }
    }

    @Override
    protected void writeTileData(ValueOutput output) {
        super.writeTileData(output);
        var stacks = new ArrayList<ItemStack>();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            stacks.add(filterInventory.getStackInSlot(i));
        }
        output.store(FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC), stacks);
        output.store(BUFFER_KEY, ItemStack.OPTIONAL_CODEC, buffer);
        int pullMask = 0;
        for (int i = 0; i < 6; i++) {
            if (pullSides[i]) {
                pullMask |= (1 << i);
            }
        }
        output.store("pullSides", Codec.INT, pullMask);
    }

    // ── 容器有效性 ─────────────────────────────────────────────────────

    /** 该邻居方块是否"可用容纳物品"（存在至少一个容量 &gt; 0 的槽位）。 */
    private static boolean canHoldItems(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getSlotLimit(slot) > 0) {
                return true;
            }
        }
        return false;
    }

    private void setActive(boolean active) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BaseMachineBlock.ACTIVE) && state.getValue(BaseMachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(BaseMachineBlock.ACTIVE, active), 3);
        }
    }
}
