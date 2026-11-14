package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.EngineFuels;
import com.modularmc.ten.common.blockentity.MatchFuel.FuelProfile;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

/**
 * 引擎基类：由燃料匹配（MatchFuel）或天空光照驱动产能，
 * 能量面强制 OUT 主动向相邻机器推送。
 * <p>
 * 规范 v2 生产端：<b>FE/t 只由引擎声明</b>（{@code BASE_GENERATION_RATE}），
 * 配方只给「耗时 T」与「FE/t 修正倍率 m」。实际产出 {@code P × m × s × B}，
 * 燃料总能量 {@code P × m × T × B}——配方不再声明绝对 FE/t。
 */
public abstract class EngineBlockEntity extends CmMachineBlockEntity {

    /**
     * 当前燃烧燃料对引擎额定 FE/t 的修正倍率（配方数据）。
     * 无燃料时为 1.0，因此空载面板显示引擎额定功率。
     */
    protected double fuelRateMultiplier = 1.0;

    public EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 实际产出/耗能速率：引擎额定 × 升级功率倍率 × 配方 FE/t 修正。
     * 覆写给面板与产出共用同一口径，避免「面板 30、实际 20」的错位。
     */
    @Override
    public int getActualEfficiency() {
        return Math.max(1, (int) Math.round(super.getActualEfficiency() * fuelRateMultiplier));
    }

    /** 当前燃料的 FE/t 修正倍率（1.0 = 按引擎额定）。 */
    public double getFuelRateMultiplier() {
        return fuelRateMultiplier;
    }

    @Override
    public int machineType() {
        return MachineType.GENERATOR;
    }

    @Override
    public int initialFaceModeEnergy() {
        return FaceOption.OUT;
    }

    @Override
    public boolean hasFaceCapabilityFluid(Direction side) {
        return false;
    }

    @Override
    public boolean hasFaceCapabilityEnergy(Direction side) {
        return side == null || side == Direction.UP;
    }

    @Override
    public boolean hasFaceCapabilityItem(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 36));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 117, 22, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeModular(this, 81, 39, false));
        });
    }

    /**
     * 规范 v2 ⑥：并行运行对生产端 = 同时燃烧 B 份燃料。
     * 产出 ×B、燃料消耗 ×B、总能量 ×B，每份燃料烧的时间仍为 {@code T ÷ s}，
     * 不随 B 缩短（否则等于从同一份燃料里凭空多榨能量）。
     */
    @Override
    public boolean supportsBatch() {
        return true;
    }

    @Override
    public void tick() {
        doBaseData();

        if (!signalAllowRun() || !energyAllowRun()) return;

        int batchSize = getTheoreticalBatchSize();

        if (fuel > 0) {
            // 产出与燃料扣减用同一个数：实际写入量。缓冲满时不再扣燃料，
            // 否则 receiveEnergy 截断会造成「燃料烧掉、电没发出来」的能量损失。
            // 末 tick 按剩余预算封顶，避免最后一下多发一份超出燃料总能量。
            int requested = Math.min(safeMultiply(getActualEfficiency(), batchSize), fuel);
            int accepted = energyStorage.receiveEnergy(requested, false);
            if (accepted <= 0) {
                setActive(false);
                return;
            }
            fuel = Math.max(fuel - accepted, 0);
            setActive(true);
            return;
        }

        ItemStack fuelStack = ItemStack.EMPTY;
        if (itemHandler != null && itemHandler.getSlots() > 0) {
            fuelStack = itemHandler.getStackInSlot(0);
        }

        FuelProfile profile = fuelProfile(fuelStack);
        if (profile.isEmpty()) {
            // 燃料烧完且没有下一份：修正倍率归位，面板回到引擎额定功率；
            // 燃料条的「总刻度」一并清零，免得空条上还挂着上一份的读数
            fuelRateMultiplier = 1.0;
            fuelTicksTotal = 0;
            setActive(false);
            return;
        }

        // 库存不足 B 份时按实有份数降级；光合引擎无燃料槽，预算由配方自身给出。
        int portions = itemHandler != null && itemHandler.getSlots() > 0 ? Math.min(batchSize, Math.max(1, fuelStack.getCount())) : batchSize;
        if (itemHandler != null && itemHandler.getSlots() > 0) {
            fuelStack.shrink(portions);
        }
        fuelRateMultiplier = profile.rateMultiplier();
        fuel = safeMultiply(profile.budget(), portions);
        maxFuel = fuel;
        // 修正后的持续时间 = 本次装填的总能量 ÷ 实际产出率（已含升级功率倍率与并行运行数）。
        // 无升级时它就是配方的 durationTicks；装了倍率卡后按 Σpower 缩短。
        int perTick = safeMultiply(getActualEfficiency(), batchSize);
        fuelTicksTotal = perTick > 0 ? (int) Math.ceil((double) fuel / perTick) : 0;
        setActive(true);
    }

    /**
     * 本引擎在燃料配方里使用的 id（{@code extractor} / {@code metalizer} / {@code biomass}）。
     * 数据包按这个 id 决定一份燃料是否适用于本引擎。
     */
    public abstract String fuelEngineId();

    /**
     * 查询一份燃料在本引擎上的配方数据（耗时 + FE/t 修正倍率）。
     * 默认走数据包配方 {@code kenergyengineering:engine_fuel}；
     * 返回 {@link FuelProfile#EMPTY} 表示不接受该物品。
     */
    public FuelProfile fuelProfile(ItemStack stack) {
        return EngineFuels.profile(level, fuelEngineId(), initialEfficientIn, stack);
    }
}
