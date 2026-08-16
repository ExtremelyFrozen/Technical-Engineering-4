package com.modularmc.ten.common.blockentity.machine;

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

    // ── 游标扫描状态（单元 A：仅核心算法；GUI 切换按钮/契约更新为后续单元）──

    /** 当前层栅格游标 X 偏移（-radius..+radius）。 */
    @Persisted
    public int scanX;

    /** 当前层栅格游标 Z 偏移（-radius..+radius）。 */
    @Persisted
    public int scanZ;

    /** 当前扫描层 Y（machineY-1 起向下递减，到 0 视为挖尽）。 */
    @Persisted
    public int scanY;

    /**
     * 极速模式标志：false=标准逐层扫描，true=findNextNonAirLayer 跳过空气层。
     * 默认 false（标准）。由后续 GUI 按钮设置。
     */
    @Persisted
    @DescSynced
    public boolean scanFast;

    /**
     * 上次扫描时的半径。半径变化（升级）→ 游标复位。
     * 瞬态字段：重启后强制重置一次（已挖层为空气，快速跳过，无害）。
     */
    private int lastScanRadius = -1;

    /**
     * 上次评估到的模式。挖尽停机后模式变化（升级增减）→ 游标复位重新扫描（恢复路径）。
     * 瞬态字段：重启后首次 updateMode 即与 mode 对齐，不会误触发复位。
     */
    private int lastMode = -1;

    /**
     * 游标是否已初始化。防止挖尽（scanY=0）后重复初始化重扫空气层空转。
     * 持久化（M1 修复）：重启后保持初始化状态——挖尽（scanY=0）的存档恢复后继续停机，
     * 不再整坑重扫空转；旧存档（无此字段）首次加载回落 false，
     * 一次性空气层快速跳过后再停机（迁移成本，无害）。
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

    @Override
    public int inventorySize() {
        return 13;
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot == 0 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        if (slot == 0) {
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

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 79, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 133, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 5, 79, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 6, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 7, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 8, 133, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 9, 79, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 11, 115, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 12, 133, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 74, true));
            root.addChild(scanModeToggleButton());
        });
    }

    /**
     * 标准/极速扫描模式切换按钮（LDLib2 Button，文本显示当前模式）。
     * <p>
     * 坐标验证（GUI 176x166，MACHINE_GUI 空面板，全区域为底图）：
     * <ul>
     * <li>位置 (2,66) 44x16：位于能量条（(8,18) 14x46 → y18..64）正下方空余带；</li>
     * <li>右侧宽进度条 (48,74) 80x5 起点 x48 &gt; 按钮右缘 46，无横向重叠；</li>
     * <li>玩家物品栏 (7,83) 起 y83 &gt; 按钮底 82，无纵向重叠；</li>
     * <li>13 槽位（输入 (43,34) 18x18、输出 (79..151, 16..70)）与机器名牌 (6,4) 均不冲突。</li>
     * </ul>
     * 点击经 rpcToServer 显式发包（LDLib2 不拦截 @RPCMethod 直接调用），
     * 服务端执行 {@link #rpcToggleScanFast}。
     */
    private Button scanModeToggleButton() {
        Button button = new Button();
        button.textStyle(style -> style.textShadow(false));
        button.setOnClick(event -> rpcToServer("rpcToggleScanFast"));
        Runnable refresh = () -> button.setText(isScanFast() ? ComponentHelper.translated("kenergyengineering.info.quarry.scan_fast") : ComponentHelper.translated("kenergyengineering.info.quarry.scan_normal"));
        refresh.run();
        button.addEventListener(UIEvents.TICK, event -> refresh.run());
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(
                ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.quarry.scan_mode"),
                isScanFast() ? ComponentHelper.translated("kenergyengineering.info.quarry.scan_fast_tip") : ComponentHelper.translated("kenergyengineering.info.quarry.scan_normal_tip"),
                ComponentHelper.translated("kenergyengineering.info.quarry.scan_click")));
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
        // 挖尽停机后的恢复路径（单元 B）：升级半径/模式变化 → 复位游标重新扫描。
        // 停机时探测 lastScanRadius/lastMode 变化；运行态持续跟踪 lastMode，
        // 避免停机瞬间把既有状态误判为「变化」而误复位。
        if (scanCursorInitialized && scanExhausted()) {
            if (lastScanRadius != radius || lastMode != mode) {
                lastScanRadius = radius;
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
        // 挖尽停机（单元 B）：游标已初始化且 scanY 到 0 → 立即停机并清锁，不再空转耗能。
        // 停机门禁见 conditionStart()；恢复路径：rpcToggleScanFast 切换 / 升级半径或模式变化（tick()）。
        if (scanCursorInitialized && scanExhausted()) {
            setActive(false);
            clearLockedBatch();
            return;
        }
        // P3-T1c: Install dynamic output slot limit based on locked batch size
        installDynamicOutputLimit();
        // conditionStart is already checked in outer process() — no need to repeat.
        // Only truly fatal conditions break the loop: tool broken (empty) or exhausted
        // mid-cycle (M2 修复). Per-operation misses continue — QuarryLoopMissVsBreak
        // 契约：不得 break 于 executeSingleOperation() false / 不得重复 conditionStart()。
        int B = getLockedBatchSize();
        for (int i = 0; i < B; i++) {
            if (itemHandler.getStackInSlot(0).isEmpty()) break; // Tool broken/empty → fatal → break
            // 挖尽（本 cycle 中途 scanY→0）：剩余 B−1 次操作已无产出，不再空转耗能。
            // 门禁与 conditionStart()/applyEffect 早退一致（scanCursorInitialized && scanExhausted），
            // 避免 mode 1/2 生成型（永不初始化游标）被误 break。
            if (scanCursorInitialized && scanExhausted()) break;
            // Per-operation miss (no drops, can't break, capacity full) → continue
            executeSingleOperation();
        }
    }

    /**
     * 挖尽停机门禁（单元 B）：游标已初始化且 scanY 到 0 → 不再锁定批次/消耗能量。
     * 游标未初始化（首次放置 / 旧存档无 scanCursorInitialized 字段）时放行，
     * 首个 applyEffect 周期会初始化游标。重启后 scanCursorInitialized 自存档恢复
     * （M1），挖尽停机状态跨重启保持。
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
        // Pure capacity predicate (P1 contract): block processing when output slots
        // (1..12) cannot accommodate this cycle's B output units. Each operation
        // produces at most 1 output unit (mode 1/2: single item; mode 0/3: one
        // block's drops). No progress read/write, no side effects — progress is
        // preserved while output is full (stall semantics, Deviation #2 fix).
        if (itemHandler == null) {
            return false;
        }
        int B = getLockedBatchSize();
        int units = 0;
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            units += existing.isEmpty() ? itemHandler.getSlotLimit(i) : Math.max(0, itemHandler.getSlotLimit(i) - existing.getCount());
        }
        return units < B;
    }

    /**
     * Install dynamic slot limit on output slots (1..12) based on lockedB.
     * Limit = min(lockedB + 63, 99) per slot, preserving existing overstack.
     */
    private void installDynamicOutputLimit() {
        if (itemHandler == null) return;
        int B = getLockedBatchSize();
        itemHandler.setDynamicSlotLimit((slot, candidate) -> {
            if (slot < 1 || slot >= inventorySize()) return 64; // Not output
            int limit = Math.min(B + 63, 99);
            // Never go below existing count
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (!existing.isEmpty()) {
                limit = Math.max(limit, existing.getCount());
            }
            return limit;
        });
    }

    /**
     * Execute one unit of the current mode's operation.
     *
     * @return true if operation was executed, false if cannot continue
     */
    private boolean executeSingleOperation() {
        return switch (mode) {
            // mode 0（常规挖掘）与 mode 3（Mineral 矿石扫描）共用游标栅格扫描：
            // canBreak() 按模式过滤 quarry_valids / c:ores 标记。
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
     * 挖第一个可挖标记方块（mode 0=quarry_valids，mode 3=c:ores，见 {@link #canBreak}）；
     * 当前层扫完推进下一层。
     * <p>
     * 标准模式（scanFast=false）：当前层扫完 → scanY-1 逐层下降；
     * 极速模式（scanFast=true）：层扫完后 findNextNonAirLayer 跳过空气层，锁定下一非空气层。
     * <p>
     * 返回 true=完成一次挖掘；false=本次未挖（容量不足/工具缺失/挖尽）。
     * 挖尽时（scanY 到 0 或无可挖层）{@link #scanExhausted()} 为 true，
     * 由 {@link #conditionStart()} 停机门禁 + {@link #applyEffect()} 早退停机，不再空转耗能。
     */
    /**
     * 游标栅格扫描挖掘（mode 0 常规 / mode 3 Mineral 矿石共用）。
     * 从 machineY-1 层起栅格顺序扫描当前层（±radius 内），跳过空气/不可挖/空掉落，
     * 挖第一个可挖标记方块（mode 0=quarry_valids，mode 3=c:ores，见 {@link #canBreak}）；
     * 当前层扫完推进下一层。
     * <p>
     * 按层为单位（性能优化）：每调用只处理<b>当前一层</b>的栅格，层扫完推进/跳层后返回，
     * 不再跨多层长循环（防单 tick 卡顿）。层内扫描用 chunk 缓存（借鉴 Mekanism
     * MinerRegionCache），避免逐格走 server chunk 层查找；挖不掉方块记入 skipped 集合
     * （借鉴 QuarryPlus）防重复尝试；全空气层整层跳过加速挖尽收敛。
     * <p>
     * 标准模式（scanFast=false）：当前层扫完 → scanY-1 逐层下降（但全空气层跳过）；
     * 极速模式（scanFast=true）：层扫完后 findNextNonAirLayer 跳过空气层，锁定下一非空气层。
     * <p>
     * 返回 true=完成一次挖掘；false=本次未挖（容量不足/工具缺失/挖尽）。
     * 挖尽时（scanY 到 0 或无可挖层）{@link #scanExhausted()} 为 true，
     * 由 {@link #conditionStart()} 停机门禁 + {@link #applyEffect()} 早退停机，不再空转耗能。
     */
    private boolean scanMine() {
        if (level == null || itemHandler == null) {
            return false;
        }
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) {
            return false;
        }
        // 半径变化（升级）→ 游标复位，从 machineY-1 层重扫新范围
        if (lastScanRadius != radius) {
            lastScanRadius = radius;
            resetScanCursor();
        }
        // 首次运行 → 初始化游标（尊重存档 scanY，若在合法范围内）
        if (!scanCursorInitialized) {
            scanCursorInitialized = true;
            initScanCursor();
        }
        if (scanY < 1) {
            return false; // 挖尽
        }
        int mx = worldPosition.getX();
        int mz = worldPosition.getZ();
        // 层内 chunk 缓存：扫描当前层时按 chunk 坐标缓存已加载 chunk，
        // 直接读 chunk.getBlockState，避免逐格 level.getBlockState 走 server chunk 层查找。
        Map<Long, ChunkAccess> chunkCache = new HashMap<>(9);
        boolean layerHasBlock = false; // 本层是否存在非空气方块（空气层跳层标志）
        // 单层栅格扫描（按层为单位）：每调用只处理当前层，层扫完推进/跳层后返回
        while (scanZ <= radius) {
            int x = mx + scanX;
            int z = mz + scanZ;
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
                // 非标记/工具不正确：跳过（无需记入集合，canBreak 本身成本低）
                advanceCursorInLayer();
                continue;
            }
            List<ItemStack> drops = state.getDrops(WorkingHelper.getLootBuilder(level, pos, tool));
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
        scanX = -radius;
        scanZ = -radius;
        if (scanFast || !layerHasBlock) {
            // 极速 或 本层全空气 → 跳过空气层直达下一非空气层（findNextNonAirLayer 按层限步）
            int next = findNextNonAirLayer(scanY - 1, MAX_LAYER_SKIP);
            if (next > 0) {
                scanY = next;
            } else {
                // 限步内未找到：推进 MAX_LAYER_SKIP 层继续（scanY 到 0 由 scanExhausted 判定挖尽）
                scanY = Math.max(scanY - 1 - MAX_LAYER_SKIP, 0);
            }
        } else {
            scanY--;
        }
        return false;
    }

    /**
     * 层内栅格游标推进：scanX 从 -radius 递增到 +radius，越界后 scanZ 进一行。
     * 层扫完时 scanZ 变为 radius+1，由调用方检测后重置。
     */
    private void advanceCursorInLayer() {
        scanX++;
        if (scanX > radius) {
            scanX = -radius;
            scanZ++;
        }
    }

    /**
     * 极速模式辅助：从 fromY 起向下找最近的非空气层（层内 ±radius 存在非空气方块）。
     *
     * @return 非空气层的 Y；到 1 层为止无可挖层时返回 -1
     */
    /**
     * 极速模式辅助：从 fromY 起向下找最近的非空气层（层内 ±radius 存在非空气方块）。
     *
     * @param fromY     起始层（向下递减）
     * @param maxLayers 每调用最多检查的层数（限步防单 tick 长循环）；
     *                  ≤0 表示不限步（initScanCursor 首次初始化用）
     * @return 非空气层的 Y；限步内或到 1 层为止无可挖层时返回 -1
     */
    private int findNextNonAirLayer(int fromY, int maxLayers) {
        if (level == null) {
            return -1;
        }
        int mx = worldPosition.getX();
        int mz = worldPosition.getZ();
        Map<Long, ChunkAccess> cache = new HashMap<>(9);
        int limit = (maxLayers <= 0) ? 1 : Math.max(fromY - maxLayers + 1, 1);
        for (int y = fromY; y >= limit; y--) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockState s = blockStateCached(cache, mx + dx, y, mz + dz);
                    if (s != null && !s.isAir()) {
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 按 chunk 坐标缓存的方块状态读取：避免逐格 level.getBlockState 走 server chunk 层
     * 查找（借鉴 Mekanism MinerRegionCache 思路）。chunk 未加载返回 null（不触发同步加载）。
     * 缓存为方法内局部，每次层扫描新建，chunk 卸载后下次调用重新获取，无跨 tick 过期问题。
     */
    private BlockState blockStateCached(Map<Long, ChunkAccess> cache, int x, int y, int z) {
        long key = ((long) (x >> 4) << 32) | ((long) (z >> 4) & 0xFFFFFFFFL);
        ChunkAccess chunk = cache.get(key);
        if (chunk == null) {
            chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
            if (chunk == null) {
                return null; // chunk 未加载
            }
            cache.put(key, chunk);
        }
        return chunk.getBlockState(new BlockPos(x, y, z));
    }

    /**
     * 半径变化（升级）后复位游标：从 machineY-1 层起点重新扫描。
     */
    private void resetScanCursor() {
        scanY = worldPosition.getY() - 1;
        scanX = -radius;
        scanZ = -radius;
    }

    /**
     * 首次运行初始化游标：尊重存档中的 scanY（若在 1..machineY-1 合法范围），
     * 否则从 machineY-1 层开始；极速模式直接锁定最近的非空气层。
     */
    private void initScanCursor() {
        int startY = worldPosition.getY() - 1;
        if (scanY < 1 || scanY > startY) {
            scanY = startY;
        }
        if (scanFast) {
            int y = findNextNonAirLayer(scanY, 0); // 首次初始化：不限步扫到底
            scanY = y < 1 ? 0 : y;
        }
        scanX = -radius;
        scanZ = -radius;
    }

    /**
     * 挖尽判定：scanY 到达 0（或极速模式无可挖层）。
     * 返回 true 表示矿已挖尽；停机由 {@link #conditionStart()} 门禁 +
     * {@link #applyEffect()} 早退执行（单元 B），恢复经 {@link #rpcToggleScanFast} / tick()。
     */
    public boolean scanExhausted() {
        return scanY <= 0;
    }

    /**
     * C→S: 切换标准/极速扫描模式（GUI 切换按钮）。
     * 挖尽停机后切换 → 复位游标重新扫描（恢复路径之一，另见 tick() 半径/模式变化恢复）。
     */
    @RPCMethod
    public void rpcToggleScanFast(RPCSender sender) {
        // C→S 方向服务端执行时 sender 为 ofClient(player)：isRemote()=true
        // （isServer() 仅对 S→C 方向为 true，此处用于 C→S 会永远不执行）。
        if (sender.isRemote()) {
            scanFast = !scanFast;
            if (scanCursorInitialized && scanExhausted()) {
                resetScanCursor();
            }
            setChanged();
        }
    }

    private boolean canBreak(BlockState state) {
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) {
            return false;
        }
        if (mode == 0) {
            // 标准模式宽泛名单（用户需求）：挖掘几乎所有无额外数据组件的方块。
            // 仅排除含方块实体/留存数据的方块（如存能量的机器、容器等，破坏会丢失数据），
            // 不再依赖 quarry_valids 标签；工具挖掘等级约束保留（isCorrectToolForDrops 含 mining_level）。
            return !state.hasBlockEntity() && tool.isCorrectToolForDrops(state);
        }
        if (mode == 3) {
            // Mineral 矿石扫描：仍限定 c:ores 标签 + 工具正确
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
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                return true;
            }
            // Use dynamic slot limit instead of hard-coded maxStackSize
            int slotLimit = itemHandler.getSlotLimit(i);
            if (ItemStack.isSameItem(existing, stack) && existing.getCount() + stack.getCount() <= slotLimit) {
                return true;
            }
        }
        return false;
    }

    /**
     * P3-T1c: Check if all drops can fit in output slots using simulated
     * accumulation into a local snapshot copy. This matches the exact same
     * logic as fitAll's commit, ensuring multiple different drops don't
     * compete for the same empty slot (unlike the per-item canFit approach).
     */
    private boolean canFitAll(List<ItemStack> stacks) {
        int outputStart = 1;
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

    /**
     * Snapshot output slots into a new ItemStack[] array (deep copy).
     */
    private ItemStack[] copyOutputSlots(int start, int count) {
        ItemStack[] copy = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            ItemStack s = itemHandler.getStackInSlot(start + i);
            copy[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
        return copy;
    }

    /**
     * Simulate inserting a single stack into the slot array, updating it in-place.
     * Returns the remaining stack (empty if fully inserted).
     * Logic matches the commit-phase insert exactly: respects slot limits,
     * handles same-item merge, and spills across slots.
     */
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
     * P3-T1c: Fit all drops using snapshot+simulate+commit pattern.
     * <ol>
     * <li>Snapshot output slots into local array</li>
     * <li>Simulate all drops on snapshot</li>
     * <li>Fail-fast if any remaining (pre-condition violated)</li>
     * <li>Commit snapshot atomically to real handler</li>
     * </ol>
     */
    private void fitAll(List<ItemStack> stacks) {
        // 1. Snapshot output slots
        int outputStart = 1;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] snapshot = copyOutputSlots(outputStart, slotCount);

        // 2. Simulate insertion on snapshot
        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(snapshot, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                // 3. Fail-fast: partial state must never reach the handler
                throw new IllegalStateException(
                        "Quarry cannot fit all drops: " + stack + " has " + remaining.getCount() + " remaining. " + "canFitAll pre-check should have prevented this.");
            }
        }

        // 4. Commit: write snapshot atomically to the real handler
        for (int i = 0; i < slotCount; i++) {
            itemHandler.setStackInSlot(outputStart + i, snapshot[i]);
        }
    }

    /**
     * Insert a single stack into the first available output slot.
     * Used by {@link #giveGeneratedLoot} for single-item generated loot.
     * <p>
     * NOTE: This method should not be reached from the canFitAll+fitAll flow
     * (which uses snapshot+commit). Fail-fast if stack doesn't fully fit.
     */
    private void insertFirstFit(ItemStack stack) {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
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
        // Fail-fast: prevent silent item loss
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
