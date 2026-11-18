package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.MatchFuel.FuelProfile;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class SolarBlockEntity extends EngineBlockEntity {

    /** 光合引擎额定产能（FE/t），与 setEfficiency 一致。 */
    public static final int BASE_GENERATION_RATE = 10;

    /** 晴天一份「光照燃料」的总能量（FE）。 */
    private static final int SOLAR_CLEAR_SKY_BUDGET = 600;

    public SolarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(80));
        setEfficiency(BASE_GENERATION_RATE);
    }

    @Override
    public int machineType() {
        return MachineType.ENGINE_SOLAR;
    }

    @Override
    public int inventorySize() {
        return 0;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
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
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {}, root -> {
            // 光合引擎独立燃料表（fuel_gauge_solar_bg/fill 13×13 渐显）；能量条用 modular energy_gauge（80,12）
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 80, 12, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeModular13(this, 81, 64, false));
        });
    }

    /**
     * 光合引擎没有燃料槽，产能由天空条件续杯而非消耗物品，
     * 并行运行对它没有「并行燃烧几份」的语义，因此不开放并行运行。
     */
    @Override
    public boolean supportsBatch() {
        return false;
    }

    /** 晴天一份「光照燃料」的总能量（FE）。 */
    @Override
    public FuelProfile fuelProfile(ItemStack stack) {
        if (level != null && level.canSeeSky(worldPosition.above()) && level.isDay() && !level.isRaining()) {
            return FuelProfile.of(SOLAR_CLEAR_SKY_BUDGET, BASE_GENERATION_RATE, 1.0);
        }
        return FuelProfile.EMPTY;
    }

    /** 光合引擎不烧物品，燃料由天空条件续杯，不参与 {@code engine_fuel} 配方匹配。 */
    @Override
    public String fuelEngineId() {
        return "solar";
    }
}
