package com.modularmc.ten.common.blockentity.machine;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class CauldronBlockEntity extends ProcessingMachineBlockEntity {
    public CauldronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        tanks.add(new MachineFluidTank(20000));
    }

    private boolean locked;
    private int currentType = 0;
    private int checkCooling = 0;

    @Override
    public MachineType machineType() {
        return MachineType.CAULDRON;
    }

    @Override
    public int inventorySize() {
        return 0;
    }

    public int initialFaceModeFluid() {
        return FaceOption.BE_OUT;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    @Override
    public int baseTickTime() {
        return 20;
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {}, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 31, 17, 18, 50, 0));
        });
    }

    @Override
    public boolean conditionStart() {
        if (locked){
            return true;
        }
        MachineFluidTank tank = tanks.getFirst();
        if (tank.getSpace() == 0) return false;
        if (checkCooling > 0){
            checkCooling --;
            return false;
        }
        int type = checkStalactiteDrip(level, worldPosition);
        if (type == 0){
            if (level != null && level.canSeeSky(worldPosition.above()) && level.isRaining()){
                Biome biome = level.getBiome(worldPosition).value();
                if (biome.hasPrecipitation()) {
                    type = biome.warmEnoughToRain(worldPosition) ? 1 : 3;
                }
            }
        }

        if (tank.isEmpty() || tank.getFluid().getFluid() == getDew(type).getFluid()){
            locked = true;
            currentType = type;
            return true;
        }
        checkCooling = 40;
        return false;
    }

    @Override
    public void onCookFinish() {
        tanks.getFirst().fill(DewMode.values()[currentType].produce(), IFluidHandler.FluidAction.EXECUTE);
        locked = false;
    }

    private static FluidStack getDew(int type){
        return DewMode.values()[type].produce();
    }

    private static int checkStalactiteDrip(Level level, BlockPos pos){
        BlockPos tipPos = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
        if (tipPos == null) {
            return 0;
        }
        Fluid fluid = PointedDripstoneBlock.getCauldronFillFluidType((ServerLevel) level, tipPos);
        if (fluid == Fluids.LAVA) {
            return 2;
        } else if (fluid == Fluids.WATER) {
            return 1;
        }else return 0;
    }

    public enum DewMode {
        WATER { public FluidStack produce() { return new FluidStack(Fluids.WATER, 2500); } },
        WATER_ENHANCED { public FluidStack produce() { return new FluidStack(Fluids.WATER, 5000); } },
        LAVA  { public FluidStack produce() { return new FluidStack(Fluids.LAVA, 100); } },
        SNOW  { public FluidStack produce() { return new FluidStack(Fluids.WATER, 100); } };

        public abstract FluidStack produce();  // 每次返回新实例，避免 FluidStack 可变共享
    }
}
