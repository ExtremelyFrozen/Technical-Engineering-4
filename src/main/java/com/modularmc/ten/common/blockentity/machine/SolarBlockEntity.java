package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.SkyLightHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class SolarBlockEntity extends EngineBlockEntity {

    /** Base FE/t generation rate for Solar. Pinned by BaseGenerationRateTest. */
    public static final int BASE_GENERATION_RATE = 10;

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
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 80, 12, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeModular(this, 81, 64, false));
        });
    }

    @Override
    public int matchFuel(ItemStack stack, boolean simulate) {
        // 使用共享光照判定 helper（可看到天空、不下雨、昼间三者同时成立）
        if (SkyLightHelper.hasEffectiveLight(level, worldPosition)) {
            return 600;
        }
        return 0;
    }
}
