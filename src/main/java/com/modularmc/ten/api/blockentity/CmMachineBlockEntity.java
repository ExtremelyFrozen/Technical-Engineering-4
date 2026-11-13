package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.capability.MachineEnergyStorage;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.option.RedstoneMode;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.blockentity.TransferNetworks;
import com.modularmc.ten.common.data.TENTags;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupBlast;
import com.modularmc.ten.common.item.upgrades.LevelupSmoke;
import com.modularmc.ten.common.item.upgrades.LevelupSyn;
import com.modularmc.ten.common.item.upgrades.UpgradeConstants;
import com.modularmc.ten.common.item.upgrades.UpgradeItem;
import com.modularmc.ten.utils.SkyLightHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 机器方块实体总基类：聚合能量/物品/流体三能力与升级、面配置、批处理锁、
 * 主动 IO、范围显示等横切机制；子类按机器形态选择下级抽象基类，
 * 不直接继承本类。
 */
public abstract class CmMachineBlockEntity extends CmBlockEntity implements IUpgradableMachine {

    // ───── ldlib2 自动同步/持久化字段（由 FieldManagedStorage 管理）─────
    @Persisted
    @DescSynced
    public int progress = 0;
    @Persisted
    @DescSynced
    public int maxProgress = 0;
    @Persisted
    @DescSynced
    public int energyStored = 0;    // mirror of energyStorage.getEnergyStored()
    @Persisted
    @DescSynced
    public int maxEnergyStored = 0; // mirror of maxStorageEnergy
    @Persisted
    @DescSynced
    public int fuel = 0;
    @Persisted
    @DescSynced
    public int maxFuel = 0;
    /** 当前能量消耗（FE/t）= 最近 5 tick 实测扣能的滑动平均；停机后随窗口滑出归零。 */
    @Persisted
    @DescSynced
    public int energyUse = 0;
    /** 最大能量消耗（FE/t）= 实际效率 × 理论批量 B（当前升级配置可达的峰值）。 */
    @Persisted
    @DescSynced
    public int energyUseMax = 0;
    @Persisted
    @DescSynced
    public int upgSize = 1;         // UPGSIZE
    @Persisted
    @DescSynced
    public int facingVal = 2;       // FACE (Direction.NORTH)
    @Persisted
    @DescSynced
    public int redstoneMode = RedstoneMode.OFF;
    @Persisted
    @DescSynced
    public boolean active = false;

    /**
     * 范围显示开关（采矿场/啃噬者等）：开启时客户端渲染工作范围线框（getRangeBoxes）。
     * 持久化 + 客户端同步（@DescSynced），经 rpcToggleRangeVisible 切换。
     */
    @Persisted
    @DescSynced
    public boolean rangeVisible = false;

    // ───── 范围调节面板：设备的 XYZ 半径与 XYZ 相对偏移（服务端权威）─────
    /**
     * 三轴半径，以 0.5 方块为单位：1 = 0.5 半径 = 每轴 1 方块，2 = 1.0 半径 = 每轴 2 方块。
     * 默认 1（0.5 半径）；面板第 0..2 行，每次点击 ±1，即 ±0.5 方块半径。
     */
    @Persisted
    @DescSynced
    public int rangeX = 1;
    @Persisted
    @DescSynced
    public int rangeY = 1;
    @Persisted
    @DescSynced
    public int rangeZ = 1;
    /** 三轴相对偏移（整数格，-64..64）：面板第 3..5 行，每次点击 ±1 格。 */
    @Persisted
    @DescSynced
    public int offsetX = 0;
    @Persisted
    @DescSynced
    public int offsetY = 0;
    @Persisted
    @DescSynced
    public int offsetZ = 0;
    /** 当前可调节半径上限：范围升级按倍率抬高，硬上限 {@link #ADJUST_RANGE_MAX}；每 tick 重算。 */
    @DescSynced
    public int adjustRangeCap = 64;

    /**
     * 应用工具/武器附魔开关（采矿场/破坏器/啂噬者）：默认开启，经 rpcToggleUseEnchantments 切换。
     */
    @Persisted
    @DescSynced
    public boolean useEnchantments = true;

    // Face config for client display — server-authoritative mirror, rebuilt from faceMode
    // maps in readTileData/doBaseData. NOT @Persisted (derived data; persistence lives in the
    // faceMode maps via dire* keys). NOT @DescSynced: int[] sync is unreliable (initial sync
    // fails, client keeps defaults until first manual sync) — instead buildMachineUI() pushes
    // all faces on GUI open, and syncAllFacesToClients() pushes on every change.
    public int[] energyFaceData = new int[6];
    public int[] itemFaceData = new int[6];
    public int[] fluidFaceData = new int[6];

    /** 是否已按「拆卸时朝向 → 放置时朝向」旋转过面配置（运行期一次性，不持久化）。 */
    private boolean faceRotationApplied;

    /**
     * 面语义版本键，逐级迁移：
     * <ul>
     * <li>1 = 被动面（BE_IN / BE_OUT / BOTH）不再由机器主动搬运，仅 OUT 主动推。
     * 缺键即旧档：产能类机器（引擎/单元）的能量 BOTH 面迁移为 OUT——旧版靠 BOTH 主动放电。</li>
     * <li>2 = 能量「向外」档位只留给产能类。版本 1 的存档里普通机器能量面仍是 BOTH/OUT，
     * 迁移为 BE_IN（保留外部可塞电），否则升级后会被归一化成关闭、整台机器断电。</li>
     * </ul>
     */
    private static final String FACE_SEMANTICS_KEY = "faceSemantics";
    private static final int FACE_SEMANTICS_PASSIVE = 1;
    private static final int FACE_SEMANTICS_ENERGY_DIRECTIONS = 2;

    // ───── 乘法模型与批处理字段 ─────
    /** 时长乘子（升级叠加；未装时长类升级时恒 1.0）。 */
    public double durationMultiplier = 1.0;
    /** 是否已安装 LevelupSyn（光合注能）。 */
    public boolean photosynInstalled = false;
    /**
     * 批量升级计数（Shulker +3 / Power +1）。服务端 doBaseData 每 tick 计算；
     * 客户端 @DescSynced 同步（范围预览 getRangeBoxes 需要读取真实 B）。
     */
    @Persisted
    @DescSynced
    public int batch = 0;

    /**
     * Locked batch size B_actual（批处理锁定的实际批量，四维取最小值）。
     * 0 = 未锁定（无批处理）；处理中 {@link #getLockedBatchSize()} 返回至少 1。
     */
    public int lockedB = 0;

    /**
     * Locked maxProgress（配方周期开始锁定，防 durationMultiplier 漂移）。
     * 0 = 未锁定（每周期重算）。
     */
    public int lockedMaxProgress = 0;

    /** 功率乘子（升级叠加；未装功率类升级时恒 1.0）。 */
    public double powerMultiplier = 1.0;
    /** 配方模式（Blast/Smoke 升级切换；默认熔炼）。 */
    public int recipeMode = IUpgradableMachine.RECIPE_MODE_SMELTING;
    /**
     * 无限能量传输（Stream 升级）。能量收发已不再限速，此标记当前无可观察效果，
     * 保留待升级内容重构时重新定义。
     */
    public boolean unlimitedEnergyTransfer = false;

    // ───── Machine fields ─────
    public MachineEnergyStorage energyStorage;
    public int maxStorageEnergy;
    public int initialEnergyStorage;
    public int efficientIn;
    public int initialEfficientIn;

    public MachineItemHandler itemHandler;
    public MachineItemHandler upgradeHandler;
    public int upgradeSize = 1;
    public int initialUpgradeSize = 1;
    public static final int MAX_UPGRADE_SLOTS = 6;

    /**
     * 视图态：升级槽面板处于展开态的玩家。不持久化、不 @DescSynced，
     * 只供服务端容器的 quickMoveStack 门控使用（见 upgradePanelSlot 的 mayPlace）。
     * 按玩家区分，避免同机多人打开 GUI 时互相覆盖；条目随 BE 卸载回收。
     */
    private final Set<UUID> upgradePanelOpenPlayers = ConcurrentHashMap.newKeySet();

    public List<MachineFluidTank> tanks = new ArrayList<>();

    public Map<Direction, Integer> energyFaceMode = new HashMap<>();
    public Map<Direction, Integer> itemFaceMode = new HashMap<>();
    public Map<Direction, Integer> fluidFaceMode = new HashMap<>();

    private boolean machineInitialised = false;
    private IFluidHandler combinedFluidHandler;

