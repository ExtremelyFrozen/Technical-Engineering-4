package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupIce;
import com.modularmc.ten.common.item.upgrades.LevelupMagma;
import com.modularmc.ten.common.item.upgrades.LevelupMineral;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.TagHelper;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuarryBlockEntity extends RadiusMachineBlockEntity {

    private int mode;

    // ── 游标扫描状态（核心算法 + 跨重启恢复路径）──

    /** 当前层栅格游标 X（世界块坐标，区块边界内）。 */
    @Persisted
    public int scanX;

    /** 当前层栅格游标 Z（世界块坐标，区块边界内）。 */
    @Persisted
    public int scanZ;

    /** 当前扫描层 Y（machineY-1 起向下递减，低于可挖下界即视为挖尽）。 */
    @Persisted
    public int scanY;

    /**
     * 极速模式标志：false=标准逐层扫描，true=跳过空气层直达下一非空气层。
     */
    @Persisted
    @DescSynced
    public boolean scanFast;

    /**
     * 上次评估到的模式。挖尽停机后模式变化 → 游标复位重新扫描（恢复路径）。
     */
    private int lastMode = -1;

    /**
     * 游标是否已初始化。防止挖尽（scanY 低于可挖下界）后重复初始化重扫空气层空转。
     * 持久化：重启后挖尽停机状态跨重启保持。
     */
    @Persisted
    public boolean scanCursorInitialized = false;

    /**
     * 本轮扫描是否真的挖到过东西。到底后据此决定要不要以机器自身为新起点再扫一轮：
     * 挖到过 → 上层可能因下游被挖空而塌落出新的可挖层，重扫有收益；
     * 一轮完全空手 → 保持停机，避免无休止重扫空层。
     */
    @Persisted
    public boolean scanPassMined;

    /**
     * 一轮空手到底、但过程中有方块被当前工具挡下 → 停在记录点等合规工具。
     * 工具身份（物品或耐久）变化即清除，从 {@link #passStartY} 续扫，不必从头再来。
     */
    @Persisted
    public boolean awaitTool;

    /** 本轮是否有方块因工具挖不动或无掉落而被跳过，决定空手到底是挖尽还是等工具。 */
    @Persisted
    public boolean passBlockedByTool;

    /** 本轮扫描起点层，进入 awaitTool 时游标回到这里。 */
    @Persisted
    public int passStartY;

    /** 上一次看到的工具特征（物品 + 耐久），用于识别「换了一把合规工具」。 */
    private String lastToolSignature = "";

    /**
     * 本维度可挖下界：设备自主探测到的世界内容底（{@link #autoRangeMinY()}）。
     * 不能写死 {@code TENConstants.WORLD_MIN}，也不能只信维度声明的高度——模组维度两者都可能
     * 与真实地形相差几十层。
     */
    private int minScanY() {
        return autoRangeMinY();
    }

    /**
     * 极速跳层限步：findNextNonAirLayer 每调用最多跳过 MAX_LAYER_SKIP 层
     * （防单 tick 长循环；未找到则推进限步层数由 scanExhausted 判定挖尽）。
     */
    private static final int MAX_LAYER_SKIP = 32;

    public boolean isScanFast() {
        return scanFast;
    }

    /** 初始 XZ 半径 8 → 面板跨度 16 半单位；Y 轴由设备接管（世界底到自身下方一格）。 */
    public static final int BASE_XZ_RANGE = 16;

    public QuarryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 8;
        radius = 8;
        setDefaultRange(BASE_XZ_RANGE, 1, BASE_XZ_RANGE);
    }

    @Override
    protected int baseRangeCap() {
        return BASE_XZ_RANGE;
    }

    /** Y 轴自动接管：从探测到的世界内容底到自身下方一格，忽略面板的 Y 半径与 Y 偏移两行。 */
    @Override
    public boolean autoYRange() {
        return true;
    }

    @Override
    public int autoRangeMaxY() {
        return worldPosition.getY() - 1;
    }

    @Override
    public int machineType() {
        return MachineType.QUARRY;
    }

    /** 工具槽数量：槽 0..2；GUI 坐标依次为 (43,34)、(43,16)、(43,52)。 */
    public static final int TOOL_SLOTS = 3;
    /**
     * 第一个输出槽索引：工具槽 0..2，输出槽 3..14。
     * 存档兼容：老存档（1 工具槽）槽 1..12 的输出物品加载后落在新索引 1..12——
     * 其中槽 1/2 现为工具槽（非工具物品不会通过 valid 校验放入，但已存物品不会被强制清出），
     * 需玩家手动转移；槽 0（原工具槽）索引不变。
     */
    public static final int OUTPUT_START = TOOL_SLOTS;

    /** 第一个非空工具槽的物品（工具槽 0..2 顺序取首个；全空返回 EMPTY）。 */
    private ItemStack firstAvailableTool() {
        if (itemHandler == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < TOOL_SLOTS; slot++) {
            ItemStack tool = itemHandler.getStackInSlot(slot);
            if (!tool.isEmpty()) {
                return tool;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int inventorySize() {
        return 13 + (TOOL_SLOTS - 1);
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot < TOOL_SLOTS ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        if (slot < TOOL_SLOTS) {
            // 工具槽仅接受带 TOOL 组件的物品。
            return stack.has(DataComponents.TOOL);
        }
        return true;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    /**
     * 生效范围：范围面板的工作盒——XZ 以机器自身为 0 点对称展开，Y 从维度最低点到机器下方一格
     * （Y 两行只读）。
     */
    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        return List.of(adjustedRangeBox());
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 工具槽 0..2 使用 (43,34)/(43,16)/(43,52)，输出槽 3..14 按 3×4 网格排布
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 43, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 43, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 79, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 5, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 6, 133, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 7, 79, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 8, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 9, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 10, 133, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 11, 79, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 12, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 13, 115, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 14, 133, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 74, true));
            // 扫描 / 附魔切换位于左缘 7px 内缩处，范围显隐位于右缘默认坐标
            root.addChild(TENMachineBlockUIFactory.scanModeToggleButton(this::isScanFast, () -> rpcToServer("rpcToggleScanFast"),
                    TENMachineBlockUIFactory.TOGGLE_LEFT_X, TENMachineBlockUIFactory.TOGGLE_Y));
            root.addChild(TENMachineBlockUIFactory.useEnchantmentsToggleButton(this,
                    TENMachineBlockUIFactory.TOGGLE_LEFT_X + 14, TENMachineBlockUIFactory.TOGGLE_Y));
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this));
        });
    }

    /** 扫描游标/工作区变化没有事件源，靠探针复查；缩短以满足响应预算。 */
    @Override
    protected int idleProbePeriod() {
        return 10;
    }

    @Override
    public void tick() {
        super.tick();
        if (getAliveTime() % 10 == 0) {
            updateMode();
        }
        // 挖尽停机后的恢复路径：只有模式变化会复位游标重扫（范围改由面板越界防御兜底）。
        // 运行态持续跟踪 lastMode，避免停机瞬间把既有状态误判为「变化」而误复位。
        if (scanCursorInitialized && scanExhausted()) {
            if (lastMode != mode) {
                lastMode = mode;
                resetScanCursor();
            } else if (scanPassMined) {
                // 到底且本轮确实挖到过方块 → 以机器自身为新起点再扫一轮。
                // 空手到底的那轮不复位，否则会永远重扫空气层。
                scanPassMined = false;
                resetScanCursor();
            }
        } else {
            lastMode = mode;
        }
        // 工具特征变了（新放一把、换了更高阶、或耐久不同）→ 清除等待，从记录点续扫
        String signature = toolSignature();
        if (awaitTool && !signature.equals(lastToolSignature)) {
            awaitTool = false;
            passBlockedByTool = false;
            scanPassMined = false;
        }
        lastToolSignature = signature;
    }

    /** 工具槽首个工具的特征串：物品 id + 耐久，空槽为空串。 */
    private String toolSignature() {
        ItemStack tool = firstAvailableTool();
        if (tool.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(tool.getItem()) + "@" + tool.getDamageValue();
    }

    private void updateMode() {
        if (upgradeHandler == null) {
            mode = 0;
            return;
        }
        if (hasUpgrade(LevelupIce.class)) {
            mode = 1;
            return;
        }
        if (hasUpgrade(LevelupMagma.class)) {
            mode = 2;
            return;
        }
        if (hasUpgrade(LevelupMineral.class)) {
            mode = 3;
            return;
        }
        mode = 0;
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null) {
            return;
        }
        // 挖尽停机：游标已初始化且 scanY 到世界底时立即停机并清锁，不空转耗能。
        if (scanCursorInitialized && scanExhausted()) {
            setActive(false);
            clearLockedBatch();
            return;
        }
        // 只有真正致命条件 break：工具损坏（空）/周期中途挖尽。
        // 单次未命中（无掉落/挖不动/容量满）→ continue（QuarryLoopMissVsBreak 契约）。
        int B = getLockedBatchSize();
        for (int i = 0; i < B; i++) {
            if (firstAvailableTool().isEmpty()) break; // 全部工具损坏/耗尽 → 致命 → break
            if (scanCursorInitialized && scanExhausted()) break;
            executeSingleOperation();
        }
    }

    /**
     * 挖尽停机门禁：游标已初始化且 scanY 到世界底时不锁定并行运行数、不消耗能量。
     * 游标未初始化（首次放置/旧存档）时放行，首个 applyEffect 周期会初始化游标。
     */
    @Override
    public boolean conditionStart() {
        // 无工具与被工具挡下两种「暂无可用工具」都就地停住：不耗能、游标留在原地
        if (awaitTool || firstAvailableTool().isEmpty()) {
            return false;
        }
        if (!scanCursorInitialized) {
            return true;
        }
        return !scanExhausted();
    }

    @Override
    public boolean cooking() {
        // 纯容量谓词：输出槽（3..14）无法容纳本周期 B 次操作产出时停机（保留 progress）。
        if (itemHandler == null) {
            return false;
        }
        int B = getLockedBatchSize();
        int units = 0;
        for (int i = OUTPUT_START; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            units += existing.isEmpty() ? itemHandler.getSlotLimit(i) : Math.max(0, itemHandler.getSlotLimit(i) - existing.getCount());
        }
        return units < B;
    }

    /**
     * 执行一次当前模式的单次操作。
     *
     * @return true=操作已执行；false=无法继续
     */
    private boolean executeSingleOperation() {
        return switch (mode) {
            // mode 0（常规挖掘）与 mode 3（Mineral 矿石扫描）共用游标栅格扫描
            case 0, 3 -> scanMine();
            case 1 -> giveGeneratedLoot(
                    Math.random() < 0.75 ? Items.ICE.getDefaultInstance() : Math.random() < 0.75 ? Items.PACKED_ICE.getDefaultInstance() : Items.BLUE_ICE.getDefaultInstance());
            case 2 -> giveGeneratedLoot(
                    Math.random() < 0.75 ? Items.MAGMA_BLOCK.getDefaultInstance() : Items.MAGMA_CREAM.getDefaultInstance());
            default -> false;
        };
    }

    /**
     * 游标栅格扫描挖掘（mode 0 常规 / mode 3 Mineral 矿石共用）。
     * 从 machineY-1 层起栅格顺序扫描当前层（面板 XZ 盒内），跳过空气/不可挖/空掉落，
     * 挖第一个可挖方块；当前层扫完推进下一层。
     * <p>
     * 按层为单位：每调用只处理当前一层的栅格，层扫完推进/跳层后返回。
     * 层内扫描用 chunk 缓存避免逐格走 server chunk 层查找。
     * 标准模式：当前层扫完 → scanY-1 逐层下降（全空气层整层跳过）；
     * 极速模式：层扫完后 findNextNonAirLayer 跳过空气层。
     */
    private boolean scanMine() {
        if (level == null || itemHandler == null) {
            return false;
        }
        ItemStack tool = firstAvailableTool();
        if (tool.isEmpty()) {
            return false;
        }
        // 首次运行从机器 Y-1 初始化游标，不读取存档 scanY。
        if (!scanCursorInitialized) {
            scanCursorInitialized = true;
            initScanCursor();
        }
        if (scanY < minScanY()) {
            finishPass(); // 轮次收尾：挖尽 / 重扫一轮 / 等合规工具
            return false;
        }
        // 扫描区锚定：范围面板的 XZ 盒（以机器自身为 0 点、向四周对称展开）
        int minX = adjustMinX();
        int minZ = adjustMinZ();
        int maxX = adjustMaxX();
        int maxZ = adjustMaxZ();
        // 游标越界防御：面板改小 / 旧存档游标超界时归位西北角，防死循环与漏扫
        if (scanX < minX || scanX > maxX || scanZ < minZ || scanZ > maxZ) {
            scanX = minX;
            scanZ = minZ;
        }
        // 层内 chunk 缓存：本层扫描只涉及 1 个 chunk，单槽缓存即可
        Map<Long, ChunkAccess> chunkCache = new HashMap<>(2);
        boolean layerHasBlock = false; // 本层是否存在非空气方块（空气层跳层标志）
        // 单层栅格扫描（扫描区坐标序）：每调用只处理当前层，层扫完推进/跳层后返回
        while (scanZ <= maxZ) {
            int x = scanX;
            int z = scanZ;
            BlockState state = blockStateCached(chunkCache, x, scanY, z);
            if (state == null) {
                // chunk 未加载：推进游标跳过（不触发同步加载，防卡顿）
                advanceCursorInLayer();
                continue;
            }
            if (state.isAir()) {
                advanceCursorInLayer();
                continue;
            }
            layerHasBlock = true;
            BlockPos pos = new BlockPos(x, scanY, z);
            if (!canBreak(state, pos)) {
                passBlockedByTool = true; // 空手到底时据此区分「真挖完」与「工具不行」
                advanceCursorInLayer();
                continue;
            }
            List<ItemStack> drops = dropsFor(state, pos, effectiveToolForDrops(tool));
            if (drops.isEmpty()) {
                passBlockedByTool = true;
                advanceCursorInLayer();
                continue;
            }
            if (!canFitAll(drops)) {
                // 容量不足：游标不推进，输出清空后重试同一格（输出防丢，不漏挖）
                return false;
            }
            fitAll(drops);
            if (isAtmOre(state)) {
                // ATM 矿石的 canEntityDestroy 会拒绝无玩家的机器挖掘；掉落已手动入库，
                // 直接移除方块即可，不必走 destroyBlock 的破坏回调。
                level.removeBlock(pos, false);
            } else {
                destroyAndCollectDrops(pos);
            }
            scanPassMined = true; // 本轮有实际产出，允许到底后再以自身为起点重扫一轮
            ItemNBTHelper.damage(tool, level, 1);
            advanceCursorInLayer();
            return true;
        }
        // 当前层扫完 → 推进下一层（每 tick 至多处理一层；全空气层整层跳过加速收敛）
        // 游标为扫描区内绝对坐标，层推进时重置回扫描区西北角（区块中心外扩后的边界）
        scanX = minX;
        scanZ = minZ;
        if (scanFast || !layerHasBlock) {
            // 极速 或 本层全空气 → 跳过空气层直达下一非空气层（按层限步）
            int next = findNextNonAirLayer(scanY - 1);
            if (next >= minScanY()) {
                scanY = next;
            } else {
                // 限步内未找到：推进 MAX_LAYER_SKIP 层继续，钳在可挖下界 - 1
                // （低于下界即 1 层哨兵值，scanExhausted 据此停机）
                scanY = Math.max(scanY - 1 - MAX_LAYER_SKIP, minScanY() - 1);
            }
        } else {
            scanY--;
        }
        return false;
    }

    /**
     * 取方块掉落：常规上下文先算一次；空结果时用服务端虚构玩家再算一次。
     * <p>
     * 星辉魔法的水晶石矿石掉落表带 {@code astralsorcery:player_nearby} 条件，
     * 且 {@code failIfNoPlayer=true}——机器挖掘没有 {@code THIS_ENTITY} 玩家时整条池落空，
     * 于是采矿场误判为「工具挡下」而跳过矿石。虚构玩家只进掉落上下文，不参与破坏，
     * 也不写世界，因此不影响其它方块与原版时运规则。
     */
    private List<ItemStack> dropsFor(BlockState state, BlockPos pos, ItemStack tool) {
        List<ItemStack> drops = state.getDrops(WorkingHelper.getLootBuilder(level, pos, tool));
        if (!drops.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
            return drops;
        }
        FakePlayer standIn = FakePlayerFactory.getMinecraft(serverLevel);
        // 条件量的是「玩家到方块」的距离，不是方块到原点；虚构玩家必须站到目标格上。
        standIn.setPos(Vec3.atCenterOf(pos));
        var builder = WorkingHelper.getLootBuilder(level, pos, tool)
                .withParameter(LootContextParams.THIS_ENTITY, standIn);
        return state.getDrops(builder);
    }

    /**
     * 破坏方块并兜底回收「额外」掉落物实体。
     * <p>
     * 常规掉落已由 {@link #fitAll(List)} 直接写入输出槽，不生成掉落物。第三方（如万象担架的
     * 造化杖掉落吸收）有时会改写方块移除回调、把产物另起一个 item entity；这类实体留在
     * 世界里的表现就是「物品变成掉落物而不是进入采矿场存储区」。这里只回收本次破坏在
     * 该格新生成的实体，既不与周边已有掉落物抢，也不改变正常无掉落方块的行为。
     */
    private void destroyAndCollectDrops(BlockPos pos) {
        AABB area = new AABB(pos).inflate(0.5D);
        List<ItemEntity> before = level.getEntitiesOfClass(ItemEntity.class, area);
        level.destroyBlock(pos, false);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (before.contains(entity)) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (stack.isEmpty() || !canFit(stack)) {
                continue;
            }
            fitAll(List.of(stack.copy()));
            entity.discard();
        }
    }

    /**
     * 层内栅格游标推进：scanX 沿扫描区 X 边界递增，越界后 scanZ 进一行。
     * 边界 = 范围面板的 XZ 盒（与 {@link #scanMine()} 同源）。
     */
    private void advanceCursorInLayer() {
        int scanMinX = adjustMinX();
        scanX++;
        if (scanX > adjustMaxX()) {
            scanX = scanMinX;
            scanZ++;
        }
    }

    /**
     * 极速跳层探测：从 fromY 起向下找最近的非空气层，每层只采 5 个点——工作盒中心 + 盒子四角的列。
     * 5 点全空即整层跳过，任一非空即停在该层交给正常扫描逐格处理；代价是只落在采样列之外的
     * 中空层会被跳过，这是急速模式「以速度为先」的取舍。
     * <p>
     * 单次最多探 {@link #MAX_LAYER_SKIP} 层，绝不不限步直扫到底——否则机器悬空时第一 tick 就会
     * 把游标甩到几十层以下，看着像「起点不在自身下方」。
     *
     * @param fromY 起始层（向下递减）
     * @return 非空气层的 Y；限步内或到可挖下界为止无可挖层时返回 {@code minScanY() - 1}
     */
    private int findNextNonAirLayer(int fromY) {
        if (level == null) {
            return minScanY() - 1;
        }
        int minX = adjustMinX();
        int maxX = adjustMaxX();
        int minZ = adjustMinZ();
        int maxZ = adjustMaxZ();
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int[] probeX = { centerX, minX, maxX, minX, maxX };
        int[] probeZ = { centerZ, minZ, minZ, maxZ, maxZ };
        Map<Long, ChunkAccess> cache = new HashMap<>(2);
        int limit = Math.max(fromY - MAX_LAYER_SKIP + 1, minScanY());
        for (int y = fromY; y >= limit; y--) {
            for (int i = 0; i < probeX.length; i++) {
                BlockState s = blockStateCached(cache, probeX[i], y, probeZ[i]);
                if (s != null && !s.isAir()) {
                    return y;
                }
            }
        }
        return minScanY() - 1;
    }

    /**
     * 按 chunk 坐标缓存的方块状态读取：避免逐格 level.getBlockState 走 server chunk
     * 层查找。chunk 未加载返回 null（不触发同步加载）。缓存为方法内局部，无跨 tick 过期问题。
     */
    private BlockState blockStateCached(Map<Long, ChunkAccess> cache, int x, int y, int z) {
        // 防御：仅服务端扫描路径应调用（客户端无 chunk 源）
        if (level == null || level.isClientSide()) return null;
        long key = ((long) (x >> 4) << 32) | ((long) (z >> 4) & 0xFFFFFFFFL);
        ChunkAccess chunk = cache.get(key);
        if (chunk == null) {
            // 仅服务端扫描路径调用；ServerChunkCache.getChunk(…, requireChunk=false) 不触发同步加载
            chunk = ((ServerLevel) level).getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
            if (chunk == null) {
                return null;
            }
            cache.put(key, chunk);
        }
        return chunk.getBlockState(new BlockPos(x, y, z));
    }

    /**
     * 一轮扫到底的收尾：按 {@link QuarryScanDecision} 决定停机、重扫，还是停在记录点等工具。
     */
    private void finishPass() {
        switch (QuarryScanDecision.decide(true, scanPassMined, passBlockedByTool)) {
            case AWAIT_TOOL -> {
                // 回到本轮起点等工具，不算挖尽，也不消耗能量
                awaitTool = true;
                scanPassMined = false;
                scanY = passStartY;
                scanX = adjustMinX();
                scanZ = adjustMinZ();
            }
            case RESCAN_FROM_START -> {
                scanPassMined = false;
                resetScanCursor();
            }
            default -> {
                // CONTINUE 不会在到底时出现；EXHAUSTED_STOP 保持游标在底部，由 scanExhausted 停机
            }
        }
    }

    /**
     * 复位游标（模式变化/挖尽后恢复）：从工作盒西北角、machineY-1 层起点重新扫描。
     */
    private void resetScanCursor() {
        scanY = worldPosition.getY() - 1;
        passStartY = scanY;
        passBlockedByTool = false;
        scanX = adjustMinX();
        scanZ = adjustMinZ();
    }

    /**
     * 首次运行初始化游标：从机器 Y-1 与扫描区西北角起扫，不读取存档 {@code scanY}，
     * 避免新机器的默认值 0 在低处落入合法区间后从 y=0 开始扫描。
     * <p>
     * 首轮不做不限步跳层；后续极速或全空气层探测最多跳过 {@link #MAX_LAYER_SKIP} 层，
     * 避免悬空机器一开始就跳到几十层以下。
     */
    private void initScanCursor() {
        scanY = worldPosition.getY() - 1;
        passStartY = scanY;
        passBlockedByTool = false;
        // 游标水平位置一律回到扫描区西北角（防御旧存档游标越界：升级增减后范围可能变化）
        scanX = adjustMinX();
        scanZ = adjustMinZ();
    }

    /**
     * 挖尽判定：scanY 到达世界底（或极速模式无可挖层）。
     * 停机由 {@link #conditionStart()} 门禁 + {@link #applyEffect()} 早退执行，
     * 恢复经 {@link #rpcToggleScanFast} / tick() 模式变化。
     */
    public boolean scanExhausted() {
        return scanY < minScanY();
    }

    /**
     * C→S: 切换标准/极速扫描模式（GUI 切换按钮）。
     * 挖尽停机后切换 → 复位游标重新扫描（恢复路径之一）。
     */
    @RPCMethod
    public void rpcToggleScanFast(RPCSender sender) {
        // C→S 方向服务端执行时 sender 为 ofClient(player)：isRemote()=true
        if (sender.isRemote()) {
            scanFast = !scanFast;
            awaitTool = false;
            if (scanCursorInitialized && scanExhausted()) {
                resetScanCursor();
            }
            setChanged();
        }
    }

    /**
     * ATM 三种特色矿石及其变种。它们用 {@code destroyTime = -1} 阻止常规机器挖掘，
     * 还在 {@code canEntityDestroy} 里拒绝 FakePlayer、要求 16 格内有真玩家；
     * 但挖掘等级是真实门槛，不能绕过——等级判定完全交给工具的 {@code TOOL} 组件与
     * {@code c:needs_*_tool} 标签链，不限定 ATM 自家镐：任何模组只要把工具的 correct/incorrect
     * 规则声明到覆盖这些标签，就能通过（下界合金 → allthemodium → vibranium → unobtainium）。
     * 这里只放行硬度门禁，等级仍走 {@code isCorrectToolForDrops}。
     * 用字符串 id 判定方块，避免让可选模组类型进入总是加载的类。
     */
    private static final Set<String> ATM_ORES = Set.of(
            "allthemodium_ore", "allthemodium_slate_ore",
            "unobtainium_ore", "vibranium_ore", "other_vibranium_ore");

    private boolean isAtmOre(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "allthemodium".equals(id.getNamespace()) && ATM_ORES.contains(id.getPath());
    }

    /**
     * 可挖判定。第一道是 {@code destroyTime < 0}（原版不可破坏方块的统一标记，模组的也走这条），
     * 不靠「掉落表恰好为空」来兜底——任何带掉落表的不可破坏方块都会被挖穿。ATM 特色矿石是显式例外。
     */
    private boolean canBreak(BlockState state, BlockPos pos) {
        ItemStack tool = firstAvailableTool();
        if (tool.isEmpty()) {
            return false;
        }
        boolean atmOre = isAtmOre(state);
        if (!atmOre && level != null && state.getDestroySpeed(level, pos) < 0) {
            return false; // 基岩一类不可破坏方块：不靠掉落表为空来兜底
        }
        if (mode == 0) {
            // 标准模式：仅跳过带方块实体（组件信息）的方块；
            // 挖掘等级受限于工具槽工具——需要正确工具的方块按工具 tier 判定，
            // 无等级要求的方块（泥土/沙等）任意工具可挖。极速模式仅寻路不同，共用本判定。
            if (state.hasBlockEntity()) {
                return false;
            }
            return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        }
        if (mode == 3) {
            // Mineral 矿石扫描：c:ores 标签或 ATM 白名单 + 工具正确；ATM 三矿同样要过工具等级
            return (atmOre || TagHelper.containsBlock(state.getBlock(), TagHelper.keyBlock("c:ores"))) && tool.isCorrectToolForDrops(state);
        }
        return false;
    }

    private boolean giveGeneratedLoot(ItemStack stack) {
        if (canFit(stack)) {
            insertFirstFit(stack.copy());
            return true;
        }
        return false;
    }

    private boolean canFit(ItemStack stack) {
        for (int i = OUTPUT_START; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                return true;
            }
            // 使用动态槽上限而非硬编码 maxStackSize
            int slotLimit = itemHandler.getSlotLimit(i);
            if (ItemStack.isSameItem(existing, stack) && existing.getCount() + stack.getCount() <= slotLimit) {
                return true;
            }
        }
        return false;
    }

    /**
     * 快照模拟预检：与 fitAll 提交逻辑完全一致，多件掉落不竞争同一空槽。
     */
    private boolean canFitAll(List<ItemStack> stacks) {
        int outputStart = OUTPUT_START;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] simulated = copyOutputSlots(outputStart, slotCount);
        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(simulated, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ItemStack[] copyOutputSlots(int start, int count) {
        ItemStack[] copy = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            ItemStack s = itemHandler.getStackInSlot(start + i);
            copy[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
        return copy;
    }

    /** 模拟向槽位数组插入单组物品，就地更新；返回剩余（空=全部插入）。与 fitAll 提交阶段一致。 */
    private ItemStack simulateInsert(ItemStack[] slots, ItemStack stack, int outputStart) {
        for (int j = 0; j < slots.length && !stack.isEmpty(); j++) {
            if (slots[j].isEmpty()) {
                int slotLimit = itemHandler.getSlotLimit(outputStart + j);
                if (stack.getCount() <= slotLimit) {
                    slots[j] = stack;
                    stack = ItemStack.EMPTY;
                } else {
                    ItemStack fill = stack.copy();
                    fill.setCount(slotLimit);
                    slots[j] = fill;
                    stack.shrink(slotLimit);
                }
            } else if (ItemStack.isSameItem(slots[j], stack)) {
                int slotLimit = itemHandler.getSlotLimit(outputStart + j);
                int room = slotLimit - slots[j].getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    slots[j].grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        return stack;
    }

    /**
     * 快照 + 模拟 + 原子提交：模拟失败 fail-fast，绝不部分写入。
     */
    private void fitAll(List<ItemStack> stacks) {
        int outputStart = OUTPUT_START;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] snapshot = copyOutputSlots(outputStart, slotCount);

        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(snapshot, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                throw new IllegalStateException(
                        "Quarry cannot fit all drops: " + stack + " has " + remaining.getCount() + " remaining. " + "canFitAll pre-check should have prevented this.");
            }
        }

        for (int i = 0; i < slotCount; i++) {
            itemHandler.setStackInSlot(outputStart + i, snapshot[i]);
        }
    }

    /**
     * 单件产出（mode 1/2 生成型掉落）首适配插入。fail-fast 防静默丢物。
     */
    private void insertFirstFit(ItemStack stack) {
        for (int i = OUTPUT_START; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                int slotLimit = itemHandler.getSlotLimit(i);
                if (stack.getCount() <= slotLimit) {
                    itemHandler.setStackInSlot(i, stack);
                    return;
                } else {
                    ItemStack fill = stack.copy();
                    fill.setCount(slotLimit);
                    stack.shrink(slotLimit);
                    itemHandler.setStackInSlot(i, fill);
                    continue;
                }
            }
            if (ItemStack.isSameItem(existing, stack)) {
                int slotLimit = itemHandler.getSlotLimit(i);
                int room = slotLimit - existing.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    stack.shrink(moved);
                    if (stack.isEmpty()) {
                        return;
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            throw new IllegalStateException(
                    "Quarry insertFirstFit cannot fit " + stack + ": no available slot. " + "canFitAll pre-check should have prevented this.");
        }
    }

    @Override
    public double effectInterval() {
        return switch (mode) {
            case 0 -> 3;
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 0.4;
            default -> Integer.MAX_VALUE;
        };
    }
}
