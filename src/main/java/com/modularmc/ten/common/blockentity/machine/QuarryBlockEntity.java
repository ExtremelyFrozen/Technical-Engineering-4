package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupIce;
import com.modularmc.ten.common.item.upgrades.LevelupMagma;
import com.modularmc.ten.common.item.upgrades.LevelupMineral;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.TagHelper;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuarryBlockEntity extends RadiusMachineBlockEntity {

    private int mode;

    // ── 游标扫描状态（核心算法 + 跨重启恢复路径）──

    /** 当前层栅格游标 X（世界块坐标，区块边界内）。 */
    @Persisted
    public int scanX;

    /** 当前层栅格游标 Z（世界块坐标，区块边界内）。 */
    @Persisted
    public int scanZ;

    /** 当前扫描层 Y（machineY-1 起向下递减，到世界底 -64 视为挖尽，用户裁决启用深层）。 */
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
     * 游标是否已初始化。防止挖尽（scanY < WORLD_MIN）后重复初始化重扫空气层空转。
     * 持久化：重启后挖尽停机状态跨重启保持。
     */
    @Persisted
    public boolean scanCursorInitialized = false;

    /**
     * 极速跳层限步：findNextNonAirLayer 每调用最多跳过 MAX_LAYER_SKIP 层
     * （防单 tick 长循环；未找到则推进限步层数由 scanExhausted 判定挖尽）。
     */
    private static final int MAX_LAYER_SKIP = 32;

    public boolean isScanFast() {
        return scanFast;
    }

    public QuarryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 3;
        radius = 3;
    }

    @Override
    public int machineType() {
        return MachineType.QUARRY;
    }

    /** 工具槽数量（用户设定 1→3：槽 0..2；GUI 在原工具槽 (43,34) 相邻上下 (43,16)/(43,52)）。 */
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
            // 工具槽：任何带 TOOL 组件的物品（原 TieredItem 判定过窄）
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
     * 生效范围：以区块中心对称的正方形——基础为所属区块 16×16（基础半宽 8），
     * 每件范围升级（LevelupRg）以区块中心向外扩 4 格（radius 超出 initialRadius 的
     * 部分即外扩格数，由 {@link com.modularmc.ten.common.item.upgrades.LevelupRg} 累加）。
     * 机器仅定位所属区块与 Y 高度，任意模式下都位于机器自身 Y 之下
     * （从机器下方一格向下到世界底，1.21.1 为 -64，用户裁决启用深层）。
     */
    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        int top = worldPosition.getY() - 1; // 机器下方一格
        int e = Math.max(0, radius - initialRadius); // 外扩格数（无 Rg 时 0 → 原区块）
        int minX = (worldPosition.getX() & ~15) - e;      // 区块西边界外扩（含）
        int minZ = (worldPosition.getZ() & ~15) - e;      // 区块北边界外扩（含）
        int span = 16 + 2 * e;
        return List.of(new net.minecraft.world.phys.AABB(
                minX, TENConstants.WORLD_MIN, minZ,
                minX + span, Math.max(top, TENConstants.WORLD_MIN) + 1, minZ + span));
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 工具槽（用户设定 1→3）：原槽位 (43,34) 相邻上下各加一槽；输出槽 3..14 索引顺移
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
            root.addChild(scanModeToggleButton());
            // 范围显示按钮：Quarry 已有扫描按钮占用 (2,66)，范围按钮放 (2,48)
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this, 2, 48));
            // 应用工具附魔切换按钮（右侧；原 (48,48) 与新增工具槽 2 (43,52) 重叠 13×12px，
            // 移至顶部空带 (48,0)：y0..16 与工具槽 1/输出槽 3（y16 起）仅边界相切）
            root.addChild(TENMachineBlockUIFactory.useEnchantmentsToggleButton(this, 48, 0));
        });
    }

    /**
     * 标准/极速扫描模式切换按钮（逐坐标）。
     * 位置 (2,66) 44x16：能量条正下方空余带；与进度条 (48,74)、玩家物品栏 (7,83) 无重叠。
     * 点击经 rpcToServer 显式发包，服务端执行 {@link #rpcToggleScanFast}。
     */
    private Button scanModeToggleButton() {
        Button button = new Button();
        button.textStyle(style -> style.textShadow(false));
        button.setOnClick(event -> rpcToServer("rpcToggleScanFast"));
        Runnable refresh = () -> button.setText(isScanFast() ? ComponentHelper.translated("kenergyengineering.info.quarry.scan_fast") : ComponentHelper.translated("kenergyengineering.info.quarry.scan_normal"));
        refresh.run();
        button.addEventListener(UIEvents.TICK, event -> refresh.run());
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(
                List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.quarry.scan_mode"),
                        isScanFast() ? ComponentHelper.translated("kenergyengineering.info.quarry.scan_fast_tip") : ComponentHelper.translated("kenergyengineering.info.quarry.scan_normal_tip"),
                        ComponentHelper.translated("kenergyengineering.info.quarry.scan_click")),
                null, null, null));
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(2);
            layout.top(66);
            layout.width(44);
            layout.height(16);
        });
        return button;
    }

    @Override
    public void tick() {
        super.tick();
        if (getAliveTime() % 10 == 0) {
            updateMode();
        }
        // 挖尽停机后的恢复路径：升级半径/模式变化 → 复位游标重新扫描。
        // 停机时探测 lastScanRadius/lastMode 变化；运行态持续跟踪 lastMode，
        // 避免停机瞬间把既有状态误判为「变化」而误复位。
        if (scanCursorInitialized && scanExhausted()) {
            // 恢复路径：模式变化 → 复位游标重扫（半径已退出范围计算，不再触发复位）
            if (lastMode != mode) {
                lastMode = mode;
                resetScanCursor();
            }
        } else {
            lastMode = mode;
        }
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
        // 挖尽停机：游标已初始化且 scanY 到世界底 → 立即停机并清锁，不再空转耗能。
        if (scanCursorInitialized && scanExhausted()) {
            setActive(false);
            clearLockedBatch();
            return;
        }
        // 批量升级时按 lockedB 动态扩展输出槽上限
        installDynamicOutputLimit();
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
     * 挖尽停机门禁：游标已初始化且 scanY 到世界底 → 不再锁定批次/消耗能量。
     * 游标未初始化（首次放置/旧存档）时放行，首个 applyEffect 周期会初始化游标。
     */
    @Override
    public boolean conditionStart() {
        if (!scanCursorInitialized) {
            return true;
        }
        return !scanExhausted();
    }

    @Override
    public boolean cooking() {
        // 纯容量谓词：输出槽（1..12）无法容纳本周期 B 次操作产出时停滞（保留 progress）。
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
     * 批量升级时按 lockedB 动态扩展输出槽上限（1..12），保留既有超堆叠。
     */
    private void installDynamicOutputLimit() {
        if (itemHandler == null) return;
        int B = getLockedBatchSize();
        int cap = (int) Math.min(64L * Math.max(1, B), Integer.MAX_VALUE);
        itemHandler.setDynamicSlotLimit((slot, candidate) -> {
            if (slot < OUTPUT_START || slot >= inventorySize()) return 1; // 工具槽堆叠上限 1（用户调整）
            ItemStack existing = itemHandler.getStackInSlot(slot);
            return existing.isEmpty() ? cap : Math.max(cap, existing.getCount());
        });
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
     * 从 machineY-1 层起栅格顺序扫描当前层（±radius 内），跳过空气/不可挖/空掉落，
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
        // 首次运行 → 初始化游标（尊重存档 scanY，若在合法范围内）
        if (!scanCursorInitialized) {
            scanCursorInitialized = true;
            initScanCursor();
        }
        if (scanY < TENConstants.WORLD_MIN) {
            return false; // 挖尽
        }
        // 扫描区锚定：以区块中心对称的正方形（基础区块 16×16，Rg 外扩 e 格）
        int expand = Math.max(0, radius - initialRadius);
        int minX = (worldPosition.getX() & ~15) - expand;
        int minZ = (worldPosition.getZ() & ~15) - expand;
        int span = 16 + 2 * expand;
        // 游标越界防御：升级增减后范围收缩/旧存档游标超界时归位西北角，防死循环与漏扫
        if (scanX < minX || scanX >= minX + span || scanZ < minZ || scanZ >= minZ + span) {
            scanX = minX;
            scanZ = minZ;
        }
        // 层内 chunk 缓存：本层扫描只涉及 1 个 chunk，单槽缓存即可
        Map<Long, ChunkAccess> chunkCache = new HashMap<>(2);
        boolean layerHasBlock = false; // 本层是否存在非空气方块（空气层跳层标志）
        // 单层栅格扫描（扫描区坐标序）：每调用只处理当前层，层扫完推进/跳层后返回
        while (scanZ < minZ + span) {
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
            if (!canBreak(state)) {
                advanceCursorInLayer();
                continue;
            }
            List<ItemStack> drops = state.getDrops(WorkingHelper.getLootBuilder(level, pos, effectiveToolForDrops(tool)));
            if (drops.isEmpty()) {
                advanceCursorInLayer();
                continue;
            }
            if (!canFitAll(drops)) {
                // 容量不足：游标不推进，输出清空后重试同一格（输出防丢，不漏挖）
                return false;
            }
            fitAll(drops);
            level.destroyBlock(pos, false);
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
            int next = findNextNonAirLayer(scanY - 1, MAX_LAYER_SKIP);
            if (next >= TENConstants.WORLD_MIN) {
                scanY = next;
            } else {
                // 限步内未找到：推进 MAX_LAYER_SKIP 层继续；
                // 钳位下界 = WORLD_MIN - 1，使 scanExhausted（scanY < WORLD_MIN）可触发停机
                scanY = Math.max(scanY - 1 - MAX_LAYER_SKIP, TENConstants.WORLD_MIN - 1);
            }
        } else {
            scanY--;
        }
        return false;
    }

    /**
     * 层内栅格游标推进：scanX 沿扫描区 X 边界递增，越界后 scanZ 进一行。
     * 边界 = 区块中心外扩后的扫描区（与 {@link #scanMine()} 的 minX/span 派生同源）。
     */
    private void advanceCursorInLayer() {
        int expand = Math.max(0, radius - initialRadius);
        int scanMinX = (worldPosition.getX() & ~15) - expand;
        int span = 16 + 2 * expand;
        scanX++;
        if (scanX >= scanMinX + span) {
            scanX = scanMinX;
            scanZ++;
        }
    }

    /**
     * 极速模式辅助：从 fromY 起向下找最近的非空气层（层内 ±radius 存在非空气方块）。
     *
     * @param fromY     起始层（向下递减）
     * @param maxLayers 每调用最多检查的层数（限步防单 tick 长循环）；≤0 表示不限步
     * @return 非空气层的 Y；限步内或到世界底为止无可挖层时返回 {@code WORLD_MIN - 1}
     *         （哨兵取合法域 [WORLD_MIN, …] 之外，避免与深层合法返回值碰撞）
     */
    private int findNextNonAirLayer(int fromY, int maxLayers) {
        if (level == null) {
            return TENConstants.WORLD_MIN - 1;
        }
        int expand = Math.max(0, radius - initialRadius);
        int minX = (worldPosition.getX() & ~15) - expand;
        int minZ = (worldPosition.getZ() & ~15) - expand;
        int span = 16 + 2 * expand;
        Map<Long, ChunkAccess> cache = new HashMap<>(2);
        int limit = (maxLayers <= 0) ? TENConstants.WORLD_MIN : Math.max(fromY - maxLayers + 1, TENConstants.WORLD_MIN);
        for (int y = fromY; y >= limit; y--) {
            for (int dx = 0; dx < span; dx++) {
                for (int dz = 0; dz < span; dz++) {
                    BlockState s = blockStateCached(cache, minX + dx, y, minZ + dz);
                    if (s != null && !s.isAir()) {
                        return y;
                    }
                }
            }
        }
        return TENConstants.WORLD_MIN - 1;
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
                return null; // chunk 未加载
            }
            cache.put(key, chunk);
        }
        return chunk.getBlockState(new BlockPos(x, y, z));
    }

    /**
     * 复位游标（模式变化/挖尽后恢复）：从区块西北角、machineY-1 层起点重新扫描。
     */
    private void resetScanCursor() {
        int expand = Math.max(0, radius - initialRadius);
        scanY = worldPosition.getY() - 1;
        scanX = (worldPosition.getX() & ~15) - expand;
        scanZ = (worldPosition.getZ() & ~15) - expand;
    }

    /**
     * 首次运行初始化游标：尊重存档中的 scanY（若在 [WORLD_MIN, machineY-1] 合法范围），
     * 否则从 machineY-1 层开始；极速模式直接锁定最近的非空气层（全空气世界 → scanY=哨兵，
     * 由 scanExhausted 判定停机）。
     */
    private void initScanCursor() {
        int expand = Math.max(0, radius - initialRadius);
        int startX = (worldPosition.getX() & ~15) - expand;
        int startZ = (worldPosition.getZ() & ~15) - expand;
        int startY = worldPosition.getY() - 1;
        if (scanY < TENConstants.WORLD_MIN || scanY > startY) {
            scanY = startY;
        }
        if (scanFast) {
            int y = findNextNonAirLayer(scanY, 0); // 首次初始化：不限步扫到底
            // 哨兵（WORLD_MIN - 1）= 全空气世界 → scanY 置为哨兵值本身，scanExhausted 判定停机
            scanY = y;
        }
        // 游标水平位置一律回到扫描区西北角（防御旧存档游标越界：升级增减后范围可能变化）
        scanX = startX;
        scanZ = startZ;
    }

    /**
     * 挖尽判定：scanY 到达世界底（或极速模式无可挖层）。
     * 停机由 {@link #conditionStart()} 门禁 + {@link #applyEffect()} 早退执行，
     * 恢复经 {@link #rpcToggleScanFast} / tick() 模式变化。
     */
    public boolean scanExhausted() {
        return scanY < TENConstants.WORLD_MIN;
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
            if (scanCursorInitialized && scanExhausted()) {
                resetScanCursor();
            }
            setChanged();
        }
    }

    private boolean canBreak(BlockState state) {
        ItemStack tool = firstAvailableTool();
        if (tool.isEmpty()) {
            return false;
        }
        if (mode == 0) {
            // 标准模式（用户调整）：仅跳过带方块实体（组件信息）的方块；
            // 挖掘等级受限于工具槽工具——需要正确工具的方块按工具 tier 判定，
            // 无等级要求的方块（泥土/沙等）任意工具可挖。极速模式仅寻路不同，共用本判定。
            return !state.hasBlockEntity() && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
        }
        if (mode == 3) {
            // Mineral 矿石扫描：限定 c:ores 标签 + 工具正确
            return TagHelper.containsBlock(state.getBlock(), TagHelper.keyBlock("c:ores")) && tool.isCorrectToolForDrops(state);
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
