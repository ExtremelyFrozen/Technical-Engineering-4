package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PipeBlockEntity extends CmBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 过滤标记物存储键（readTileData/writeTileData 持久化，BE 存档 CompoundTag 序列化）。
     */
    private static final String FILTER_KEY = "filter";

    /** 单页过滤槽数 = 3×9 网格（每页一屏；页数 = 1+pageLevel，见 {@link #getFilterPageCount()}）。 */
    private static final int FILTER_SLOTS_PER_PAGE = 27;

    // 偏差 #1：过滤操作区单页 3×9=27 槽（白名单/黑名单标记物），玩家在配置 GUI 中放入标记物。
    // 偏差 #3（扩写升级）：总槽 = 27×(1+pageLevel)（pageLevel 0=1 页/27 槽，上限 63=64 页/1728 槽）；
    // 升级/读档时经 setSize 动态扩容（见 setUpgradeLevel PAGE 分支与 readTileData）。
    private final MachineItemHandler filterInventory = new MachineItemHandler(FILTER_SLOTS_PER_PAGE);

    // ── 管道独特升级等级（@Persisted @DescSynced：存档 + 客户端实时推送，
    // Jade 状态显示依赖实时同步；与 readTileData/writeTileData 手动读写冗余共存，值一致）──
    @Persisted
    @DescSynced
    private int pullLevel;
    @Persisted
    @DescSynced
    private int pushLevel;
    @Persisted
    @DescSynced
    private int speedLevel;
    @Persisted
    @DescSynced
    private int pageLevel;
    @Persisted
    @DescSynced
    private int enderLevel;
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

    // ── 管道独特升级 ────────────────────────────────────────────────

    public int getUpgradeLevel(PipeUpgradeType type) {
        return switch (type) {
            case PULL -> pullLevel;
            case PUSH -> pushLevel;
            case SPEED -> speedLevel;
            case PAGE -> pageLevel;
            case ENDER -> enderLevel;
        };
    }

    /** 该升级是否还可再升一级（当前等级 &lt; 上限）。 */
    public boolean canUpgrade(PipeUpgradeType type) {
        return getUpgradeLevel(type) < type.maxLevel();
    }

    /** 升级 +1 并标记存档变更；已达上限返回 false（不改变任何状态）。 */
    public boolean doUpgrade(PipeUpgradeType type) {
        if (!canUpgrade(type)) {
            return false;
        }
        setUpgradeLevel(type, getUpgradeLevel(type) + 1);
        markDirty();
        return true;
    }

    private void setUpgradeLevel(PipeUpgradeType type, int level) {
        switch (type) {
            case PULL -> pullLevel = level;
            case PUSH -> pushLevel = level;
            case SPEED -> speedLevel = level;
            case PAGE -> {
                pageLevel = level;
                // 扩写升级：容量随页数扩容（27×(1+pageLevel)），新页槽立即可用
                filterInventory.setSize(filterSlotCount());
            }
            case ENDER -> enderLevel = level;
        }
    }

    /** 过滤页数 = 1 + pageLevel（扩写 0 级=1 页；上限 63 级=64 页）。 */
    public int getFilterPageCount() {
        return 1 + pageLevel;
    }

    /** 过滤总槽数 = 每页 27 槽 × 页数（最多 64×27 = 1728 槽）。 */
    private int filterSlotCount() {
        return FILTER_SLOTS_PER_PAGE * getFilterPageCount();
    }

    /**
     * 右键管道手持升级物的交互入口（BaseMachineBlock.useItemOn 优先拦截）。
     * 非升级物 → PASS（走原逻辑）；升级成功/已达上限 → 反馈玩家并 CONSUME。
     */
    public InteractionResult tryApplyUpgrade(ItemStack stack, Player player) {
        PipeUpgradeType type = PipeUpgradeType.fromItem(stack.getItem());
        if (type == null) {
            return InteractionResult.PASS;
        }
        if (level == null || level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (doUpgrade(type)) {
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            player.sendSystemMessage(Component.translatable(
                    ComponentHelper.getKey("pipe.upgrade.success"),
                    Component.translatable(ComponentHelper.getKey(type.nameKey())),
                    getUpgradeLevel(type), type.maxLevel()).withStyle(ChatFormatting.GREEN));
            return InteractionResult.CONSUME;
        }
        player.sendSystemMessage(Component.translatable(
                ComponentHelper.getKey("pipe.upgrade.max"),
                Component.translatable(ComponentHelper.getKey(type.nameKey()))).withStyle(ChatFormatting.RED));
        return InteractionResult.CONSUME;
    }

    public boolean hasUi() {
        return isFiltered();
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        // 新传输模型（方案 A）：root 每 tick 撮合一次 —— 收集网络内全部 Pull 源与 Push 目标，
        // 做「源→目标」空间优先分配并执行（无缓冲直通，物品不留网络）。
        // root 前剪枝（低成本局部短路）：存在按 x→y→z 字典序更小的相邻管道 ⇒ 本管道必非网络
        // root（相邻管道同网络，root = 网络最小坐标，与 isRoot 判定同序），免去非 root 管道每
        // tick 一次整网络 BFS —— 仅局部极小点执行 collectConnected 确认（通常每网络每 tick 1 次，
        // 见 docs/pipe-transport-redesign.md §3.5 注记）。
        if (TransferNetworks.hasSmallerNeighbor(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof PipeBlockEntity)) {
            return;
        }
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof PipeBlockEntity);
        if (!TransferNetworks.isRoot(network, worldPosition)) {
            return;
        }
        matchAndTransfer(network);
    }

    /**
     * 单次传输量 = (1 + 7×speedLevel) × 2^enderLevel（public 供客户端 Jade 状态显示读取）。
     * long 计算防 int 溢出（速度 9 级 → 64，末影 8 级 → ×256 = 16384），
     * 钳制 int（IItemHandler.extractItem/insertItem 的 amount 参数为 int）。
     */
    public int singleTransferAmount() {
        long amount = (1 + 7L * speedLevel) << enderLevel;
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    // ── 新传输模型（方案 A）：root 每 tick 撮合 ──────────────────────────────

    /** Pull 源候选：某源容器某槽 simulate 可抽取 ≤ perBeatLimit 且通过源侧过滤。 */
    private record PullCandidate(IItemHandler source, int slot, BlockPos sourcePos,
                                 int perBeatLimit, PipeBlockEntity pullPipe, ItemStack preview) {}

    /** Push 目标：某相邻容器可接收空间 = Σ(getSlotLimit - 当前量)。 */
    private record PushTarget(IItemHandler sink, BlockPos sinkPos, int freeSpace, PipeBlockEntity pushPipe) {}

    /** 每源限量计数键：同一 Pull 管道 + 同一源容器为一组（各 Pull 管道独立限量）。 */
    private record PullSourceKey(PipeBlockEntity pipe, BlockPos sourcePos) {}

    /**
     * 管道网络传输角色（主动/被动互补模型）：
     * <ul>
     *   <li><b>主动 Pull</b>：装了粘性活塞（pullLevel&gt;0）的管道，只从相邻容器抽入进网络</li>
     *   <li><b>主动 Push</b>：装了活塞（pushLevel&gt;0）的管道，只把网络物品推出到相邻容器</li>
     *   <li><b>被动端点</b>：无 Pull/Push 升级（pullLevel==pushLevel==0）的管道，方向由网络
     *       主动端点互补决定（{@link #passiveRoleForNetwork}）——支持主动端点完成网络 IO，无需
     *       「源管装 Pull + 目标管装 Push」的对照配置</li>
     * </ul>
     */
    private enum PipeRole {
        PULL, PUSH, NONE
    }

    /**
     * 网络级 round-robin 轮转指针（仅 root 使用；同空间目标轮转打破平局，
     * 防多源在同一节拍反复竞争同一目标导致振荡）。不持久化，重建时自然重排。
     */
    private long roundRobinIndex;

    /** 收集网络内全部 Pull 源候选：pullLevel>0 管道 × 6 面相邻容器（跳过管道），simulate 抽取 + 源侧过滤。 */
    private List<PullCandidate> collectPullSources(Set<BlockPos> network, PipeRole passiveRole) {
        List<PullCandidate> candidates = new ArrayList<>();
        for (BlockPos pipePos : network) {
            if (!(level.getBlockEntity(pipePos) instanceof PipeBlockEntity pipe) || !isPullActor(pipe, passiveRole)) {
                continue;
            }
            int perBeatLimit = pipe.singleTransferAmount();
            for (Direction direction : Direction.values()) {
                BlockPos sourcePos = pipePos.relative(direction);
                if (level.getBlockEntity(sourcePos) instanceof PipeBlockEntity) {
                    continue;
                }
                IItemHandler source = TransferNetworks.getItems(level, sourcePos, direction.getOpposite());
                if (source == null || !canHoldItems(source)) {
                    continue; // 仅"可容纳物品"的方块才算作被连接到网络的容器
                }
                for (int slot = 0; slot < source.getSlots(); slot++) {
                    ItemStack simulated = source.extractItem(slot, perBeatLimit, true);
                    if (simulated.isEmpty() || !pipe.isItemAllowed(simulated)) {
                        continue;
                    }
                    candidates.add(new PullCandidate(source, slot, sourcePos, perBeatLimit, pipe, simulated.copy()));
                }
            }
        }
        return candidates;
    }

    /** 收集网络内全部 Push 目标：主动 Push 管道 + 被动互补(PUSH)管道 × 6 面相邻容器（跳过管道），可接收空间 = Σ(槽上限 - 当前量)。 */
    private List<PushTarget> collectPushTargets(Set<BlockPos> network, PipeRole passiveRole) {
        List<PushTarget> targets = new ArrayList<>();
        for (BlockPos pipePos : network) {
            if (!(level.getBlockEntity(pipePos) instanceof PipeBlockEntity pipe) || !isPushActor(pipe, passiveRole)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos sinkPos = pipePos.relative(direction);
                if (level.getBlockEntity(sinkPos) instanceof PipeBlockEntity) {
                    continue;
                }
                IItemHandler sink = TransferNetworks.getItems(level, sinkPos, direction.getOpposite());
                if (sink == null || !canHoldItems(sink)) {
                    continue; // 仅"可容纳物品"的方块才算作被连接到网络的容器
                }
                int freeSpace = 0;
                for (int slot = 0; slot < sink.getSlots(); slot++) {
                    ItemStack stack = sink.getStackInSlot(slot);
                    freeSpace += Math.max(sink.getSlotLimit(slot) - stack.getCount(), 0);
                }
                if (freeSpace > 0) {
                    targets.add(new PushTarget(sink, sinkPos, freeSpace, pipe));
                }
            }
        }
        return targets;
    }

    /**
     * 网络级互补方向判定：主动端点（pullLevel&gt;0 / pushLevel&gt;0）决定被动端点（无升级）的传输角色。
     * <ul>
     *   <li>有主动 Pull、无主动 Push → 被动端点全部为 {@link PipeRole#PUSH}（互补推出，支持主动抽入）</li>
     *   <li>有主动 Push、无主动 Pull → 被动端点全部为 {@link PipeRole#PULL}（互补抽入，支持主动推出）</li>
     *   <li>主动 Pull+Push 同时存在 → 被动 {@link PipeRole#NONE}（两方向已由主动端点覆盖）</li>
     *   <li>全无主动端点 → {@link PipeRole#NONE}（网络不传输，需至少一个升级化端点）</li>
     * </ul>
     */
    private PipeRole passiveRoleForNetwork(Set<BlockPos> network) {
        boolean pullActive = false;
        boolean pushActive = false;
        for (BlockPos pipePos : network) {
            if (!(level.getBlockEntity(pipePos) instanceof PipeBlockEntity pipe)) {
                continue;
            }
            if (pipe.pullLevel > 0) pullActive = true;
            if (pipe.pushLevel > 0) pushActive = true;
        }
        if (pullActive && !pushActive) return PipeRole.PUSH;
        if (pushActive && !pullActive) return PipeRole.PULL;
        return PipeRole.NONE;
    }

    /**
     * 该管道是否作为 Pull 源参与：主动 Pull（pullLevel&gt;0），或被动端点（无 Pull/Push 升级）
     * 且网络互补方向为 {@link PipeRole#PULL}（互补抽入支持主动推出）。主动 Push 管道不抽入。
     */
    private static boolean isPullActor(PipeBlockEntity pipe, PipeRole passiveRole) {
        if (pipe.pullLevel > 0) {
            return true; // 主动 Pull
        }
        if (pipe.pullLevel == 0 && pipe.pushLevel == 0) {
            return passiveRole == PipeRole.PULL; // 被动端点互补抽入
        }
        return false; // 主动 Push（或同时主动）不额外作为纯 Pull
    }

    /**
     * 该管道是否作为 Push 目标参与：主动 Push（pushLevel&gt;0），或被动端点（无 Pull/Push 升级）
     * 且网络互补方向为 {@link PipeRole#PUSH}（互补推出支持主动抽入）。主动 Pull 管道不推出。
     */
    private static boolean isPushActor(PipeBlockEntity pipe, PipeRole passiveRole) {
        if (pipe.pushLevel > 0) {
            return true; // 主动 Push
        }
        if (pipe.pullLevel == 0 && pipe.pushLevel == 0) {
            return passiveRole == PipeRole.PUSH; // 被动端点互补推出
        }
        return false; // 主动 Pull（或同时主动）不额外作为纯 Push
    }

    /** 空间优先排序：按剩余可接收量降序；同分分组内按 round-robin 指针循环位移破平局（防振荡）。 */
    private void sortTargetsSpaceFirst(List<PushTarget> targets) {
        targets.sort(Comparator.comparingInt(PushTarget::freeSpace).reversed());
        if (targets.size() <= 1) {
            return;
        }
        int offset = (int) (roundRobinIndex % targets.size());
        List<PushTarget> rotated = new ArrayList<>(targets.size());
        int i = 0;
        while (i < targets.size()) {
            int score = targets.get(i).freeSpace();
            int j = i;
            while (j < targets.size() && targets.get(j).freeSpace() == score) {
                j++;
            }
            int groupOffset = offset % (j - i);
            for (int k = 0; k < j - i; k++) {
                rotated.add(targets.get(i + (k + groupOffset) % (j - i)));
            }
            i = j;
        }
        targets.clear();
        targets.addAll(rotated);
        roundRobinIndex++;
    }

    /**
     * root 每 tick 撮合：收集 Pull 源与 Push 目标 → 空间优先+round-robin 分配 →
     * simulate insert 先行确认 → 真实 extract/insert → 剩余退回源（兜底）。
     * 双侧过滤 AND（源侧 + 目标侧）、目标排除同源、每源每节拍限量。
     */
    private void matchAndTransfer(Set<BlockPos> network) {
        // 网络级主动/被动互补方向：主动端点（升级管道）指定方向，被动端点（无升级）自动互补，
        // 无需「源管装 Pull + 目标管装 Push」对照配置即可完成网络 IO。
        PipeRole passiveRole = passiveRoleForNetwork(network);
        List<PullCandidate> candidates = collectPullSources(network, passiveRole);
        List<PushTarget> targets = collectPushTargets(network, passiveRole);
        if (candidates.isEmpty() || targets.isEmpty()) {
            // 无 Pull 源或无 Push 目标：无缓冲直通模型下不搬动（物品留在源容器）
            setActive(false);
            return;
        }
        sortTargetsSpaceFirst(targets);
        Map<PullSourceKey, Integer> movedBySource = new HashMap<>();
        boolean movedAny = false;
        for (PullCandidate candidate : candidates) {
            PullSourceKey key = new PullSourceKey(candidate.pullPipe, candidate.sourcePos);
            int remaining = candidate.perBeatLimit - movedBySource.getOrDefault(key, 0);
            if (remaining <= 0) {
                continue; // 每源每节拍限量已耗尽
            }
            int moveAmount = Math.min(candidate.preview.getCount(), remaining);
            for (PushTarget target : targets) {
                if (target.sinkPos.equals(candidate.sourcePos)) {
                    continue; // 目标排除同源：物品不回自身
                }
                if (!target.pushPipe.isItemAllowed(candidate.preview)) {
                    continue; // 目标侧过滤（AND：源侧已在收集时过滤）
                }
                ItemStack toInsert = candidate.preview.copy();
                toInsert.setCount(moveAmount);
                ItemStack simLeft = TransferNetworks.insertItem(target.sink, toInsert, true);
                int accepted = moveAmount - simLeft.getCount();
                if (accepted <= 0) {
                    continue; // 目标无空间，换下一个目标
                }
                ItemStack extracted = candidate.source.extractItem(candidate.slot, accepted, false);
                if (extracted.isEmpty()) {
                    continue;
                }
                // 防御竞态：真实 extract 的物品可能与 simulate 的 preview 不同（第三方并发修改源），
                // 目标侧过滤按真实物品复查（simulate 基于 preview 可能误放行黑名单物品）。
                if (!target.pushPipe.isItemAllowed(extracted)) {
                    // 目标过滤拒绝：物品未进入目标，退回源（失败则掉落兜底），结束该候选待下节拍重试。
                    ItemStack back = TransferNetworks.insertItem(candidate.source, extracted, false);
                    if (!back.isEmpty()) {
                        spawnRollbackItem(back, candidate.sourcePos, target.sinkPos);
                    }
                    break;
                }
                ItemStack leftover = TransferNetworks.insertItem(target.sink, extracted.copy(), false);
                if (!leftover.isEmpty()) {
                    // 兜底退回源容器（simulate 先行已保证可放，此处仅防御竞态）
                    ItemStack rollbackLeftover = TransferNetworks.insertItem(candidate.source, leftover, false);
                    if (!rollbackLeftover.isEmpty()) {
                        // 退回失败（竞态致源/目标均拒收）：不吞物品——掉落实体到目标附近空气格（可见可拾取）
                        spawnRollbackItem(rollbackLeftover, candidate.sourcePos, target.sinkPos);
                    }
                }
                movedBySource.merge(key, extracted.getCount(), Integer::sum);
                movedAny = true;
                break; // 该候选只搬一次
            }
        }
        setActive(movedAny);
    }

    /**
     * 该邻居方块是否"可用容纳物品"（存在至少一个容量 &gt; 0 的槽位）。
     * 只有可容纳物品的方块才算作被连接到网络的容器；空壳 handler（0 槽 / 全零容量）不算，
     * 避免把不能装物品的方块误识别为网络 Pull 源 / Push 目标。
     */
    private static boolean canHoldItems(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getSlotLimit(slot) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 竞态兜底：源/目标均拒收时生成掉落实体到目标附近空气格（优先上方，其次 6 面），
     * 保证物品绝不消失且可见可拾取（而非卡在实心方块内部不可见）。全部非空气时回退目标中心。
     */
    private void spawnRollbackItem(ItemStack stack, BlockPos sourcePos, BlockPos targetPos) {
        LOGGER.warn("Pipe rollback failed: {} item(s) could not be returned to source {}, spawning item entity at {}",
                stack.getCount(), sourcePos, targetPos);
        if (level == null) {
            return;
        }
        BlockPos spawnPos = findAirSpawnPos(targetPos);
        if (spawnPos == null) {
            spawnPos = targetPos;
        }
        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                level, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, stack);
        level.addFreshEntity(entity);
    }

    /** 寻找 targetPos 附近空气格：优先上方（玩家易见可拾取），其次 6 面；全非空气返回 null。 */
    private BlockPos findAirSpawnPos(BlockPos targetPos) {
        if (level == null) {
            return null;
        }
        BlockPos above = targetPos.above();
        if (level.getBlockState(above).isAir()) {
            return above;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = targetPos.relative(direction);
            if (level.getBlockState(neighbor).isAir()) {
                return neighbor;
            }
        }
        return null;
    }

    // ── 翻页控件坐标（偏差 #3：扩写升级多页 GUI）──
    // 3×9 网格占 x=7..169、y=24..78，右侧无纵向空间（垂直排布会遮挡网格），
    // 玩家物品栏 y=83..140。唯一可用横带 = 标题行 y=6..18：页码 Label 居左、
    // ▲▼ 按钮水平排列贴右缘（150..175），与标题/网格/物品栏均无重叠。
    private static final int PAGE_CONTROLS_Y = 6;
    private static final int PAGE_LABEL_X = 100;
    private static final int PREV_BUTTON_X = 150;  // ▲ 上一页（右缘 162）
    private static final int NEXT_BUTTON_X = 163;  // ▼ 下一页（右缘 175，贴 GUI 右缘）

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.MACHINE_GUI);
        if (isFiltered()) {
            // 标题标注白/黑名单模式（按方块种类）；操作区每页 3×9=27 个标记物槽位
            String modeKey = isWhitelist() ? "kenergyengineering.pipe.filter.whitelist" : "kenergyengineering.pipe.filter.blacklist";
            root.addChild(label(8, 6, ComponentHelper.translated(modeKey)));
            int pageCount = getFilterPageCount();
            int[] currentPage = { 0 }; // 翻页浏览态：UI 局部状态，不持久化（与频道列表 ChannelUIState 同款）
            // 一次性创建全部页槽：vanilla slot 列表在 menu 构建时固定，服务端/客户端同步一致；
            // 翻页仅 setDisplay 切换（隐藏元素 TaffyDisplay.NONE 不参与布局/渲染），无网络交互。
            List<ItemSlot> pageSlots = new ArrayList<>(filterInventory.getSlots());
            for (int page = 0; page < pageCount; page++) {
                for (int i = 0; i < FILTER_SLOTS_PER_PAGE; i++) {
                    int slotIndex = page * FILTER_SLOTS_PER_PAGE + i;
                    int x = 7 + (i % 9) * 18;
                    int y = 24 + (i / 9) * 18;
                    ItemSlot slot = filterSlot(slotIndex, x, y);
                    slot.setDisplay(page == 0);
                    pageSlots.add(slot);
                    root.addChild(slot);
                }
            }
            Label pageLabel = label(PAGE_LABEL_X, PAGE_CONTROLS_Y + 3, pageText(currentPage[0], pageCount));
            root.addChild(pageLabel);
            // ▲ 上一页 / ▼ 下一页：点击钳制到 [0, pageCount-1] 后切换当前页显示
            Button prevButton = scrollButton(PREV_BUTTON_X, PAGE_CONTROLS_Y, TENConstants.SCROLL_UP_NORMAL, TENConstants.SCROLL_UP_HOVER);
            prevButton.setOnClick(event -> {
                if (currentPage[0] > 0) {
                    currentPage[0]--;
                    refreshPage(pageSlots, pageLabel, currentPage[0], pageCount);
                }
            });
            Button nextButton = scrollButton(NEXT_BUTTON_X, PAGE_CONTROLS_Y, TENConstants.SCROLL_DOWN_NORMAL, TENConstants.SCROLL_DOWN_HOVER);
            nextButton.setOnClick(event -> {
                if (currentPage[0] < pageCount - 1) {
                    currentPage[0]++;
                    refreshPage(pageSlots, pageLabel, currentPage[0], pageCount);
                }
            });
            root.addChild(prevButton);
            root.addChild(nextButton);
        }
        TENMachineBlockUIFactory.addPlayerInventory(root);
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    /** 页码文本「第 x/共 y 页」（x 为 1 基）。 */
    private static Component pageText(int currentPage, int pageCount) {
        return Component.translatable(ComponentHelper.getKey("pipe.filter.page"), currentPage + 1, pageCount);
    }

    /** 翻页刷新：仅当前页 27 槽可见，其余页隐藏；同步页码 Label。 */
    private static void refreshPage(List<ItemSlot> pageSlots, Label pageLabel, int currentPage, int pageCount) {
        for (int page = 0; page < pageCount; page++) {
            for (int i = 0; i < FILTER_SLOTS_PER_PAGE; i++) {
                pageSlots.get(page * FILTER_SLOTS_PER_PAGE + i).setDisplay(page == currentPage);
            }
        }
        pageLabel.setText(pageText(currentPage, pageCount));
    }

    /**
     * channel_buttons sheet 12×12 翻页按钮：LDLib2 Button + buttonStyle 三态
     * （base=normal 行0、hover=hover 行1、pressed 复用 hover），与频道列表翻页同款。
     */
    private static Button scrollButton(int x, int y, TENConstants.SheetUV normal, TENConstants.SheetUV hover) {
        Button button = new Button();
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

    @Override
    protected void readTileData(ValueInput input) {
        super.readTileData(input);
        // 偏差 #3：先恢复 pageLevel 以确定过滤容量（总槽 = 27×(1+pageLevel)），再读过滤槽；
        // 旧存档缺键 → 0 → 容量保持单页 27 槽，不炸旧存档。
        pageLevel = input.getInt("pageLevel").orElse(0);
        filterInventory.setSize(filterSlotCount());
        // 偏差 #1：过滤标记物持久化——缺键/旧存档 → 保持空过滤（与 CmMachineBlockEntity 存档约定一致）
        input.read(FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC)).ifPresent(stacks -> {
            for (int i = 0; i < Math.min(stacks.size(), filterInventory.getSlots()); i++) {
                filterInventory.setStackInSlot(i, stacks.get(i));
            }
        });
        // 偏差 #2：管道独特升级等级持久化——缺键/旧存档 → 0（与过滤约定一致，不炸旧存档）
        pullLevel = input.getInt("pullLevel").orElse(0);
        pushLevel = input.getInt("pushLevel").orElse(0);
        speedLevel = input.getInt("speedLevel").orElse(0);
        enderLevel = input.getInt("enderLevel").orElse(0);
    }

    @Override
    protected void writeTileData(ValueOutput output) {
        super.writeTileData(output);
        var stacks = new ArrayList<ItemStack>();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            stacks.add(filterInventory.getStackInSlot(i));
        }
        output.store(FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC), stacks);
        output.store("pullLevel", Codec.INT, pullLevel);
        output.store("pushLevel", Codec.INT, pushLevel);
        output.store("speedLevel", Codec.INT, speedLevel);
        output.store("pageLevel", Codec.INT, pageLevel);
        output.store("enderLevel", Codec.INT, enderLevel);
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