    public CmMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 默认面取 initialFaceMode*()：产能类（引擎/单元/创造单元）覆写为主动输出，其余默认被动双向。
        // 构造期调用可覆写方法有风险——这里所有覆写都只返回 FaceOption 常量，不读子类字段。
        // 之前硬编码 BOTH 会让「默认面」覆写完全失效：被动面不再主动搬运后，引擎与电池就停止供电。
        int energyMode = initialFaceModeEnergy();
        int itemMode = initialFaceModeItem();
        int fluidMode = initialFaceModeFluid();
        for (Direction d : Direction.values()) {
            energyFaceMode.put(d, energyMode);
            itemFaceMode.put(d, itemMode);
            fluidFaceMode.put(d, fluidMode);
        }
        Arrays.fill(energyFaceData, energyMode);
        Arrays.fill(itemFaceData, itemMode);
        Arrays.fill(fluidFaceData, fluidMode);
    }

    public abstract int inventorySize();

    public abstract int machineType();

    public static int kFE(double k) {
        return (int) (1000 * k);
    }

    public void setCapacity(int energy) {
        initialEnergyStorage = energy;
        maxStorageEnergy = energy;
        energyStorage = new MachineEnergyStorage(energy);
        energyStorage.setChangeListener(this::markDirty);
    }

    public void setEfficiency(int eff) {
        initialEfficientIn = efficientIn = eff;
    }

    public void initMachine() {
        if (machineInitialised) return;
        machineInitialised = true;

        if (initialEnergyStorage <= 0) initialEnergyStorage = 10000;
        if (maxStorageEnergy <= 0) maxStorageEnergy = initialEnergyStorage;

        if (itemHandler == null || itemHandler.getSlots() != inventorySize()) {
            itemHandler = new MachineItemHandler(inventorySize(), this::canPlaceItem);
        } else {
            itemHandler.setValidator(this::canPlaceItem);
        }
        itemHandler.setChangeListener(this::markDirty);

        if (upgradeHandler == null) {
            upgradeHandler = new MachineItemHandler(MAX_UPGRADE_SLOTS, this::validUpgrade);
        } else {
            upgradeHandler.setValidator(this::validUpgrade);
        }
        upgradeHandler.setChangeListener(this::onUpgradeChanged);

        if (energyStorage == null) {
            energyStorage = new MachineEnergyStorage(maxStorageEnergy);
        }
        energyStorage.setChangeListener(this::markDirty);

        for (var tank : tanks) tank.setChangeListener(this::markDirty);
        combinedFluidHandler = createCombinedFluidHandler();
        installBatchSlotLimit();
        // 能力对象已就绪，才能准确判定「这台方块有没有这种资源」→ 面配置归一化放最后
        normalizeFaceModes();
    }

    /** 非批量槽位的常规堆叠上限（升级槽、结构槽等）。 */
    private static final int DEFAULT_SLOT_LIMIT = 64;

    /**
     * 安装全机器统一的批处理倍增堆叠上限。由 {@link #initMachine()} 装一次即可——
     * provider 每次求值都现读理论 B，不需要随配方变化或锁定批量重装。
     */
    protected void installBatchSlotLimit() {
        if (itemHandler == null) return;
        itemHandler.setDynamicSlotLimit(this::batchSlotLimitFor);
    }

    /**
     * 槽位堆叠上限：可进/可出的槽按「物品自身上限 × 理论 B」倍增，不可堆叠物品保持 1
     * （工具槽等自动落在此规则内），其余槽用 {@value #DEFAULT_SLOT_LIMIT}。
     * 超 99 的部分由 {@code MachineItemHandler} 的 BigCount 负责持久化。
     * 子类可覆写以表达个别槽位的特殊语义。
     * <p>
     * 注意：这里不能用 {@code Item.ABSOLUTE_MAX_STACK_SIZE} 钳制——该常量在 1.21.1 就是 99，
     * 一旦套上会把所有批量倍增上限统一压回 99（B≥2 时 128/192/…全变 99），
     * 使批处理堆叠特性静默失效。
     */
    protected int batchSlotLimitFor(int slot, ItemStack candidate) {
        IngredientType type = slotType(slot);
        if (!type.canIn() && !type.canOut()) {
            return DEFAULT_SLOT_LIMIT;
        }
        // 空槽查询时无从得知物品上限，按 64×B 预留（真正插入时会带 candidate 再算一次）
        int maxStackSize = candidate.isEmpty() ? DEFAULT_SLOT_LIMIT : Math.max(1, candidate.getMaxStackSize());
        if (maxStackSize <= 1) {
            return 1;
        }
        return (int) Math.min((long) maxStackSize * getTheoreticalBatchSize(), Integer.MAX_VALUE);
    }

    // ───── ldlib2 NBT: 确保反序列化前 handler 已初始化 ─────
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        initMachine();
        super.loadAdditional(tag, registries);
    }

    /**
     * 是否支持升级槽 UI：Cell/CreativeCell/Channel 等不支持，覆写返回 false。
     */
    public boolean supportsUpgradeSlots() {
        return true;
    }

    /**
     * 是否注册右侧「范围调节」tab 与展开面板：目前只有方块破坏器/方块成型器接入，
     * 其余机器即便存了 rangeX/Y/Z 也不显示该入口。
     */
    public boolean supportsRangeAdjust() {
        return false;
    }

    public boolean hasUpgrade() {
        if (upgradeHandler == null) return false;
        for (int i = 0; i < upgradeHandler.getSlots(); i++) {
            if (!upgradeHandler.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    public boolean hasUpgrade(Class<? extends UpgradeItem> upgradeClass) {
        if (upgradeHandler == null) return false;
        for (int i = 0; i < upgradeHandler.getSlots(); i++) {
            ItemStack stack = upgradeHandler.getStackInSlot(i);
            if (!stack.isEmpty() && upgradeClass.isInstance(stack.getItem())) return true;
        }
        return false;
    }

    public boolean hasSideBar() {
        return true;
    }

    public int getActualEfficiency() {
        // 乘法模型——实际效率 = initialEfficientIn × powerMultiplier（升级乘子叠加）
        return Math.max(1, (int) Math.round(initialEfficientIn * powerMultiplier));
    }

    public double getActualEfficiencyPercent() {
        return 1.0;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean a) {
        if (active == a) return;
        active = a;
        if (level != null) {
            BlockState state = getBlockState();
            // [修复] BE 已被移除（破坏/替换流程中）时不得回写方块状态——
            // 否则破坏 setBlock(air) 后 setActive(false) 会把旧 state 写回，
            // 吞掉破坏（方块变成未连接态而非掉落）
            if (isRemoved()) return;
            if (state.hasProperty(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE) && state.getValue(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE) != a) {
                level.setBlock(worldPosition,
                        state.setValue(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE, a), Block.UPDATE_ALL);
            }
        }
        markDirty();
    }

    public void setFacing(Direction direction) {
        if (direction == null) return;
        int value = direction.get3DDataValue();
        if (facingVal == value) return;
        facingVal = value;
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(com.modularmc.ten.common.block.machine.HorizontalMachineBlock.FACING) && direction.getAxis().isHorizontal()) {
                level.setBlock(worldPosition,
                        state.setValue(com.modularmc.ten.common.block.machine.HorizontalMachineBlock.FACING, direction), Block.UPDATE_ALL);
            } else if (state.hasProperty(com.modularmc.ten.common.block.machine.DirectionalMachineBlock.FACING)) {
                level.setBlock(worldPosition,
                        state.setValue(com.modularmc.ten.common.block.machine.DirectionalMachineBlock.FACING, direction), Block.UPDATE_ALL);
            }
        }
        markDirty();
    }

    /**
     * 机器朝向：优先读方块状态的 FACING 属性（与方块实际朝向一致，权威）。
     * 无 FACING 属性时回退到 {@link #facingVal}（持久化镜像，供 GUI/恢复使用）。
     * 同步镜像字段，保持 GUI/持久化一致。
     */
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.modularmc.ten.common.block.machine.HorizontalMachineBlock.FACING)) {
            Direction d = state.getValue(com.modularmc.ten.common.block.machine.HorizontalMachineBlock.FACING);
            facingVal = d.get3DDataValue();
            return d;
        }
        if (state.hasProperty(com.modularmc.ten.common.block.machine.DirectionalMachineBlock.FACING)) {
            Direction d = state.getValue(com.modularmc.ten.common.block.machine.DirectionalMachineBlock.FACING);
            facingVal = d.get3DDataValue();
            return d;
        }
        return Direction.from3DDataValue(facingVal);
    }

    /**
     * 把六面配置从 {@code from} 朝向的逻辑面整体旋转到 {@code to} 朝向。
     * <p>
     * 面配置按世界方向存储（capability 查询直接用 Direction），GUI 与拆卸重放都以
     * 「机器正面」为逻辑基准。机器被拆下换方向重放时，必须把旧朝向的逻辑面映射到新朝向，
     * 否则「正面」会跑到世界北面去。
     */
    private void rotateFaceModes(Direction from, Direction to) {
        int[] energy = new int[6];
        int[] item = new int[6];
        int[] fluid = new int[6];
        for (Direction oldDir : Direction.values()) {
            Direction newDir = faceForLogicalSide(to, logicalSideForFace(from, oldDir));
            if (newDir == null) {
                continue;
            }
            int newIndex = newDir.get3DDataValue();
            energy[newIndex] = energyFaceMode.getOrDefault(oldDir, initialFaceModeEnergy());
            item[newIndex] = itemFaceMode.getOrDefault(oldDir, initialFaceModeItem());
            fluid[newIndex] = fluidFaceMode.getOrDefault(oldDir, initialFaceModeFluid());
        }
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            energyFaceMode.put(direction, energy[index]);
            itemFaceMode.put(direction, item[index]);
            fluidFaceMode.put(direction, fluid[index]);
        }
    }

    /**
     * 逻辑面编号 → 世界方向；编号为 0 前 / 1 后 / 2 左 / 3 右 / 4 上 / 5 下。
     *
     * <p>
     * 任意朝向都是<b>双射</b>（六个方向互不重复），包括频道这类六向方块朝上/朝下的情形：
     * 竖直朝向没有天然的「上」，以北方为向上参考，左右按水平叉积补齐。旧实现把上/下固定成
     * UP/DOWN，竖直朝向下会与「前/后」落到同一方向——面板上改一个面会连带改另一个面。
     */
    @Nullable
    public static Direction faceForLogicalSide(Direction facing, int logicalSide) {
        // 朝上/朝下时没有屏幕意义上的「上」，固定取北方；水平朝向沿用世界 UP。
        Direction up = facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        // 左 = front × up：水平朝向等价于 facing.getClockWise()（Y 轴朝向调用它会抛异常）。
        Direction left = facing.getAxis() == Direction.Axis.Y ? (facing == Direction.UP ? Direction.WEST : Direction.EAST) : facing.getClockWise();
        return switch (logicalSide) {
            case 0 -> facing;
            case 1 -> facing.getOpposite();
            case 2 -> left;
            case 3 -> left.getOpposite();
            case 4 -> up;
            case 5 -> up.getOpposite();
            default -> null;
        };
    }

    /** 世界方向 → 逻辑面编号；{@link #faceForLogicalSide} 的逆映射，找不到时返回 -1。 */
    public static int logicalSideForFace(Direction facing, Direction direction) {
        for (int side = 0; side < 6; side++) {
            if (faceForLogicalSide(facing, side) == direction) {
                return side;
            }
        }
        return -1;
    }

    public int initialFaceModeEnergy() {
        // 普通机器是耗能方：默认「被动输入」——外部电缆/管道可塞电，但拿不走机器的工作电量。
        // 产能类（引擎 / 单元 / 创造单元）覆写为主动输出。
        return FaceOption.BE_IN;
    }

    public int initialFaceModeItem() {
        return FaceOption.BOTH;
    }

    public int initialFaceModeFluid() {
        return FaceOption.BOTH;
    }

    public boolean hasFaceCapabilityEnergy(@Nullable Direction side) {
        return true;
    }

    public boolean hasFaceCapabilityItem(@Nullable Direction side) {
        return true;
    }

    public boolean hasFaceCapabilityFluid(@Nullable Direction side) {
        return !tanks.isEmpty();
    }

    // ───── 面配置可选范围：由方块标签组合决定 ─────

    /**
     * 该面类型在此方块上是否存在，决定面板该页有没有「关闭」之外的可选模式。
     *
     * <p>
     * 只问「有没有这种资源」，与朝向/接入状态无关——频道方块的面能力要接入后才暴露，
     * 但玩家在未接入时也该能预配置面，故不走 {@link #hasFaceCapabilityFluid} 那套带 side 的门控。
     */
    protected boolean supportsFaceResource(int changeType) {
        return switch (changeType) {
            case FaceOption.TYPE_ENERGY -> energyStorage != null;
            case FaceOption.TYPE_ITEM -> itemHandler != null && itemHandler.getSlots() > 0;
            case FaceOption.TYPE_FLUID -> !tanks.isEmpty();
            default -> false;
        };
    }

    /**
     * 主动面（{@link FaceOption#IN} / {@link FaceOption#OUT}）是否允许，默认允许；
     * 带 {@code ten:no_active_face_energy} / {@code _item} / {@code _fluid} 的方块被去除。
     *
     * <p>
     * 三个标签同一极性（去除）、同一条判定分支，不做「某资源白名单、另一资源黑名单」的混搭，
     * 否则同一份配置在不同资源页上的行为就会不对称。
     *
     * <p>
     * 默认按方块标签判定；需要更细粒度的方块可覆写。客户端也能读：原版会把方块标签
     * 同步给客户端，故标签判定在 GUI 侧同样成立。
     */
    protected boolean allowsActiveFace(int changeType) {
        BlockState state = getBlockState();
        return switch (changeType) {
            case FaceOption.TYPE_ENERGY -> !state.is(TENTags.NO_ACTIVE_FACE_ENERGY);
            case FaceOption.TYPE_ITEM -> !state.is(TENTags.NO_ACTIVE_FACE_ITEM);
            case FaceOption.TYPE_FLUID -> !state.is(TENTags.NO_ACTIVE_FACE_FLUID);
            default -> false;
        };
    }

    /**
     * 该面模式在这个方向上是否有实际用途——按设备功能性判定，与方块标签无关。
     *
     * <p>
     * 标签回答的是「这个方块整体该不该自搬」（共享存储型方块），这里回答「这台设备的这个方向
     * 有没有实际用途」。两层都过，该模式才进面配置轮换；方向级判定直接吃设备已有的功能事实
     * （{@link #canExternalExtract()} / {@link #slotType} / {@link #tankType}），随实现自动变化，
     * 不需要另立标签。
     *
     * <p>
     * 六个模式归成两种能力：<b>向内</b>（{@code IN} 机器自拉、{@code BE_IN} 外部塞入、{@code BOTH}
     * 的收侧）要求设备有可进项；<b>向外</b>（{@code OUT} 机器自推、{@code BE_OUT} 外部抽取、
     * {@code BOTH} 的发侧）要求设备有可出项。被动档位同样要过这层——否则只进不出的引擎会出现
     * 「被动输出」、只有 XP 输出罐的熔炉会出现「被动输入」这种点了也不生效的档位。
     * <ul>
     * <li><b>能量输出</b>：仅 {@link #canExternalExtract()} 的产能类（引擎 / 能量单元 / 创造单元）。
     * 普通机器没有产能路径，任何「向外」档位都拿不到电。</li>
     * <li><b>能量输入</b>：引擎除外（纯电源被灌电只会被自己推回）。</li>
     * <li><b>物品 / 流体</b>：按 {@code slotType()} / {@code tankType()} 有没有可进、可出项判定。</li>
     * </ul>
     */
    protected boolean supportsFaceDirection(int changeType, int mode) {
        boolean needIn = mode == FaceOption.IN || mode == FaceOption.BE_IN || mode == FaceOption.BOTH;
        boolean needOut = mode == FaceOption.OUT || mode == FaceOption.BE_OUT || mode == FaceOption.BOTH;
        if (!needIn && !needOut) {
            return true; // OFF 不需要任何方向能力
        }
        return switch (changeType) {
            case FaceOption.TYPE_ENERGY -> (!needIn || canPullEnergy()) && (!needOut || canExternalExtract());
            case FaceOption.TYPE_ITEM -> (!needIn || hasItemSlot(false)) && (!needOut || hasItemSlot(true));
            case FaceOption.TYPE_FLUID -> (!needIn || hasTank(false)) && (!needOut || hasTank(true));
            default -> false;
        };
    }

    /** 能量主动输入是否有意义：普通机器取电、能量单元充电；引擎是纯电源，灌电只会来回搬运。 */
    private boolean canPullEnergy() {
        if (!canExternalExtract()) {
            return true;
        }
        return machineType() == MachineType.CELL || machineType() == MachineType.CREATIVE_CELL;
    }

    /** 是否存在可进（{@code output=false}）/ 可出（{@code output=true}）的物品槽。 */
    private boolean hasItemSlot(boolean output) {
        if (itemHandler == null) {
            return false;
        }
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            IngredientType type = slotType(slot);
            if (output ? type.canOut() : type.canIn()) {
                return true;
            }
        }
        return false;
    }

    /** 是否存在可进（{@code output=false}）/ 可出（{@code output=true}）的流体罐。 */
    private boolean hasTank(boolean output) {
        for (int tank = 0; tank < tanks.size(); tank++) {
            IngredientType type = tankType(tank);
            if (output ? type.canOut() : type.canIn()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 面配置可选范围的类型级判定：关闭恒可选；其余五档要求该资源存在，且该方向在设备功能上有意义
     * （{@link #supportsFaceDirection}）；主动两态另需标签未去除（{@link #allowsActiveFace}）。
     * 写入路径请用带方向的 {@link #allowsFaceMode(Direction, int, int)}——本方法不看「这一面是不是入口」。
     *
     * <p>
     * GUI 循环（{@link #rpcCycleFaceMode}）与读档归一化（{@link #normalizeFaceModes}）都走带方向的
     * {@link #allowsFaceMode(Direction, int, int)}，因此轮换表天然是「按设备现算的活列表」：
     * 任何一层判定变了，下一次点击就按新集合走。
     * 新增的「不允许组合」都应加在本方法或其下游，不要散落到调用点。
     */
    public final boolean allowsFaceMode(int changeType, int mode) {
        if (!FaceOption.isValid(mode)) {
            return false;
        }
        if (mode == FaceOption.OFF) {
            return true;
        }
        if (!supportsFaceResource(changeType)) {
            return false;
        }
        if (!supportsFaceDirection(changeType, mode)) {
            return false;
        }
        return !FaceOption.isActive(mode) || allowsActiveFace(changeType);
    }

    /**
     * 方向级可选范围：在类型级 {@link #allowsFaceMode(int, int)} 之外再收一层
     * 「这一面是不是本设备的面入口」。
     *
     * <p>
     * 默认六面都是入口；频道方块只有朝向外侧的那一面接容器，其余五面即使存了档位也暴露不出
     * capability，写入路径必须落回关闭。类型级判定不带方向，GUI 循环、连接器粘贴、拆卸重放
     * 都必须走这个带方向的版本，否则会在死面上留下「配了也不生效」的假象。
     */
    public final boolean allowsFaceMode(@Nullable Direction direction, int changeType, int mode) {
        if (!FaceOption.isValid(mode)) {
            return false;
        }
        if (direction != null && !supportsFaceOnSide(direction)) {
            return mode == FaceOption.OFF;
        }
        return allowsFaceMode(changeType, mode);
    }

    /** 该世界方向是否是本设备的面配置入口。默认六面都可配，共享存储型方块可收窄。 */
    protected boolean supportsFaceOnSide(Direction direction) {
        return true;
    }

    /**
     * 把面配置归一到可选集合内（非法项落回 {@link FaceOption#OFF}）。
     *
     * <p>
     * 需要归一化的场景：玩家在旧版本给频道方块配过主动面，或整合包收回了某些方块的
     * 主动面（补上 {@code no_active_face_*} 标签）。放在 {@link #initMachine()} 末尾执行——
     * 此时能力对象已建好，{@link #supportsFaceResource} 才能给出准确答案。
     */
    protected void normalizeFaceModes() {
        normalizeFaceMap(energyFaceMode, FaceOption.TYPE_ENERGY);
        normalizeFaceMap(itemFaceMode, FaceOption.TYPE_ITEM);
        normalizeFaceMap(fluidFaceMode, FaceOption.TYPE_FLUID);
    }

    private void normalizeFaceMap(Map<Direction, Integer> map, int changeType) {
        for (Direction direction : Direction.values()) {
            int mode = map.getOrDefault(direction, FaceOption.OFF);
            if (!allowsFaceMode(direction, changeType, mode)) {
                map.put(direction, FaceOption.OFF);
            }
        }
    }

    /** 外部能否向该面写入能量：被动输入 / 被动双向（主动输出面按既有策略一并放行）。 */
    protected boolean canReceiveEnergy(@Nullable Direction side) {
        if (!hasFaceCapabilityEnergy(side)) return false;
        if (side == null) return true;
        return FaceOption.allowsExternalInsert(energyFaceMode.getOrDefault(side, FaceOption.OFF));
    }

    /** 外部能否从该面取出能量：被动输出 / 被动双向。主动输出面由机器自推，不开放外部取。 */
    protected boolean canExtractEnergy(@Nullable Direction side) {
        if (!hasFaceCapabilityEnergy(side)) return false;
        if (side == null) return true;
        return FaceOption.allowsExternalExtract(energyFaceMode.getOrDefault(side, FaceOption.OFF));
    }

    /**
     * 物品面接收门控：被动输入 / 被动双向可被外部塞入（主动输出面按既有策略一并放行）；
     * 主动输入由机器自身拉取（{@link #doActiveItemIo}），不开放给外部。
     */
    protected boolean canReceiveItem(@Nullable Direction side) {
        if (!hasFaceCapabilityItem(side)) return false;
        if (side == null) return true;
        return FaceOption.allowsExternalInsert(itemFaceMode.getOrDefault(side, FaceOption.OFF));
    }

    /**
     * 物品面提取门控：被动输出 / 被动双向可被外部抽取；
     * 主动输出由机器自身推出（{@link #doActiveItemIo}），不开放给外部。
     */
    protected boolean canExtractItem(@Nullable Direction side) {
        if (!hasFaceCapabilityItem(side)) return false;
        if (side == null) return true;
        return FaceOption.allowsExternalExtract(itemFaceMode.getOrDefault(side, FaceOption.OFF));
    }

    /** 外部能否向该面写入流体：被动输入 / 被动双向（主动输出面按既有策略一并放行）。 */
    protected boolean canReceiveFluid(@Nullable Direction side) {
        if (!hasFaceCapabilityFluid(side)) return false;
        if (side == null) return true;
        return FaceOption.allowsExternalInsert(fluidFaceMode.getOrDefault(side, FaceOption.OFF));
    }

    /** 外部能否从该面取出流体：被动输出 / 被动双向。主动输出面由机器自推，不开放外部取。 */
    protected boolean canExtractFluid(@Nullable Direction side) {
        if (!hasFaceCapabilityFluid(side)) return false;
        if (side == null) return true;
        return FaceOption.allowsExternalExtract(fluidFaceMode.getOrDefault(side, FaceOption.OFF));
    }

    public boolean signalAllowRun() {
        if (level == null) return true;
        boolean power = level.hasNeighborSignal(worldPosition);
        return switch (redstoneMode) {
            case RedstoneMode.LOW -> !power;
            case RedstoneMode.HIGH -> power;
            default -> true;
        };
    }

    public boolean energyAllowRun() {
        if (energyStorage == null) return false;
        // 批处理锁感知——锁定时按总 FE/t（baseFe × lockedB）检查，
        // 防止 stored >= baseFe 但 < totalFe 时 active 闪烁。
        int baseFe = getActualEfficiency();
        int checkFe = hasLockedBatch() ? (int) Math.round((double) baseFe * getLockedBatchSize()) : baseFe;
        return switch (machineType()) {
            case com.modularmc.ten.api.option.MachineType.GENERATOR, com.modularmc.ten.api.option.MachineType.ENGINE_SOLAR, com.modularmc.ten.api.option.MachineType.ENGINE_EXTRACTION, com.modularmc.ten.api.option.MachineType.ENGINE_METAL, com.modularmc.ten.api.option.MachineType.ENGINE_BIOMASS -> energyStorage.getEnergyStored() + checkFe <= maxStorageEnergy;
            default -> energyStorage.getEnergyStored() >= checkFe;
        };
    }

    /**
     * Syn 光合注能：在有光条件下向本机储能注入固定 FE/t，
     * 不向相邻 capability 或网络推送能量。仅在已装 LevelupSyn 且机器为
     * PROCESS/EFFECT 类型时生效。
     *
     * @return 实际注入的 FE 量（0 ~ SYN_PHOTOSYN_FE）
     */
    protected int tryInjectPhotosynEnergy() {
        if (!photosynInstalled) return 0;
        if (!(isType("MACHINE_PROCESS") || isType("MACHINE_EFFECT"))) {
            return 0;
        }
        if (energyStorage == null) return 0;
        if (!SkyLightHelper.hasEffectiveLight(level, worldPosition)) return 0;
        return energyStorage.receiveEnergy(UpgradeConstants.SYN_PHOTOSYN_FE, false);
    }

    // ───── 批处理锁定接口（含四维 B_actual 计算与 validateAndLockB）─────

    public boolean hasLockedBatch() {
        return lockedB != 0;
    }

    public void clearLockedBatch() {
        lockedB = 0;
        lockedMaxProgress = 0;
    }

    public void lockBatchForNewOperation(int B) {
        if (B <= 0) {
            throw new IllegalArgumentException("Batch size must be positive, got " + B);
        }
        lockedB = Math.min(B, 19);
    }

    /**
     * @return locked batch size B_actual，最小 1（lockedB=0 时返回 1）
     */
    public int getLockedBatchSize() {
        return Math.max(1, lockedB);
    }

    /**
     * @return 理论批处理 B（1 + Σbatch，钳位 1..19）；装批量升级即常驻生效
     */
    public int getTheoreticalBatchSize() {
        return Math.max(1, Math.min(1 + batch, 19));
    }

    public boolean hasLockedMaxProgress() {
        return lockedMaxProgress != 0;
    }

    public void lockMaxProgressForNewOperation(int progress) {
        lockedMaxProgress = Math.max(1, progress);
    }

    public static int safeMultiply(int a, int b) {
        if (a <= 0 || b <= 0) return 0;
        long result = (long) a * b;
        return (int) Math.min(result, Integer.MAX_VALUE);
    }

    /** C→S：切换范围显示（服务端执行，sender 为 ofClient(player)）。 */
    @RPCMethod
    public void rpcToggleRangeVisible(RPCSender sender) {
        if (sender.isRemote()) {
            rangeVisible = !rangeVisible;
            setChanged();
        }
    }

    /** 范围调节面板六行对应的同步字段名：0..2 = 范围 XYZ，3..5 = 偏移 XYZ。 */
    public static final String[] ADJUST_FIELDS = { "rangeX", "rangeY", "rangeZ", "offsetX", "offsetY", "offsetZ" };
    /** 半径/偏移硬上限：半径以 0.5 方块为单位（64 = 32 半径 = 每轴 64 方块），偏移为整数格 ±64。 */
    public static final int ADJUST_RANGE_MAX = 64;
    /**
     * 接入范围调节的机器，其可调节半径上限的起点：1 半单位 = 0.5 半径 = 1×1×1 方块。
     * 与 {@code rangeX/Y/Z} 默认值一致，故初始作用范围与未装升级时的上限都是 1×1×1；
     * 旧档里按旧语义存下的较大值也会被 {@code doBaseData()} 的钳位收回 1。
     * 每件范围升级 ×2（1→2→4→8→16→32→64 半单位），硬上限 {@link #ADJUST_RANGE_MAX}。
     */
    public static final int ADJUST_BASE_CAP = 1;
    /** 单次遍历方块的硬上限：超大范围时截断扫描，避免每 tick 卡顿。 */
    public static final int ADJUST_SCAN_LIMIT = 4096;

    /** 工作盒的方块坐标闭区间（含端点）。 */
    public record AdjustBounds(BlockPos min, BlockPos max) {}

    /**
     * 范围调节面板驱动的工作盒：中心 = 机器位置 + XYZ 相对偏移。
     * 半径 = range/2 方块 → 每轴方块数 = 2×半径 = range，以中心向正负对称取整展开
     * （range=1 只有中心 1 格，range=2 为 2 格，range=3 为 3 格……）。
     * 调用方需跳过机器自身所在格，并用 {@link #ADJUST_SCAN_LIMIT} 截断遍历。
     */
    public AdjustBounds adjustedBounds() {
        int cx = worldPosition.getX() + offsetX;
        int cy = worldPosition.getY() + offsetY;
        int cz = worldPosition.getZ() + offsetZ;
        int rx = Math.max(1, rangeX), ry = Math.max(1, rangeY), rz = Math.max(1, rangeZ);
        return new AdjustBounds(
                new BlockPos(cx - (rx - 1) / 2, cy - (ry - 1) / 2, cz - (rz - 1) / 2),
                new BlockPos(cx + rx / 2, cy + ry / 2, cz + rz / 2));
    }

    /** 工作盒的世界绝对坐标 AABB（方块边界），供范围线框预览；与实际遍历逐格一致。 */
    public net.minecraft.world.phys.AABB adjustedRangeBox() {
        var bounds = adjustedBounds();
        return new net.minecraft.world.phys.AABB(
                bounds.min().getX(), bounds.min().getY(), bounds.min().getZ(),
                bounds.max().getX() + 1, bounds.max().getY() + 1, bounds.max().getZ() + 1);
    }

    /** 范围升级：把可调节半径上限按倍率放大，钳在硬上限内。 */
    @Override
    public void applyAdjustRangeCapMultiplier(int factor) {
        if (factor <= 0) return;
        adjustRangeCap = (int) Math.min(ADJUST_RANGE_MAX, (long) adjustRangeCap * factor);
    }

    /** GUI 读取指定行的当前值；越界行返回 0。 */
    public int adjustValue(int row) {
        return switch (row) {
            case 0 -> rangeX;
            case 1 -> rangeY;
            case 2 -> rangeZ;
            case 3 -> offsetX;
            case 4 -> offsetY;
            case 5 -> offsetZ;
            default -> 0;
        };
    }

    /** 面板显示值：半径行显示 0.5 步进的小数，偏移行显示整数格。 */
    public String adjustDisplayValue(int row) {
        int value = adjustValue(row);
        return row < 3 ? String.valueOf(value / 2.0) : Integer.toString(value);
    }

    /**
     * C→S：范围调节面板加/减按钮。row 对应 {@link #ADJUST_FIELDS}，delta 只允许 ±1；
     * mode：0 步进 ±1（即半径 ±0.5）、1 按住 Shift 直接调到最大、-1 按住 Alt 直接调到最小。
     * 半径钳 1..{@code adjustRangeCap}（范围升级抬高），偏移钳 ±{@link #ADJUST_RANGE_MAX}。
     * 改值后显式推送对应 @DescSynced 字段。
     */
    @RPCMethod
    public void rpcAdjustRange(RPCSender sender, int row, int delta, int mode) {
        if (!sender.isRemote()) return;
        if (row < 0 || row >= ADJUST_FIELDS.length || (delta != 1 && delta != -1)) return;
        int current = adjustValue(row);
        boolean isRange = row < 3;
        int min = isRange ? 1 : -ADJUST_RANGE_MAX;
        int max = isRange ? adjustRangeCap : ADJUST_RANGE_MAX;
        int next = switch (mode) {
            case 1 -> max;
            case -1 -> min;
            default -> Math.max(min, Math.min(max, current + delta));
        };
        if (next == current) return;
        switch (row) {
            case 0 -> rangeX = next;
            case 1 -> rangeY = next;
            case 2 -> rangeZ = next;
            case 3 -> offsetX = next;
            case 4 -> offsetY = next;
            case 5 -> offsetZ = next;
            default -> {}
        }
        setChanged();
        markDirty(ADJUST_FIELDS[row]);
    }

    /** C→S：切换是否应用工具/武器附魔（服务端执行）。 */
    @RPCMethod
    public void rpcToggleUseEnchantments(RPCSender sender) {
        if (sender.isRemote()) {
            useEnchantments = !useEnchantments;
            setChanged();
        }
    }

    protected ItemStack effectiveToolForDrops(ItemStack tool) {
        if (tool.isEmpty()) {
            return tool;
        }
        // 开关关闭时移除全部附魔组件（时运/精准等不生效），保留工具本体；开启时原样返回
        if (!useEnchantments) {
            ItemStack copy = tool.copy();
            copy.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
            return copy;
        }
        return tool;
    }

    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        return java.util.Collections.emptyList();
    }

    /**
     * Pure calculation of B_actual from all dimensional constraints.
     * 委托 {@link BatchMath#calculateBActual}（无 MC 依赖，可单元测试）。
     */
    public static int calculateBActual(int B_theory, int B_byItems, int B_byFluids,
                                       int B_byOutput, int B_byEnergy) {
        return BatchMath.calculateBActual(B_theory, B_byItems, B_byFluids, B_byOutput, B_byEnergy);
    }

    /**
     * 计算并锁定 B_actual（四维约束取最小，钳位 0..19）。
     * 若 B_actual < 1 则清锁并返回 false（调用方应取消启动）。
     */
    protected boolean validateAndLockB(int B_theory, int B_byItems, int B_byFluids,
                                       int B_byOutput, int B_byEnergy) {
        int B = calculateBActual(B_theory, B_byItems, B_byFluids, B_byOutput, B_byEnergy);
        if (B <= 0) {
            clearLockedBatch();
            return false;
        }
        lockBatchForNewOperation(B);
        return true;
    }

    public final boolean canExternalExtract() {
        return switch (machineType()) {
            case com.modularmc.ten.api.option.MachineType.GENERATOR, com.modularmc.ten.api.option.MachineType.ENGINE_SOLAR, com.modularmc.ten.api.option.MachineType.ENGINE_EXTRACTION, com.modularmc.ten.api.option.MachineType.ENGINE_METAL, com.modularmc.ten.api.option.MachineType.ENGINE_BIOMASS, com.modularmc.ten.api.option.MachineType.CELL, com.modularmc.ten.api.option.MachineType.CREATIVE_CELL -> true;
            default -> false;
        };
    }

    /**
     * 旧档 {@code faceSemantics} 1 → 2 迁移的适用范围：只有「能量向外档位在新语义下不成立」的
     * 方块才需要把 {@code BOTH}/{@code OUT} 改写成 {@code BE_IN}。
     * <p>
     * 不能只看 {@link #canExternalExtract()}：频道这类共享存储两向都真实存在（覆写了
     * {@link #supportsFaceDirection}），改写会让旧档频道停止对外供电。按「这台设备到底有没有
     * 能量输出档位」判定，与面可选范围共用同一份事实。
     */
    private boolean migratesEnergyOutputToPassiveInput() {
        return !canExternalExtract() && !supportsFaceDirection(FaceOption.TYPE_ENERGY, FaceOption.OUT);
    }

    /**
     * 外部推进进度（冷却器等）：默认作用于单流水线 {@code progress}。
     * 多流水线机器覆写此方法，把推进量分派到自己的各条流水线，避免只改到一台
     * 机器的单进度镜像上。
     */
    public void advanceProgress(int ticks) {
        progress = Math.min(maxProgress, progress + ticks);
        setChanged();
    }

    public void doBaseData() {
        initMachine();
        if (energyStorage == null) return;
        resetUpgradeEffects();
        // ── 单次 apply：每 doBaseData 周期重置后仅应用一次（旧版双重 apply
        // 使乘子升级效果平方化、radius/batch 每 tick 累加）──
        applyUpgradeEffects();

        // ── 理论 B 批量缩放储能：批量升级后每 tick 总能耗 fePerTick =
        // efficientIn × lockedB，储能不随 B 放大将导致扣能永久不足 → 全机停滞。
        // 收发速率已不限流，因此这里只缩放容量 ──
        int theoreticalB = getTheoreticalBatchSize();
        int effectiveStorage = safeMultiply(initialEnergyStorage, theoreticalB);

        // setCapacity 会在存量超出新容量时截断；batch 只增不减，不缩容
        energyStorage.setCapacity(effectiveStorage);

        maxStorageEnergy = effectiveStorage;

        // ── Write to ldlib2 @DescSynced fields ──
        progress = Math.max(progress, 0);
        // 统一触发入口：@DescSynced 字段每 tick 写入后显式置脏，不依赖轮询值比较
        // （背景：探针证实 progress 自增后客户端/服务端镜像树均读到恒 0，强制推送绕开值比较环节）
        markDirty("progress");
        markDirty("maxProgress");
        markDirty("energyStored");
        markDirty("maxEnergyStored");
        markDirty("energyUse");
        markDirty("energyUseMax");
        maxEnergyStored = maxStorageEnergy;
        energyStored = energyStorage.getEnergyStored();
        // 信息栏：当前消耗取最近 5 tick 的实测平均（本 tick 扣能在 process() 里记账，下一 tick 进窗口）；
        // 最大消耗取当前升级配置可达的峰值（实际效率 × 理论 B）
        energyUse = rollEnergyUseSample();
        energyUseMax = safeMultiply(getActualEfficiency(), theoreticalB);
        upgSize = upgradeSize;
        // ── 流体容量批量缩放：每 tank 按构造初始容量 × theoreticalB 独立缩放 ──
        if (!tanks.isEmpty()) {
            for (var tank : tanks) {
                int effectiveFluidCapacity = safeMultiply(tank.getInitialCapacity(), theoreticalB);
                tank.setCapacity(Math.max(effectiveFluidCapacity, tank.getFluidAmount()));
            }
        }

        if (energyStorage.getEnergyStored() > maxStorageEnergy) {
            energyStorage.setEnergy(maxStorageEnergy);
        }

        // ── 主动 IO：只有 IN 自发拉取、OUT 自发推送；被动面（BE_IN / BE_OUT / BOTH）只对
        // capability 开放，机器不替外部搬运（无速率上限，每次尽力搬空）──
        doActiveItemIo();
        doActiveFluidIo();
        doActiveEnergyIo();

        // 范围升级卸下后 cap 变小、或旧档半径低于 1：收回合法区间（仅越界时写，避免每 tick 推同步）
        if (rangeX < 1 || rangeY < 1 || rangeZ < 1 || rangeX > adjustRangeCap || rangeY > adjustRangeCap || rangeZ > adjustRangeCap) {
            rangeX = Math.max(1, Math.min(rangeX, adjustRangeCap));
            rangeY = Math.max(1, Math.min(rangeY, adjustRangeCap));
            rangeZ = Math.max(1, Math.min(rangeZ, adjustRangeCap));
            markDirty("rangeX");
            markDirty("rangeY");
            markDirty("rangeZ");
        }
        markDirty("adjustRangeCap");

        // ── Sync face maps to arrays for client (server-authoritative mirror) ──
        // int[] 元素变更不触发 @DescSynced — 变化时全量推送 6 面 × 3 类型。
        if (rebuildFaceData()) {
            syncAllFacesToClients();
        }
    }

    /** 能量消耗滑动窗口长度（tick）：信息栏「当前能量消耗」按此取平均。 */
    private static final int ENERGY_USE_WINDOW = 5;
    private final int[] energyUseSamples = new int[ENERGY_USE_WINDOW];
    private int energyUseSampleIndex = 0;
    private int energyUseThisTick = 0;

    /**
     * 记账本 tick 实际扣掉的能量（处理型/效果型机器在成功提取后调用）。
     * 不记账即视为本 tick 未消耗（停机、缺能、缺料），会随窗口滑出而归零。
     */
    protected void recordEnergyUse(int fe) {
        if (fe > 0) {
            energyUseThisTick += fe;
        }
    }

    /** 收口上一 tick 的采样、推进环形缓冲，返回窗口平均 FE/t。 */
    private int rollEnergyUseSample() {
        energyUseSamples[energyUseSampleIndex] = energyUseThisTick;
        energyUseSampleIndex = (energyUseSampleIndex + 1) % ENERGY_USE_WINDOW;
        energyUseThisTick = 0;
        long sum = 0;
        for (int sample : energyUseSamples) {
            sum += sample;
        }
        return (int) Math.min(Integer.MAX_VALUE, sum / ENERGY_USE_WINDOW);
    }

    // Slots
    public IngredientType slotType(int slot) {
        return IngredientType.IGNORE;
    }

    public boolean valid(int slot, ItemStack stack) {
        return true;
    }

    /**
     * 是否启用「同角色槽位内同一物品唯一」。电池仓（放电/充电两槽可同电池）、
     * 频道共享存储（同物品跨槽聚合）这类合法占用多槽的机器覆写返回 false。
     */
    protected boolean enforcesUniqueItemSlots() {
        return true;
    }

    /**
     * 插入校验总入口：子类 {@link #valid(int, ItemStack)} 钩子 + 同角色去重。
     * item handler 的 validator、面 capability、GUI mayPlace 都走这里；
     * 机器自身产出走 {@code setStackInSlot}/{@code grow}，不经此门，产出不受去重影响。
     * <p>
     * 去重按槽位角色分组：可插入的槽之间互斥、可抽出的槽之间互斥，输入与输出互不影响
     * ——跨组互斥会让翻倍配方（输入=输出同物品）被机器自己的产物堵死输入槽。
     * 只比物品本体、不比 components：配方原料匹配同样只按物品
     * （{@code FormsCombinedIngredient#contains}），按 components 放过等于继续让
     * 感应炉那种严格匹配的机器「占用槽数 &gt; 原料种类数」而不出配方。
     */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!valid(slot, stack)) return false;
        if (!enforcesUniqueItemSlots() || stack.isEmpty()) return true;
        var handler = itemHandler;
        if (handler == null) return true; // 子类 slotType 会读 handler，初始化顺序上先兜住
        IngredientType type = slotType(slot);
        if (!type.canIn() && !type.canOut()) return true;
        boolean inputGroup = type.canIn();
        for (int i = 0; i < handler.getSlots(); i++) {
            if (i == slot) continue;
            IngredientType other = slotType(i);
            if (inputGroup ? !other.canIn() : !other.canOut()) continue;
            if (ItemStack.isSameItem(handler.getStackInSlot(i), stack)) return false;
        }
        return true;
    }

    public boolean validUpgrade(int slot, ItemStack stack) {
        // Slots 0..MAX_UPGRADE_SLOTS-1 (0..5) are always valid for compatible upgrades
        if (!supportsUpgradeSlots()) return false;
        if (slot < 0 || slot >= MAX_UPGRADE_SLOTS) return false;
        if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) return false;
        // LevelupSyn: max 1 per machine (enforced at install time)
        if (stack.getItem() instanceof LevelupSyn && hasUpgrade(LevelupSyn.class)) return false;
        // Blast ↔ Smoke 互斥——不可共存
        if (stack.getItem() instanceof LevelupBlast && hasUpgrade(LevelupSmoke.class)) return false;
        if (stack.getItem() instanceof LevelupSmoke && hasUpgrade(LevelupBlast.class)) return false;
        return upgradeItem.canApply(this);
    }

    public IngredientType tankType(int tank) {
        return IngredientType.IGNORE;
    }

    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    protected void resetUpgradeEffects() {
        efficientIn = initialEfficientIn;
        maxStorageEnergy = initialEnergyStorage;
        upgradeSize = MAX_UPGRADE_SLOTS;
        // 重置乘法模型字段
        durationMultiplier = 1.0;
        powerMultiplier = 1.0;
        batch = 0;
        adjustRangeCap = ADJUST_RANGE_MAX;
        photosynInstalled = false;
        recipeMode = IUpgradableMachine.RECIPE_MODE_SMELTING;
        unlimitedEnergyTransfer = false;
        // NOTE: lockedB and lockedMaxProgress are NOT reset here —
        // batch/duration lock lifecycle is managed independently by
        // conditionStart/clearLockedBatch/lockBatchForNewOperation.
    }

    protected void applyUpgradeEffects() {
        if (upgradeHandler == null) return;
        for (int i = 0; i < upgradeHandler.getSlots(); i++) {
            ItemStack stack = upgradeHandler.getStackInSlot(i);
            if (stack.getItem() instanceof UpgradeItem upgradeItem) {
                // Skip incompatible upgrades: canApply must pass first
                if (!upgradeItem.canApply(this)) continue;
                upgradeItem.effect(this);
            }
        }
        upgradeSize = MAX_UPGRADE_SLOTS;
        // Single computation after all upgrades: base FE/t with power multiplier
        // This avoids per-slot repeated rounding and per-tick re-multiplication drift.
        efficientIn = Math.max(1, (int) Math.round(initialEfficientIn * powerMultiplier));
    }

    // ───── 乘法模型 API 实现（IUpgradableMachine）─────

    @Override
    public void applyDurationMultiplier(double factor) {
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("Invalid duration multiplier: " + factor);
        }
        durationMultiplier *= factor;
    }

    @Override
    public void applyPowerMultiplier(double factor) {
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("Invalid power multiplier: " + factor);
        }
        powerMultiplier *= factor;
    }

    @Override
    public void applyBatchIncrease(int increase) {
        if (increase < 0) {
            throw new IllegalArgumentException("Batch increase must be non-negative: " + increase);
        }
        batch += increase;
        // Cap Σbatch_i at 18 so B_theory max is 19 (6×Shulker = 18)
        if (batch > 18) batch = 18;
    }

    @Override
    public void applyPhotosyn() {
        if (photosynInstalled) return; // Safe idempotency — already installed
        photosynInstalled = true;
    }

    @Override
    public void setRecipeMode(int mode) {
        // First-wins 语义——仅允许从 SMELTING 切出
        if (this.recipeMode == IUpgradableMachine.RECIPE_MODE_SMELTING) {
            this.recipeMode = mode;
        }
    }

    @Override
    public int getRecipeMode() {
        return this.recipeMode;
    }

    @Override
    public void setUnlimitedEnergyTransfer(boolean unlimited) {
        this.unlimitedEnergyTransfer = unlimited;
    }

    @Override
    public boolean hasUnlimitedEnergyTransfer() {
        return this.unlimitedEnergyTransfer;
    }

    public int getUnlockedUpgradeSlots() {
        return MAX_UPGRADE_SLOTS;
    }

    // Capability access
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (energyStorage == null) return null;
        return new IEnergyStorage() {

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if (!signalAllowRun() || !canReceiveEnergy(side)) return 0;
                // 速率不限流：storage 的 maxReceive 恒为 MAX_VALUE，实际由空余容量封顶
                return energyStorage.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if (!canExternalExtract()) return 0;
                if (!signalAllowRun() || !canExtractEnergy(side)) return 0;
                return energyStorage.extractEnergy(maxExtract, simulate);
            }

            @Override
            public int getEnergyStored() {
                return signalAllowRun() || side == null ? energyStorage.getEnergyStored() : 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return maxStorageEnergy;
            }

            @Override
            public boolean canExtract() {
                return canExternalExtract() && canExtractEnergy(side);
            }

            @Override
            public boolean canReceive() {
                return canReceiveEnergy(side);
            }
        };
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (itemHandler == null) return null;
        if (side == null) return itemHandler;
        return new IItemHandler() {

            @Override
            public int getSlots() {
                return itemHandler.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                // 观察读必须真实：外部设备（AE2 样板供应器阻挡模式逐槽 getStackInSlot 判定上批是否
                // 已消费完、存储总线、比较器、漏斗等）据此获知机器内容，只看不取。抽取权限由
                // extractItem 单独门控——若此处按 canExtractItem 藏空，主动输出(OUT)面（机器自推、
                // 不开放外部抽取）会被读成空容器，供应器阻挡模式因此永远判定目标已空、连发配方。
                return itemHandler.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!signalAllowRun() || !canReceiveItem(side)) return stack;
                if (!slotType(slot).canIn() || !valid(slot, stack)) return stack;
                return itemHandler.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!signalAllowRun() || !canExtractItem(side) || !slotType(slot).canOut()) return ItemStack.EMPTY;
                // 速率不限流：只按面权限与槽位类型门控，数量由调用方决定
                return itemHandler.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return itemHandler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slotType(slot).canIn() && valid(slot, stack) && itemHandler.isItemValid(slot, stack);
            }
        };
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (combinedFluidHandler == null && !tanks.isEmpty()) {
            combinedFluidHandler = createCombinedFluidHandler();
        }
        if (combinedFluidHandler == null) return null;
        if (side == null) return combinedFluidHandler;
        return new IFluidHandler() {

            @Override
            public int getTanks() {
                return combinedFluidHandler.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                // 同物品槽：观察读不设面权限门控（否则主动输出面在外部读取侧表现为空罐），
                // 抽取权限由 drain 单独门控
                return combinedFluidHandler.getFluidInTank(tank);
            }

            @Override
            public int getTankCapacity(int tank) {
                return combinedFluidHandler.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tankType(tank).canIn() && valid(tank, stack) && combinedFluidHandler.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!signalAllowRun() || !canReceiveFluid(side)) return 0;
                return fillFluidRange(resource, action, true);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (!signalAllowRun() || !canExtractFluid(side)) return FluidStack.EMPTY;
                return drainFluid(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (!signalAllowRun() || !canExtractFluid(side)) return FluidStack.EMPTY;
                return drainFluid(maxDrain, action);
            }
        };
    }

    // ───── NBT ─────

    /** 升级槽内容变化：同步客户端（RPC 广播）+ 存档通知。 */
    private void onUpgradeChanged() {
        syncUpgradesToClients();
        markDirty();
    }

    /**
     * 服务端：全量推送 6 升级槽到追踪客户端（对齐 syncAllFacesToClients 的 RPC 模式）。
     * 背景：客户端 BE 的 readTileData 恒收到空 tag（探针实证，LDLib2 接管同步只走 @DescSynced/RPC），
     * writeTileData 数据不经更新包到客户端，升级槽（非 @DescSynced 引用 handler）客户端恒空——
     * 故用已验证的 rpcToTracking 通道推送 ItemStack（RPCMethodMeta 经 AccessorRegistries 序列化，支持 ItemStack）。
     */
    private void syncUpgradesToClients() {
        if (level == null || level.isClientSide() || upgradeHandler == null) {
            return;
        }
        int slots = upgradeHandler.getSlots();
        rpcToTracking("rpcSyncUpgradeData",
                upgradeHandler.getStackInSlot(0), upgradeHandler.getStackInSlot(1),
                upgradeHandler.getStackInSlot(2), upgradeHandler.getStackInSlot(3),
                upgradeHandler.getStackInSlot(4), slots > 5 ? upgradeHandler.getStackInSlot(5) : ItemStack.EMPTY);
    }

    /** C→S：GUI 打开时客户端请求全量面配置（服务端逐面单播 rpcSyncFaceInfo）。 */
    @RPCMethod
    public void rpcRequestFaceSync(RPCSender sender) {
        if (sender.isRemote() && sender.asPlayer() != null) {
            for (Direction d : Direction.values()) {
                int idx = d.get3DDataValue();
                rpcToPlayer(sender.asPlayer(), "rpcSyncFaceInfo", idx,
                        energyFaceData[idx], itemFaceData[idx], fluidFaceData[idx]);
            }
        }
    }

    /** C→S：升级面板打开时客户端请求全量升级数据（服务端回复单播）。 */
    @RPCMethod
    public void rpcRequestUpgradeData(RPCSender sender) {
        if (sender.isRemote() && sender.asPlayer() != null) {
            syncUpgradesToClients();
        }
    }

    /**
     * C→S：上报升级槽面板展开态（open 非 0 为展开）。quickMoveStack 在服务端执行，
     * 读不到客户端 UIState；服务端容器据此决定升级槽是否参与快捷移动。
     * 参数沿用项目既有 RPC 的 int 写法，避免运行期参数序列化对 boolean 的隐式依赖。
     */
    @RPCMethod
    public void rpcSetUpgradePanelOpen(RPCSender sender, int open) {
        if (!sender.isRemote() || sender.asPlayer() == null) {
            return;
        }
        setUpgradePanelOpenFor(sender.asPlayer(), open != 0);
    }

    /** 记录该玩家的升级槽面板展开态。RPC 入口与回归测试共用同一处状态写入。 */
    public void setUpgradePanelOpenFor(Player player, boolean open) {
        if (player == null) {
            return;
        }
        if (open) {
            upgradePanelOpenPlayers.add(player.getUUID());
        } else {
            upgradePanelOpenPlayers.remove(player.getUUID());
        }
    }

    /** 服务端容器判定：该玩家的升级槽面板是否展开（决定升级槽能否接收快捷移动）。 */
    public boolean isUpgradePanelOpenFor(Player player) {
        return player != null && upgradePanelOpenPlayers.contains(player.getUUID());
    }

    /** S→C：客户端写回 upgradeHandler，使 GUI 升级槽（SlotItemHandler 绑定客户端 handler）渲染物品。 */
    @RPCMethod
    public void rpcSyncUpgradeData(RPCSender sender, ItemStack s0, ItemStack s1, ItemStack s2,
                                   ItemStack s3, ItemStack s4, ItemStack s5) {
        if (!sender.isServer() || upgradeHandler == null) {
            return;
        }
        // setStackInSlot 会触发 onContentsChanged → onUpgradeChanged → 服务端广播；
        // 客户端 markDirty 有 !isClientSide 守卫，syncUpgradesToClients 有 isClientSide 守卫，无回声环。
        ItemStack[] stacks = { s0, s1, s2, s3, s4, s5 };
        for (int i = 0; i < Math.min(6, upgradeHandler.getSlots()); i++) {
            if (!ItemStack.matches(upgradeHandler.getStackInSlot(i), stacks[i])) {
                upgradeHandler.setStackInSlot(i, stacks[i] == null ? ItemStack.EMPTY : stacks[i]);
            }
        }
    }

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        if (energyStorage != null) energyStorage.setEnergy(tag.getInt("energy"));
        if (itemHandler != null) {
            CompoundTag invTag = tag.getCompound("inventory");
            int savedSize = invTag.contains("Size") ? invTag.getInt("Size") : 0;
            if (!invTag.isEmpty() && savedSize == itemHandler.getSlots()) {
                itemHandler.deserializeNBT(registries, invTag);
            } else if (!invTag.isEmpty() && savedSize > 0 && savedSize < itemHandler.getSlots()) {
                // 扩容迁移（如 MobRip 1→13 槽）：旧档逐槽读入前 savedSize 槽，
                // 不直接 deserializeNBT——NeoForge 按旧 Size 重建数组会缩回旧槽数复发越界。
                var list = invTag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = 0; i < savedSize && i < list.size(); i++) {
                    itemHandler.setStackInSlot(i, MachineItemHandler.readStack(list.getCompound(i), registries));
                }
            }
        }
        if (upgradeHandler != null) {
            // 空 tag 守卫：LDLib2 GUI 同步以空 tag 反复触发 readTileData，deserialize 空 tag
            // 会清空客户端升级数据（RPC rpcSyncUpgradeData 刚推送的物品立即被抹掉，探针实证
            // 21:07:58.324 推送生效 / .346 被清）；对齐上方 itemHandler 的 !invTag.isEmpty() 守卫
            CompoundTag upgTag = tag.getCompound("upgrades");
            if (!upgTag.isEmpty()) {
                upgradeHandler.deserializeNBT(registries, upgTag);
            }
        }
        upgradeSize = tag.contains("upgrade_size") ? tag.getInt("upgrade_size") : initialUpgradeSize;
        // 客户端空同步守卫：LDLib2 GUI 同步以空 tag 反复触发 readTileData，faceMode maps 缺键读 0
        // 会重置 maps 并经 rebuildFaceData 抹掉 RPC 推送的真实 faceData（面配置按钮调整后闪烁显示
        // 应有状态又回退默认——与升级槽 deserialize 空 tag 清空同构）；writeTileData 恒写全部面配置，
        // 仅当 tag 含面配置键（真实存档/更新包）时才应用
        boolean hasFaceConfig = tag.contains("direEnergy" + Direction.NORTH.get3DDataValue());
        // level null-safe：loadAdditional（存档加载）阶段 Minecraft 尚未 setLevel（level==null），
        // 原直接解引用会在每次存档加载时 NPE → MC 吞错并 skip 整批 BE（数据丢失）→
        // 下游 LDLib2 ReadOnlyManagedRef 读 null 硬崩。语义：loadAdditional 时 level 为 null
        // 但必为真实存档（应用分支）；GUI 空同步仅发生在已在世界的 BE（level 非空）。
        boolean clientSide = level != null && level.isClientSide();
        if (!clientSide || hasFaceConfig) {
            for (Direction direction : Direction.values()) {
                energyFaceMode.put(direction, tag.getInt("direEnergy" + direction.get3DDataValue()));
                itemFaceMode.put(direction, tag.getInt("direItem" + direction.get3DDataValue()));
                fluidFaceMode.put(direction, tag.getInt("direFluid" + direction.get3DDataValue()));
            }
            // 扳手拆卸的物品会带着旧朝向的面配置重新放置。面配置按世界方向存储，
            // 若不按「旧朝向 → 新朝向」旋转，玩家换个方向摆放后面语义就整体错位。
            // 只跑一次：同一份 tag 若被重复应用，二次旋转会把配置拧回去。
            if (!faceRotationApplied && tag.contains("facingVal")) {
                faceRotationApplied = true;
                Direction savedFacing = Direction.from3DDataValue(tag.getInt("facingVal"));
                Direction currentFacing = getFacing();
                if (savedFacing != currentFacing) {
                    rotateFaceModes(savedFacing, currentFacing);
                }
            }
            // 旧档面语义迁移（仅服务端、仅真正读档）：逐级补齐，每级只跑一次。
            // 1) 旧版 BOTH 兼作「机器主动推送」→ 产能类机器的能量 BOTH 面改写为 OUT。
            // 2) 新版能量「向外」档位只给产能类 → 耗能机器的 BOTH/OUT 保留有效半边 BE_IN。
            if (!clientSide) {
                int semantics = tag.getInt(FACE_SEMANTICS_KEY);
                if (semantics < FACE_SEMANTICS_PASSIVE && canExternalExtract()) {
                    for (Direction direction : Direction.values()) {
                        if (energyFaceMode.getOrDefault(direction, FaceOption.OFF) == FaceOption.BOTH) {
                            energyFaceMode.put(direction, FaceOption.OUT);
                        }
                    }
                }
                if (semantics < FACE_SEMANTICS_ENERGY_DIRECTIONS && migratesEnergyOutputToPassiveInput()) {
                    for (Direction direction : Direction.values()) {
                        int mode = energyFaceMode.getOrDefault(direction, FaceOption.OFF);
                        if (mode == FaceOption.BOTH || mode == FaceOption.OUT) {
                            energyFaceMode.put(direction, FaceOption.BE_IN);
                        }
                    }
                }
            }
            // 读档后立即重建 faceData 镜像，服务端状态即时正确
            rebuildFaceData();
        }
        loadSerializedHandlers(tag, registries);

        // ── 旧档兼容：读档时无条件清零进度与运行锁 ──
        // 旧 NBT 存的 progress 语义不明（旧版存累计 FE）；无条件清零最安全，
        // 下周期 conditionStart() 会重建基于 tick 的值。库存/能量/升级/面配置均保留。
        progress = 0;
        maxProgress = 0;
        lockedB = 0;
        lockedMaxProgress = 0;
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        if (energyStorage != null) tag.putInt("energy", energyStorage.getEnergyStored());
        if (itemHandler != null) tag.put("inventory", itemHandler.serializeNBT(registries));
        if (upgradeHandler != null) tag.put("upgrades", upgradeHandler.serializeNBT(registries));
        tag.putInt("upgrade_size", upgradeSize);
        for (Direction direction : Direction.values()) {
            tag.putInt("direEnergy" + direction.get3DDataValue(), energyFaceMode.getOrDefault(direction, initialFaceModeEnergy()));
            tag.putInt("direItem" + direction.get3DDataValue(), itemFaceMode.getOrDefault(direction, initialFaceModeItem()));
            tag.putInt("direFluid" + direction.get3DDataValue(), fluidFaceMode.getOrDefault(direction, initialFaceModeFluid()));
        }
        // 写入即视为已迁移：下次读档不再触发面语义改写
        tag.putInt(FACE_SEMANTICS_KEY, FACE_SEMANTICS_ENERGY_DIRECTIONS);
        saveSerializedHandlers(tag, registries);
    }

    // ───── @RPCMethod: 替换自定义网络包 ─────
    /** C→S: 切换红石模式 */
    @RPCMethod
    public void rpcSetRedstoneMode(RPCSender sender, int mode) {
        // C→S 包在服务端执行时 sender 为 ofClient(player)，仅 isRemote() 为 true
        if (sender.isRemote()) {
            redstoneMode = mode;
            setChanged();
            // faceData 同步由 doBaseData 的 rebuildFaceData() 变更检测覆盖，不在此重复推送
        }
    }

    private static boolean isValidFaceIndex(int dirIndex) {
        return dirIndex >= 0 && dirIndex < 6;
    }

    /** C→S: 切换面配置 */
    @RPCMethod
    public void rpcCycleFaceMode(RPCSender sender, int changeType, int dirIndex) {
        if (sender.isRemote() && isValidFaceIndex(dirIndex)) {
            Direction direction = Direction.from3DDataValue(dirIndex);
            Map<Direction, Integer> map;
            switch (changeType) {
                case FaceOption.TYPE_ENERGY -> map = energyFaceMode;
                case FaceOption.TYPE_ITEM -> map = itemFaceMode;
                case FaceOption.TYPE_FLUID -> map = fluidFaceMode;
                default -> {
                    return;
                }
            }
            int current = map.getOrDefault(direction, FaceOption.OFF);
            // 可选范围由方向级 + 类型级两层判定给出（该面是不是入口 → 设备功能性与标签）：
            // 跳过的模式点了不生效；频道只有朝向面能配，其余五面点了只会落回关闭。
            int mode = FaceOption.nextAllowed(current, candidate -> allowsFaceMode(direction, changeType, candidate));
            if (mode == current) {
                // 该页只有当前模式可选（资源不存在 → 只剩关闭），点击视为无操作
                return;
            }
            map.put(direction, mode);
            setChanged();
            // 全量同步（int[] 元素变更不触发 @DescSynced；单面推送会导致其他面/类型保持旧值）
            rebuildFaceData();
            syncAllFacesToClients();
        }
    }

    /** S→C: 同步面配置到客户端 */
    @RPCMethod
    public void rpcSyncFaceInfo(RPCSender sender, int dirIndex, int energyMode, int itemMode, int fluidMode) {
        // S→C 包在客户端执行时 sender 为 ofServer()（isServer()=true）；校验 dirIndex 防越界写数组
        if (sender.isServer() && isValidFaceIndex(dirIndex)) {
            energyFaceData[dirIndex] = energyMode;
            itemFaceData[dirIndex] = itemMode;
            fluidFaceData[dirIndex] = fluidMode;
            Direction d = Direction.from3DDataValue(dirIndex);
            // 同步 maps：客户端 rebuildFaceData 以 maps 为源重算，只写 faceData 不写 maps
            // 会在后续 rebuild 时被回滚（回退默认按钮状态）
            energyFaceMode.put(d, energyMode);
            itemFaceMode.put(d, itemMode);
            fluidFaceMode.put(d, fluidMode);
        }
    }

    // ───── 面配置同步 ─────

    /**
     * 从 faceMode maps 重建客户端镜像 faceData（3 类型 × 6 面）。
     * 返回是否发生变化（供调用方决定是否推送客户端）。
     */
    private boolean rebuildFaceData() {
        boolean changed = false;
        for (Direction d : Direction.values()) {
            int idx = d.get3DDataValue();
            int e = energyFaceMode.getOrDefault(d, initialFaceModeEnergy());
            int i = itemFaceMode.getOrDefault(d, initialFaceModeItem());
            int f = fluidFaceMode.getOrDefault(d, initialFaceModeFluid());
            if (energyFaceData[idx] != e || itemFaceData[idx] != i || fluidFaceData[idx] != f) {
                changed = true;
            }
            energyFaceData[idx] = e;
            itemFaceData[idx] = i;
            fluidFaceData[idx] = f;
        }
        return changed;
    }

    /**
     * 全量推送 6 面 × 3 类型的 faceData 到追踪客户端。
     * 仅服务端执行（客户端 createUI 重建 menu 时无 server level）。
     */
    private void syncAllFacesToClients() {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (Direction d : Direction.values()) {
            int idx = d.get3DDataValue();
            rpcToTracking("rpcSyncFaceInfo", idx, energyFaceData[idx], itemFaceData[idx], fluidFaceData[idx]);
        }
    }

    // ───── 主动 IO（仅 IN 自拉、仅 OUT 自推；被动面一律等外部操作；无速率上限，每次尽力搬空）─────

    /**
     * 机器主动物品 IO：仅 IN 面自拉、仅 OUT 面自推。
     *
     * <p>
     * 被动面（BE_IN / BE_OUT / BOTH）机器本 tick 完全不动作：这些面只开放给外部设备经
     * capability 读写，机器再插手就是两条搬运路径抢同一个槽。
     */
    protected void doActiveItemIo() {
        if (level == null || level.isClientSide() || itemHandler == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            int mode = itemFaceMode.getOrDefault(direction, FaceOption.OFF);
            if (FaceOption.machinePulls(mode)) {
                activePullItems(direction);
            } else if (FaceOption.machinePushes(mode)) {
                activePushItems(direction);
            }
        }
    }

    /** 主动拉取：从相邻容器（非管道）拉物品到输入槽，尽力搬空源槽（无速率上限）。 */
    private void activePullItems(Direction direction) {
        BlockPos sourcePos = worldPosition.relative(direction);
        if (level.getBlockEntity(sourcePos) instanceof PipeBlockEntity) {
            return; // 相邻为管道：由管道逐级传递处理，机器不主动跨管道拉
        }
        IItemHandler source = TransferNetworks.getItems(level, sourcePos, direction.getOpposite());
        if (source == null) {
            return;
        }
        int remaining = Integer.MAX_VALUE; // 无速率上限：剩余预算取最大，每次拉取到源槽搬空/输入槽满为止
        for (int srcSlot = 0; srcSlot < source.getSlots() && remaining > 0; srcSlot++) {
            ItemStack simulated = source.extractItem(srcSlot, remaining, true);
            if (simulated.isEmpty()) {
                continue;
            }
            // simulate 确认输入槽可接收
            ItemStack simLeft = insertIntoInputSlots(simulated.copy(), true);
            int accepted = simulated.getCount() - simLeft.getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack extracted = source.extractItem(srcSlot, accepted, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack leftover = insertIntoInputSlots(extracted.copy(), false);
            if (!leftover.isEmpty()) {
                // 防御竞态：真实插入失败退回源
                TransferNetworks.insertItem(source, leftover, false);
            }
            remaining -= accepted;
        }
    }

    /**
     * 主动推出：从输出槽推物品到相邻容器（非管道），尽力搬空输出槽（无速率上限）。
     * <p>
     * 记账口径：每次都以 capability 的实际返回值为准。先模拟提取拿到「本次真的能取出多少」
     * （超堆叠槽单次提取量可能小于槽内数量），再模拟插入拿到目标能收多少，随后才执行
     * 提取与插入；目标实收 = 机器实扣，绝不按「意欲推送量」记账——否则超堆叠槽会复制物品。
     */
    private void activePushItems(Direction direction) {
        BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof PipeBlockEntity) {
            return; // 相邻为管道：由管道逐级传递处理
        }
        IItemHandler sink = TransferNetworks.getItems(level, targetPos, direction.getOpposite());
        if (sink == null) {
            return;
        }
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            if (!slotType(slot).canOut()) {
                continue; // 仅输出槽
            }
            // 同一 tick 内循环搬空该槽，直到槽空或目标拒收（保持"无速率上限"语义）
            while (true) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    break;
                }
                int countBefore = stack.getCount();
                ItemStack simExtract = itemHandler.extractItem(slot, countBefore, true);
                if (simExtract.isEmpty()) {
                    break; // 该槽当前不可提取
                }
                ItemStack simLeft = TransferNetworks.insertItem(sink, simExtract.copy(), true);
                int acceptable = simExtract.getCount() - simLeft.getCount();
                if (acceptable <= 0) {
                    break; // 目标拒收或已满：物品留在输出槽
                }
                ItemStack extracted = itemHandler.extractItem(slot, acceptable, false);
                if (extracted.isEmpty()) {
                    break; // 防御：模拟与实际不一致时放弃本槽，不做无依据的扣减
                }
                ItemStack leftover = TransferNetworks.insertItem(sink, extracted.copy(), false);
                if (!leftover.isEmpty()) {
                    // 防御竞态：真实插入失败的部分原样退回本槽（直接合并，绕过堆叠上限——
                    // 这些物品刚从该槽取出，槽内必然容得下），不凭空消失
                    ItemStack current = itemHandler.getStackInSlot(slot);
                    if (current.isEmpty()) {
                        itemHandler.setStackInSlot(slot, leftover);
                    } else if (ItemStack.isSameItemSameComponents(current, leftover)) {
                        current.grow(leftover.getCount());
                        itemHandler.setStackInSlot(slot, current);
                    } else {
                        itemHandler.insertItem(slot, leftover, false);
                    }
                    break;
                }
                if (itemHandler.getStackInSlot(slot).getCount() >= countBefore) {
                    break; // 防御：提取未生效（异常 capability）时终止，避免死循环
                }
            }
        }
    }

    /** 尝试将物品插入机器输入槽（slotType canIn），返回剩余。 */
    private ItemStack insertIntoInputSlots(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < itemHandler.getSlots() && !remaining.isEmpty(); slot++) {
            if (!slotType(slot).canIn()) {
                continue;
            }
            remaining = itemHandler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    /**
     * 机器主动流体 IO：IN 面自拉（相邻 → 输入罐），OUT 面自推（输出罐 → 相邻），无速率上限。
     *
     * <p>
     * 被动面（BE_OUT / BOTH）机器不动作——{@code BOTH} 的含义是「既可被输入也可被输出」，
     * 不是「双向自动传输」。相邻为管道时跳过（由管道逐级传递处理，同物品/能量口径；
     * 本模组没有流体管道，判定为防御性对齐）。
     */
    protected void doActiveFluidIo() {
        if (level == null || level.isClientSide() || tanks.isEmpty()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            int mode = fluidFaceMode.getOrDefault(direction, initialFaceModeFluid());
            if (FaceOption.machinePulls(mode)) {
                activePullFluid(direction);
            } else if (FaceOption.machinePushes(mode)) {
                activePushFluid(direction);
            }
        }
    }

    /** 主动推：逐输出罐搬进相邻容器。 */
    private void activePushFluid(Direction direction) {
        if (!hasFaceCapabilityFluid(direction)) {
            return;
        }
        if (level.getBlockEntity(worldPosition.relative(direction)) instanceof PipeBlockEntity) {
            return; // 相邻为管道：由管道逐级传递处理
        }
        IFluidHandler sink = TransferNetworks.getFluids(level, worldPosition.relative(direction), direction.getOpposite());
        if (sink == null) {
            return;
        }
        // 逐输出罐推送，源取罐本身：combinedFluidHandler 恒定从首个可输出罐出液，
        // 罐 0 的流体被相邻拒收时后面的罐会被全部堵死（两罐装不同流体时必现）。
        // 不经 getFluidHandler(side)：主动输出面会被 canExtractFluid（只放行被动面）挡死。
        for (int tank = 0; tank < tanks.size(); tank++) {
            if (!tankType(tank).canOut()) {
                continue; // 仅输出罐
            }
            pushFluidTank(tanks.get(tank), sink);
        }
    }

    /**
     * 主动拉：把相邻容器的流体搬进本机可输入罐（IN 面）。
     * <p>
     * 与物品侧同一记账口径：模拟抽 → 模拟灌 → 实抽 → 实灌；实灌不足的部分倒回源，
     * 不以「想抽多少」记账。相邻方块必须自己暴露 {@code IFluidHandler}（容器、接口、
     * 储罐皆可），本方法不按方块类型做特判。
     */
    private void activePullFluid(Direction direction) {
        if (!hasFaceCapabilityFluid(direction)) {
            return;
        }
        BlockPos sourcePos = worldPosition.relative(direction);
        if (level.getBlockEntity(sourcePos) instanceof PipeBlockEntity) {
            return;
        }
        IFluidHandler source = TransferNetworks.getFluids(level, sourcePos, direction.getOpposite());
        if (source == null) {
            return;
        }
        for (int srcTank = 0; srcTank < source.getTanks(); srcTank++) {
            FluidStack inTank = source.getFluidInTank(srcTank);
            if (inTank.isEmpty()) {
                continue;
            }
            FluidStack simulated = source.drain(inTank.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }
            int acceptable = fillFluidRange(simulated.copy(), IFluidHandler.FluidAction.SIMULATE, true);
            if (acceptable <= 0) {
                continue;
            }
            FluidStack toDrain = simulated.copy();
            toDrain.setAmount(acceptable);
            FluidStack drained = source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            int filled = fillFluidRange(drained.copy(), IFluidHandler.FluidAction.EXECUTE, true);
            if (filled < drained.getAmount()) {
                // 防御竞态：实灌不足的部分原样退回源，不蒸发
                FluidStack back = drained.copy();
                back.setAmount(drained.getAmount() - filled);
                source.fill(back, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /**
     * 把单个罐搬进相邻容器：同 tick 循环到罐空或对方拒收（无速率上限，同物品口径）。
     * <p>
     * 全程按 capability 的实际返回值记账：先模拟拿「对方能收多少」，再按此量实抽、实灌；
     * 灌入阶段实收小于实抽的部分倒回本罐，流体不凭空消失。
     * <p>
     * 子类复用：频道方块的主动输出把共享罐逐个交给本方法，记账口径与机器主动输出一致。
     */
    protected static void pushFluidTank(IFluidHandler source, IFluidHandler sink) {
        while (true) {
            FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty()) {
                return;
            }
            int acceptable = sink.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (acceptable <= 0) {
                return; // 目标拒收或已满：留在罐里
            }
            FluidStack extracted = source.drain(acceptable, IFluidHandler.FluidAction.EXECUTE);
            if (extracted.isEmpty()) {
                return; // 防御：模拟与执行不一致时放弃本罐，不做无依据的扣减
            }
            int filled = sink.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
            if (filled < extracted.getAmount()) {
                // 防御竞态：未被接收的部分倒回本罐，不凭空消失
                source.fill(extracted.copyWithAmount(extracted.getAmount() - filled),
                        IFluidHandler.FluidAction.EXECUTE);
                return;
            }
        }
    }

    /**
     * 机器主动能量 IO：IN 面自拉（相邻 → 本机储能），OUT 面自推（本机储能 → 相邻）。
     *
     * <p>
     * 推只在产能类机器（{@link #canExternalExtract()} 为 true，沿用旧名：这类机器的储能本就该被取走）
     * 生效——普通机器即使把面设成主动输出也不会往外送电，避免耗能设备把输入电倒灌回网络。
     * 拉对所有带储能、且该面暴露能量能力的机器生效（耗能设备的供电方式之一）。
     * 被动面（BE_OUT / BOTH）机器不动作，交给外部设备抽取/塞入。
     */
    protected void doActiveEnergyIo() {
        if (level == null || level.isClientSide() || energyStorage == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            int mode = energyFaceMode.getOrDefault(direction, initialFaceModeEnergy());
            if (FaceOption.machinePulls(mode)) {
                activePullEnergy(direction);
            } else if (FaceOption.machinePushes(mode) && canExternalExtract()) {
                activePushEnergy(direction);
            }
        }
    }

    /** 主动推：把本机储能送给相邻接收方（速率不限流，接收方上限封顶）。 */
    private void activePushEnergy(Direction direction) {
        // 不按 hasFaceCapabilityEnergy 过滤：引擎类只在 UP 面暴露能量能力，但旧行为允许
        // 侧面主动输出把电推给相邻机器；收紧会静默改变既有存档的供电方式。
        IEnergyStorage sink = TransferNetworks.getEnergy(level, worldPosition.relative(direction), direction.getOpposite());
        if (sink == null || !sink.canReceive()) {
            return;
        }
        // 速率不限流：可推能量 = 当前储能，接收方上限与对面容量再封顶
        int available = energyStorage.getEnergyStored();
        if (available <= 0) {
            return;
        }
        int accepted = sink.receiveEnergy(available, false);
        if (accepted > 0) {
            energyStorage.extractEnergy(accepted, false);
        }
    }

    /**
     * 主动拉：从相邻能量源抽取到本机储能（IN 面）。
     * <p>
     * 与物品/流体侧同一记账口径：模拟抽 → 模拟收 → 实抽 → 实收，实收不足的部分退回源。
     * 相邻方块必须自己暴露 {@code IEnergyStorage}；创造能量单元等「被动输出」面正是这条路径的源。
     */
    private void activePullEnergy(Direction direction) {
        if (!hasFaceCapabilityEnergy(direction)) {
            return;
        }
        int space = maxStorageEnergy - energyStorage.getEnergyStored();
        if (space <= 0) {
            return;
        }
        IEnergyStorage source = TransferNetworks.getEnergy(level, worldPosition.relative(direction), direction.getOpposite());
        if (source == null || !source.canExtract()) {
            return;
        }
        int available = source.extractEnergy(space, true);
        if (available <= 0) {
            return;
        }
        int acceptable = energyStorage.receiveEnergy(available, true);
        if (acceptable <= 0) {
            return;
        }
        int drained = source.extractEnergy(acceptable, false);
        if (drained <= 0) {
            return;
        }
        int received = energyStorage.receiveEnergy(drained, false);
        if (received < drained) {
            // 防御竞态：实收不足的部分原样退回源（对方拒收则只能视为竞态损失）
            source.receiveEnergy(drained - received, false);
        }
    }

    // ───── UI helpers ─────
    public Component getDisplayName() {
        return component != null ? component : getBlockState().getBlock().getName();
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return TENMachineBlockUIFactory.createFallback(holder);
    }

    protected final ModularUI buildMachineUI(BlockUIMenuType.BlockUIHolder holder,
                                             ResourceLocation background,
                                             Consumer<UIElement> inventoryBuilder,
                                             Consumer<UIElement> contentBuilder) {
        initMachine();
        var root = TENMachineBlockUIFactory.createRoot(background);
        var uiState = new TENMachineBlockUIFactory.UIState(holder);
        // Machine name label at top-left
        root.addChild(new Label()
                .setText(holder.blockState.getBlock().getName())
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(6);
                    layout.top(4);
                    layout.width(0);
                    layout.height(10);
                }));
        // 升级面板先于机器槽挂进 UI 树：LDLib2 的容器槽按 UI 树遍历顺序注册，而 quickMoveStack
        // 在服务端执行、服务端 UI 树不经样式引擎计算，SlotStyle.quickMovePriority(1000) 读不到
        // computed 值会回退成默认 0。所有槽同级时按注册顺序取候选，故顺序才是「展开时先进升级槽」
        // 的可靠保障；面板与按钮都带显式 zIndex(1/2)，提前挂载不改变绘制层叠。
        if (supportsUpgradeSlots()) {
            TENMachineBlockUIFactory.addUpgradeSlotsTab(root, this, uiState, holder.player);
        }
        inventoryBuilder.accept(root);
        TENMachineBlockUIFactory.addPlayerInventory(root);
        TENMachineBlockUIFactory.addCommonSidebar(root, holder, this, uiState);
        // 打开 GUI 时面配置初始同步：改为客户端请求 → 服务端 rpcToPlayer 单播（对齐升级槽/过滤槽模式）。
        // 原 buildMachineUI 构建时 syncAllFacesToClients()（rpcToTracking）在 tracking 注册前调用
        // 推送丢失，客户端面按钮以默认值起跳；rpcToServer 需客户端守卫（专用服务器上服务端 createUI
        // 也执行，PacketDistributor.sendToServer 会抛 IllegalStateException）
        if (holder.player.level().isClientSide()) {
            rpcToServer("rpcRequestFaceSync");
        }
        contentBuilder.accept(root);
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    // ───── Serialized handlers (tanks) ─────
    protected void loadSerializedHandlers(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < tanks.size(); i++) {
            CompoundTag tankTag = tag.getCompound("tank" + i);
            if (!tankTag.isEmpty()) {
                tanks.get(i).readFromNBT(registries, tankTag);
            }
        }
    }

    protected void saveSerializedHandlers(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < tanks.size(); i++) {
            tag.put("tank" + i, tanks.get(i).writeToNBT(registries, new CompoundTag()));
        }
    }

    // ───── Fluid helpers ─────
    protected IFluidHandler createCombinedFluidHandler() {
        return new IFluidHandler() {

            @Override
            public int getTanks() {
                return tanks.size();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getFluid() : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getCapacity() : 0;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank >= 0 && tank < tanks.size() && tankType(tank).canIn() && valid(tank, stack) && tanks.get(tank).isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return fillFluidRange(resource, action, true);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return drainFluid(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return drainFluid(maxDrain, action);
            }
        };
    }

    protected int fillFluidRange(FluidStack resource, IFluidHandler.FluidAction action, boolean respectSlotType) {
        FluidStack remaining = resource.copy();
        for (int i = 0; i < tanks.size() && !remaining.isEmpty(); i++) {
            if (respectSlotType && !tankType(i).canIn()) continue;
            if (!valid(i, remaining)) continue;
            int filled = tanks.get(i).fill(remaining, action);
            remaining.shrink(filled);
        }
        return resource.getAmount() - remaining.getAmount();
    }

    protected FluidStack drainFluid(FluidStack resource, IFluidHandler.FluidAction action) {
        FluidStack drained = FluidStack.EMPTY;
        for (int i = 0; i < tanks.size(); i++) {
            if (!tankType(i).canOut()) continue;
            FluidStack current = tanks.get(i).getFluid();
            if (current.isEmpty() || !current.is(resource.getFluid())) continue;
            FluidStack piece = tanks.get(i).drain(resource.getAmount() - drained.getAmount(), action);
            if (piece.isEmpty()) continue;
            if (drained.isEmpty()) drained = piece.copy();
            else drained.grow(piece.getAmount());
            if (drained.getAmount() >= resource.getAmount()) break;
        }
        return drained;
    }

    protected FluidStack drainFluid(int maxDrain, IFluidHandler.FluidAction action) {
        FluidStack drained = FluidStack.EMPTY;
        for (int i = 0; i < tanks.size(); i++) {
            if (!tankType(i).canOut()) continue;
            FluidStack piece = tanks.get(i).drain(maxDrain - drained.getAmount(), action);
            if (piece.isEmpty()) continue;
            if (drained.isEmpty()) drained = piece.copy();
            else if (drained.is(piece.getFluid())) drained.grow(piece.getAmount());
            if (drained.getAmount() >= maxDrain) break;
        }
        return drained;
    }

    public void dropAllContents() {
        if (level == null || itemHandler == null) return;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    itemHandler.getStackInSlot(i));
        }
        if (upgradeHandler != null) {
            for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        upgradeHandler.getStackInSlot(i));
            }
        }
    }

    @Override
    public boolean onUpgradeApply(double percent, int slotIncrease) {
        efficientIn = (int) (efficientIn + initialEfficientIn * percent);
        maxStorageEnergy = (int) (maxStorageEnergy + initialEnergyStorage * percent);
        upgradeSize = Math.max(1, Math.min(upgradeSize + slotIncrease, MAX_UPGRADE_SLOTS));
        return true;
    }

    @Override
    public boolean isType(String type) {
        return switch (type) {
            case "MACHINE_PROCESS" -> machineType() == MachineType.MACHINE_PROCESS || machineType() == MachineType.FURNACE || machineType() == MachineType.PULVERIZER || machineType() == MachineType.COMPRESSOR || machineType() == MachineType.REFINER || machineType() == MachineType.INDUCTION_FURNACE || machineType() == MachineType.PSIONICANT || machineType() == MachineType.MATTER_CONDENSER || machineType() == MachineType.ENCHANTMENT_FLUSHER;
            case "MACHINE_EFFECT" -> machineType() == MachineType.MACHINE_EFFECT || machineType() == MachineType.BEACON || machineType() == MachineType.MOB_RIPPER || machineType() == MachineType.FARM || machineType() == MachineType.BLOCK_BREAKER || machineType() == MachineType.BLOCK_FORMER || machineType() == MachineType.COOLER;
            case "FURNACE" -> machineType() == MachineType.FURNACE;
            case "BEACON" -> machineType() == MachineType.BEACON;
            case "QUARRY" -> machineType() == MachineType.QUARRY;
            default -> false;
        };
    }

    @Override
    public int getCurrentRadius() {
        return 0;
    }

    @Override
    public void setCurrentRadius(int radius) {}

    @Override
    public int getInitialRadius() {
        return 0;
    }
}
