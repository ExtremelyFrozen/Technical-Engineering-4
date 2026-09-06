package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class SolarBlockEntity extends EngineBlockEntity {

    public SolarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(80));
        setEfficiency(10);
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
            // 光合引擎保留原 HANDLER 素材坐标（80,12 能量条 / 81,64 燃料条），仅借 Reveal 渐显
            root.addChild(TENMachineBlockUIFactory.energyGaugeReveal(this, 80, 12, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeReveal(this, 81, 64, 13, 13, 14, 52, false));
        });
    }

    @Override
    public int matchFuel(ItemStack stack, boolean simulate) {
        if (level != null && level.canSeeSky(worldPosition.above()) && level.isDay() && !level.isRaining()) {
            return 600;
        }
        return 0;
    }
}
