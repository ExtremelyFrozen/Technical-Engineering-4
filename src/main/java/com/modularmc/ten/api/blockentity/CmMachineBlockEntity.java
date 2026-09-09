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
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.IUpgradableMachine;
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
import java.util.function.Consumer;

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
    @Persisted
    @DescSynced
    public int energyRec = 0;       // E_REC
    @Persisted
    @DescSynced
    public int energyExt = 0;       // E_EXT
    @Persisted
    @DescSynced
    public int itemRec = 0;         // I_REC
    @Persisted
    @DescSynced
    public int itemExt = 0;         // I_EXT
    @Persisted
    @DescSynced
    public int fluidRec = 0;        // F_REC
    @Persisted
    @DescSynced
    public int fluidExt = 0;        // F_EXT
    @Persisted
    @DescSynced
    public int effAuc = 0;          // EFF_AUC
    @Persisted
    @DescSynced
    public int eff = 0;             // EFF
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
     * 持久化 + 客户端同步（@DescSynced），经 rpcToggleRangeVisible 切换（26.1.2 对齐）。
     */
    @Persisted
    @DescSynced
    public boolean rangeVisible = false;

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

    // ───── 乘法模型与批处理字段（P0-1 能量模型移植引入）─────
    /** 时长乘子（26.1.2 乘法模型；P0-4 升级系统接入，当前恒 1.0）。 */
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
     * Locked batch size B_actual（P0-1 基础字段；P0-2 批处理锁补四维计算）。
     * 0 = 未锁定（无批处理）；处理中 {@link #getLockedBatchSize()} 返回至少 1。
     */
    public int lockedB = 0;

    /**
     * Locked maxProgress（配方周期开始锁定，防 durationMultiplier 漂移）。
     * 0 = 未锁定（每周期重算）。
     */
    public int lockedMaxProgress = 0;

    /** 功率乘子（P0-4 乘法模型，升级叠加；初始 1.0）。 */
    public double powerMultiplier = 1.0;
    /** 配方模式（Blast/Smoke 升级切换；默认熔炼）。 */
    public int recipeMode = IUpgradableMachine.RECIPE_MODE_SMELTING;
    /** 无限能量传输（Stream 升级；解除 maxReceive/maxExtract 速率限制）。 */
    public boolean unlimitedEnergyTransfer = false;

    // ───── Machine fields ─────
    public MachineEnergyStorage energyStorage;
    public int maxStorageEnergy;
    public int initialEnergyStorage;
    public int maxReceiveEnergy;
    public int initialEnergyReceive;
    public int maxExtractEnergy;
    public int initialEnergyExtract;
    public int efficientIn;
    public int initialEfficientIn;

    public MachineItemHandler itemHandler;
    public MachineItemHandler upgradeHandler;
    public int maxReceiveItem;
    public int initialItemReceive;
    public int maxExtractItem;
    public int initialItemExtract;
    public int upgradeSize = 1;
    public int initialUpgradeSize = 1;
    public static final int MAX_UPGRADE_SLOTS = 6;

    public List<MachineFluidTank> tanks = new ArrayList<>();
    public int maxReceiveFluid;
    public int initialFluidReceive;
    public int maxExtractFluid;
    public int initialFluidExtract;

    public Map<Direction, Integer> energyFaceMode = new HashMap<>();
    public Map<Direction, Integer> itemFaceMode = new HashMap<>();
    public Map<Direction, Integer> fluidFaceMode = new HashMap<>();

    private boolean machineInitialised = false;
    private IFluidHandler combinedFluidHandler;

    public CmMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction d : Direction.values()) {
            energyFaceMode.put(d, FaceOption.BOTH);
            itemFaceMode.put(d, FaceOption.BOTH);
            fluidFaceMode.put(d, FaceOption.BOTH);
        }
        Arrays.fill(energyFaceData, FaceOption.BOTH);
        Arrays.fill(itemFaceData, FaceOption.BOTH);
        Arrays.fill(fluidFaceData, FaceOption.BOTH);
    }

    public abstract int inventorySize();

    public abstract int machineType();

    public static int kFE(double k) {
        return (int) (1000 * k);
    }

    public void setCapacity(int energy) {
        initialEnergyStorage = energy;
        maxStorageEnergy = energy;
        initialEnergyReceive = Math.max(energy / 200, 1);
        maxReceiveEnergy = initialEnergyReceive;
        initialEnergyExtract = Math.max(energy / 200, 1);
        maxExtractEnergy = initialEnergyExtract;
        energyStorage = new MachineEnergyStorage(energy, Math.max(energy / 100, 1), Math.max(energy / 100, 1));
        energyStorage.setChangeListener(this::markDirty);
    }

    public void setEfficiency(int eff) {
        initialEfficientIn = efficientIn = eff;
    }

    public void initMachine() {
        if (machineInitialised) return;
        machineInitialised = true;

        if (initialEnergyStorage <= 0) initialEnergyStorage = 10000;
        if (initialEnergyReceive <= 0) initialEnergyReceive = Math.max(initialEnergyStorage / 200, 1);
        if (initialEnergyExtract <= 0) initialEnergyExtract = Math.max(initialEnergyStorage / 200, 1);
        if (maxStorageEnergy <= 0) maxStorageEnergy = initialEnergyStorage;
        if (maxReceiveEnergy <= 0) maxReceiveEnergy = initialEnergyReceive;
        if (maxExtractEnergy <= 0) maxExtractEnergy = initialEnergyExtract;

        if (itemHandler == null || itemHandler.getSlots() != inventorySize()) {
            itemHandler = new MachineItemHandler(inventorySize(), this::valid);
        } else {
            itemHandler.setValidator(this::valid);
        }
        itemHandler.setChangeListener(this::markDirty);

        if (upgradeHandler == null) {
            upgradeHandler = new MachineItemHandler(MAX_UPGRADE_SLOTS, this::validUpgrade);
        } else {
            upgradeHandler.setValidator(this::validUpgrade);
        }
        upgradeHandler.setChangeListener(this::onUpgradeChanged);

        if (energyStorage == null) {
            energyStorage = new MachineEnergyStorage(maxStorageEnergy, maxReceiveEnergy, maxExtractEnergy);
        }
        energyStorage.setChangeListener(this::markDirty);

        if (maxReceiveItem <= 0) maxReceiveItem = initialItemReceive > 0 ? initialItemReceive : 8;
        if (maxExtractItem <= 0) maxExtractItem = initialItemExtract > 0 ? initialItemExtract : 8;
        initialItemReceive = maxReceiveItem;
        initialItemExtract = maxExtractItem;

        if (maxReceiveFluid <= 0) maxReceiveFluid = initialFluidReceive > 0 ? initialFluidReceive : 100;
        if (maxExtractFluid <= 0) maxExtractFluid = initialFluidExtract > 0 ? initialFluidExtract : 100;
        initialFluidReceive = maxReceiveFluid;
        initialFluidExtract = maxExtractFluid;

        for (var tank : tanks) tank.setChangeListener(this::markDirty);
        combinedFluidHandler = createCombinedFluidHandler();
    }

    // ───── ldlib2 NBT: 确保反序列化前 handler 已初始化 ─────
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        initMachine();
        super.loadAdditional(tag, registries);
    }

    /**
     * 是否支持升级槽 UI（26.1.2 对齐）：Cell/CreativeCell/Channel 等不支持，覆写返回 false。
     */
    public boolean supportsUpgradeSlots() {
        return true;
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
        // P0-4: 乘法模型——实际效率 = initialEfficientIn × powerMultiplier（升级乘子叠加）
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
     * 机器朝向：优先读方块状态的 FACING 属性（与方块实际朝向一致，权威，26.1.2 对齐）。
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

    public int initialFaceModeEnergy() {
        return FaceOption.BOTH;
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

    protected boolean canReceiveEnergy(@Nullable Direction side) {
        if (!hasFaceCapabilityEnergy(side)) return false;
        if (side == null) return true;
        int mode = energyFaceMode.getOrDefault(side, FaceOption.OFF);
        // [用户需求 2026-09] 主动输出（BE_OUT）面同时允许被动输入：外部可向该面充能
        // （引擎/单元主动推给网络的同时，也能被网络/电池反向充能）
        return FaceOption.isIn(mode) || mode == FaceOption.BOTH || mode == FaceOption.BE_OUT;
    }

    protected boolean canExtractEnergy(@Nullable Direction side) {
        if (!hasFaceCapabilityEnergy(side)) return false;
        if (side == null) return true;
        return FaceOption.isOut(energyFaceMode.getOrDefault(side, FaceOption.OFF)) || energyFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
    }

    /**
     * 物品面接收门控（主动/被动语义）：仅「被动输入 / 被动双向」允许被外部塞入；
     * 「主动输入」由机器自身主动拉取（{@link #doActiveItemIo}），不开放给外部。
     */
    protected boolean canReceiveItem(@Nullable Direction side) {
        if (!hasFaceCapabilityItem(side)) return false;
        if (side == null) return true;
        int mode = itemFaceMode.getOrDefault(side, FaceOption.OFF);
        return mode == FaceOption.BE_IN || mode == FaceOption.BOTH;
    }

    /**
     * 物品面提取门控（主动/被动语义）：仅「被动输出 / 被动双向」允许被外部抽取；
     * 「主动输出」由机器自身主动推出（{@link #doActiveItemIo}），不开放给外部。
     */
    protected boolean canExtractItem(@Nullable Direction side) {
        if (!hasFaceCapabilityItem(side)) return false;
        if (side == null) return true;
        int mode = itemFaceMode.getOrDefault(side, FaceOption.OFF);
        return mode == FaceOption.BE_OUT || mode == FaceOption.BOTH;
    }

    protected boolean canReceiveFluid(@Nullable Direction side) {
        if (!hasFaceCapabilityFluid(side)) return false;
        if (side == null) return true;
        return FaceOption.isIn(fluidFaceMode.getOrDefault(side, FaceOption.OFF)) || fluidFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
    }

    protected boolean canExtractFluid(@Nullable Direction side) {
        if (!hasFaceCapabilityFluid(side)) return false;
        if (side == null) return true;
        return FaceOption.isOut(fluidFaceMode.getOrDefault(side, FaceOption.OFF)) || fluidFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
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
        // P0-1: 批处理锁感知——锁定时按总 FE/t（baseFe × lockedB）检查，
        // 防止 stored >= baseFe 但 < totalFe 时 active 闪烁。
        int baseFe = getActualEfficiency();
        int checkFe = hasLockedBatch() ? (int) Math.round((double) baseFe * getLockedBatchSize()) : baseFe;
        return switch (machineType()) {
            case com.modularmc.ten.api.option.MachineType.GENERATOR, com.modularmc.ten.api.option.MachineType.ENGINE_SOLAR, com.modularmc.ten.api.option.MachineType.ENGINE_EXTRACTION, com.modularmc.ten.api.option.MachineType.ENGINE_METAL, com.modularmc.ten.api.option.MachineType.ENGINE_BIOMASS -> energyStorage.getEnergyStored() + checkFe <= maxStorageEnergy;
            default -> energyStorage.getEnergyStored() >= checkFe;
        };
    }

    /**
     * Syn 光合注能（P0-1 移植）：在有光条件下向本机储能注入固定 FE/t，
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

    // ───── 批处理锁定接口（P0-1 基础；P0-2 补四维计算与 validateAndLockB）─────

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

    public void doBaseData() {
        initMachine();
        if (energyStorage == null) return;
        resetUpgradeEffects();
        // ── 单次 apply：每 doBaseData 周期重置后仅应用一次（26.1.2 对齐；旧版双重 apply
        // 使乘子升级效果平方化、radius/batch 每 tick 累加）──
        applyUpgradeEffects();

        // ── 理论 B 批量缩放能量基础设施（26.1.2 对齐）：批量升级后处理速率 fePerTick =
        // efficientIn × lockedB，储能/吞吐不随 B 放大将导致 extractEnergy 永久不足 → 全机停滞 ──
        int theoreticalB = getTheoreticalBatchSize();
        int effectiveStorage = safeMultiply(initialEnergyStorage, theoreticalB);
        int effectiveReceive = safeMultiply(initialEnergyReceive, theoreticalB);
        int effectiveExtract = Math.max(
                safeMultiply(initialEnergyExtract, theoreticalB),
                safeMultiply(efficientIn, theoreticalB));

        // setCapacity 会在存量超出新容量时截断；batch 只增不减，不缩容
        energyStorage.setCapacity(effectiveStorage);
        energyStorage.setMaxReceive(effectiveReceive);
        energyStorage.setMaxExtract(effectiveExtract);

        maxStorageEnergy = effectiveStorage;
        maxReceiveEnergy = effectiveReceive;
        maxExtractEnergy = effectiveExtract;
        maxReceiveItem = initialItemReceive;
        maxExtractItem = initialItemExtract;
        maxReceiveFluid = initialFluidReceive;
        maxExtractFluid = initialFluidExtract;

        // ── P0-4 Stream: Unlimited energy transfer overrides rate limits ──
        // LevelupStream 安装时 maxReceive/maxExtract 设为 MAX_VALUE 解除速率限制。
        // 必须在 @DescSynced 写入段之前执行，否则客户端同步的 energyRec/energyExt
        // 永远是 override 前的 effective 值（26.1.2 对齐：override 在镜像写入前）。
        // 容量、面配置、canExternalExtract 与方向门控保持不变。
        if (hasUnlimitedEnergyTransfer()) {
            maxReceiveEnergy = Integer.MAX_VALUE;
            maxExtractEnergy = Integer.MAX_VALUE;
            energyStorage.setMaxReceive(Integer.MAX_VALUE);
            energyStorage.setMaxExtract(Integer.MAX_VALUE);
        }

        // ── Write to ldlib2 @DescSynced fields ──
        progress = Math.max(progress, 0);
        // 统一触发入口：@DescSynced 字段每 tick 写入后显式置脏，不依赖轮询值比较
        // （背景：探针证实 progress 自增后客户端/服务端镜像树均读到恒 0，强制推送绕开值比较环节）
        markDirty("progress");
        markDirty("maxProgress");
        markDirty("energyStored");
        markDirty("maxEnergyStored");
        maxEnergyStored = maxStorageEnergy;
        energyStored = energyStorage.getEnergyStored();
        energyRec = maxReceiveEnergy;
        energyExt = maxExtractEnergy;
        eff = efficientIn;
        itemRec = maxReceiveItem;
        itemExt = maxExtractItem;
        fluidRec = maxReceiveFluid;
        fluidExt = maxExtractFluid;
        upgSize = upgradeSize;
        // ── 流体容量批量缩放（26.1.2 对齐）：每 tank 按构造初始容量 × theoreticalB 独立缩放 ──
        if (!tanks.isEmpty()) {
            for (var tank : tanks) {
                int effectiveFluidCapacity = safeMultiply(tank.getInitialCapacity(), theoreticalB);
                tank.setCapacity(Math.max(effectiveFluidCapacity, tank.getFluidAmount()));
            }
        }

        if (energyStorage.getEnergyStored() > maxStorageEnergy) {
            energyStorage.setEnergy(maxStorageEnergy);
        }
        effAuc = efficientIn;
        if (getAliveTime() % 4 == 0) {
            effAuc = efficientIn;
        }

        // ── 主动物品 IO：面配置 IN=机器主动拉取 / OUT=机器主动推出（无速率上限，尽力搬空）──
        doActiveItemIo();

        // ── 主动能量 IO：面配置 OUT/BE_OUT/BOTH 时向相邻接收方推送（引擎/单元类）──
        // [修复] 引擎类此前从不调用 → 面配置“主动输出”完全不推送能量
        doActiveEnergyIo();

        // ── Sync face maps to arrays for client (server-authoritative mirror) ──
        // int[] 元素变更不触发 @DescSynced — 变化时全量推送 6 面 × 3 类型。
        if (rebuildFaceData()) {
            syncAllFacesToClients();
        }
    }

    // Slots
    public IngredientType slotType(int slot) {
        return IngredientType.IGNORE;
    }

    public boolean valid(int slot, ItemStack stack) {
        return true;
    }

    public boolean validUpgrade(int slot, ItemStack stack) {
        // Slots 0..MAX_UPGRADE_SLOTS-1 (0..5) are always valid for compatible upgrades
        if (!supportsUpgradeSlots()) return false;
        if (slot < 0 || slot >= MAX_UPGRADE_SLOTS) return false;
        if (!(stack.getItem() instanceof UpgradeItem upgradeItem)) return false;
        // LevelupSyn: max 1 per machine (enforced at install time)
        if (stack.getItem() instanceof LevelupSyn && hasUpgrade(LevelupSyn.class)) return false;
        // P3: Blast ↔ Smoke mutual exclusion — they cannot coexist.
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
        maxReceiveEnergy = initialEnergyReceive;
        maxExtractEnergy = initialEnergyExtract;
        maxReceiveItem = initialItemReceive;
        maxExtractItem = initialItemExtract;
        maxReceiveFluid = initialFluidReceive;
        maxExtractFluid = initialFluidExtract;
        upgradeSize = MAX_UPGRADE_SLOTS;
        // P0-4: 重置乘法模型字段
        durationMultiplier = 1.0;
        powerMultiplier = 1.0;
        batch = 0;
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

    // ───── P0-4 乘法模型 API 实现 (T1-T5) ─────

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
        // P3: First-wins semantics — only allow transition from SMELTING.
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
                // 限流只用 storage 实例自身 maxReceive（doBaseData 每 tick 与容量同源设置）；
                // maxReceiveEnergy 是 @DescSynced 显示镜像，作二次钳制存在与容量脱节的时序风险
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
                return canExtractItem(side) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY;
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
                return itemHandler.extractItem(slot, Math.min(amount, maxExtractItem), simulate);
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
                return canExtractFluid(side) ? combinedFluidHandler.getFluidInTank(tank) : FluidStack.EMPTY;
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
                    itemHandler.setStackInSlot(i, ItemStack.parseOptional(registries, list.getCompound(i)));
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
        // 读档后立即重建 faceData 镜像（26.1.2 P5-T1 对齐），服务端状态即时正确
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
            // 读档后立即重建 faceData 镜像（26.1.2 P5-T1 对齐），服务端状态即时正确
            rebuildFaceData();
        }
        loadSerializedHandlers(tag, registries);

        // ── P5-T1 旧档兼容：读档时无条件清零进度与运行锁 ──
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
                case 0 -> map = energyFaceMode;
                case 1 -> map = itemFaceMode;
                case 2 -> map = fluidFaceMode;
                default -> {
                    return;
                }
            }
            int mode = map.getOrDefault(direction, FaceOption.OFF) + 1;
            if (mode >= FaceOption.size()) mode = 0;
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

    // ───── 面配置同步（P0-3 移植）─────

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

    // ───── 主动 IO（P0-3 移植：面配置 IN/OUT 由机器自拉/自推；用户决策：取消 64/tick 上限，每次尽力搬空）─────

    /**
     * 机器主动物品 IO：对每个面，itemFaceMode == IN → 主动拉取；OUT → 主动推出。
     */
    private void doActiveItemIo() {
        if (level == null || level.isClientSide() || itemHandler == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            int mode = itemFaceMode.getOrDefault(direction, FaceOption.OFF);
            if (mode == FaceOption.IN) {
                activePullItems(direction);
            } else if (mode == FaceOption.OUT) {
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

    /** 主动推出：从输出槽推物品到相邻容器（非管道），尽力搬空输出槽（无速率上限）。 */
    private void activePushItems(Direction direction) {
        BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof PipeBlockEntity) {
            return; // 相邻为管道：由管道逐级传递处理
        }
        IItemHandler sink = TransferNetworks.getItems(level, targetPos, direction.getOpposite());
        if (sink == null) {
            return;
        }
        int remaining = Integer.MAX_VALUE; // 无速率上限：每次推到输出槽搬空/目标满为止
        for (int slot = 0; slot < itemHandler.getSlots() && remaining > 0; slot++) {
            if (!slotType(slot).canOut()) {
                continue; // 仅输出槽
            }
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int toPush = Math.min(stack.getCount(), remaining);
            ItemStack toInsert = stack.copy();
            toInsert.setCount(toPush);
            ItemStack leftover = TransferNetworks.insertItem(sink, toInsert, false);
            int accepted = toPush - leftover.getCount();
            if (accepted > 0) {
                itemHandler.extractItem(slot, accepted, false);
                remaining -= accepted;
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
     * 机器主动能量 IO：面配置 OUT/BOTH/BE_OUT 时向相邻容器推送能量。
     * 仅引擎/单元类机器（canExternalExtract=true）生效——普通机器设输出面不主动推。
     * BE_OUT（主动输出）语义：机器主动推给相邻；OUT（被动输出）语义：外部拉取（两者都推无害）。
     */
    protected void doActiveEnergyIo() {
        if (level == null || level.isClientSide() || energyStorage == null) {
            return;
        }
        if (!canExternalExtract()) {
            return; // 仅引擎/单元类主动推（原内部逐面判断提升为方法门禁，语义显式化）
        }
        for (Direction direction : Direction.values()) {
            int mode = energyFaceMode.getOrDefault(direction, initialFaceModeEnergy());
            if (mode != FaceOption.OUT && mode != FaceOption.BOTH && mode != FaceOption.BE_OUT) {
                continue; // [修复] 原：漏 BE_OUT——主动输出面完全不推送
            }
            IEnergyStorage sink = TransferNetworks.getEnergy(level, worldPosition.relative(direction), direction.getOpposite());
            if (sink == null || !sink.canReceive()) {
                continue;
            }
            // 本机可推能量 = min(储能, maxExtractEnergy)
            int available = Math.min(energyStorage.getEnergyStored(), maxExtractEnergy);
            if (available <= 0) {
                continue;
            }
            int accepted = sink.receiveEnergy(available, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
            }
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
        inventoryBuilder.accept(root);
        if (supportsUpgradeSlots()) {
            TENMachineBlockUIFactory.addUpgradeSlotsTab(root, this, uiState);
        }
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
        maxReceiveEnergy = (int) (maxReceiveEnergy + initialEnergyReceive * percent);
        maxExtractEnergy = (int) (maxExtractEnergy + initialEnergyExtract * percent);
        maxReceiveItem = (int) (maxReceiveItem + initialItemReceive * percent);
        maxExtractItem = (int) (maxExtractItem + initialItemExtract * percent);
        maxReceiveFluid = (int) (maxReceiveFluid + initialFluidReceive * percent);
        maxExtractFluid = (int) (maxExtractFluid + initialFluidExtract * percent);
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
