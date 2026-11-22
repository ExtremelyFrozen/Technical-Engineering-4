package com.modularmc.ten.common.blockentity.machine;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.IMachineType;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.recipe.EngineFluidFuelRecipe;
import com.modularmc.ten.common.blockentity.EngineFuels;
import com.modularmc.ten.common.blockentity.MatchFuel;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class CombustionBlockEntity extends EngineBlockEntity {

    public static final int BASE_GENERATION_RATE = 100;
    private static final int AMOUNT_PER_TIME = 100;

    public CombustionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(160));
        setEfficiency(BASE_GENERATION_RATE);
        tanks.add(new MachineFluidTank(8000));
    }

    @Setter
    private EngineFluidFuelRecipe recipe;

    @Override
    public IMachineType machineType() {
        return MachineType.ENGINE_COMBUSTION;
    }

    @Override
    public int inventorySize() {
        return 0;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.INPUT;
    }

    @Override
    public int initialFaceModeFluid() {
        return FaceOption.BE_IN;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 61, 21, 18, 50, 0));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 117, 22, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeModular(this, 81, 39, false));
        });
    }

    @Override
    public void process() {
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

        FluidStack fuelStack = FluidStack.EMPTY;
        if (!tanks.isEmpty()) {
            fuelStack = tanks.getFirst().getFluid();
        }

        MatchFuel.FuelProfile profile = fluidFuelProfile(fuelStack);
        if (profile.isEmpty()) {
            // 燃料烧完且没有下一份：修正倍率归位，面板回到引擎额定功率；
            // 燃料条的「总刻度」一并清零，免得空条上还挂着上一份的读数
            fuelRateMultiplier = 1.0;
            fuelTicksTotal = 0;
            setActive(false);
            return;
        }

        // 库存不足 B 份时按实有份数降级；光合引擎无燃料槽，预算由配方自身给出。
        int portions = !tanks.isEmpty() ? Math.min(batchSize, Math.max(1, fuelStack.getAmount() / AMOUNT_PER_TIME)) : batchSize;
        if (!tanks.isEmpty()) {
            fuelStack.shrink(portions * AMOUNT_PER_TIME);
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

    @Override
    public String fuelEngineId() {
        return "combustion";
    }

    private MatchFuel.FuelProfile fluidFuelProfile(FluidStack stack) {
        return EngineFuels.fluidProfile(level, fuelEngineId(), initialEfficientIn, stack, recipe, this);
    }
}
