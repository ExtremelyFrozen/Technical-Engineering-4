package com.modularmc.ten.lib.blockentity;

import com.modularmc.ten.lib.capability.MachineEnergyStorage;
import com.modularmc.ten.lib.capability.MachineFluidTank;
import com.modularmc.ten.lib.capability.MachineItemHandler;
import com.modularmc.ten.lib.option.FaceOption;
import com.modularmc.ten.lib.option.IngredientType;
import com.modularmc.ten.lib.option.MachineType;
import com.modularmc.ten.lib.option.RedstoneMode;
import com.modularmc.ten.lib.wrapper.SyncedIntArray;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CmMachineBlockEntity extends CmBlockEntity implements MenuProvider {

    // Data indices
    public static final int PROGRESS = 0;
    public static final int MAX_PROGRESS = 1;
    public static final int ENERGY = 2;
    public static final int MAX_ENERGY = 3;
    public static final int FUEL = 4;
    public static final int MAX_FUEL = 5;
    public static final int E_REC = 6;
    public static final int E_EXT = 7;
    public static final int I_REC = 8;
    public static final int I_EXT = 9;
    public static final int F_REC = 10;
    public static final int F_EXT = 11;
    public static final int RED_MODE = 12;
    public static final int FACE = 13;
    public static final int EFF_AUC = 14;
    public static final int EFF = 15;
    public static final int UPGSIZE = 16;

    // Energy
    public MachineEnergyStorage energyStorage;
    public int maxStorageEnergy;
    public int initialEnergyStorage;
    public int maxReceiveEnergy;
    public int initialEnergyReceive;
    public int maxExtractEnergy;
    public int initialEnergyExtract;
    public int efficientIn;
    public int initialEfficientIn;

    // Items
    public MachineItemHandler itemHandler;
    public int maxReceiveItem;
    public int initialItemReceive;
    public int maxExtractItem;
    public int initialItemExtract;

    // Fluids
    public List<MachineFluidTank> tanks = new ArrayList<>();
    public int maxReceiveFluid;
    public int initialFluidReceive;
    public int maxExtractFluid;
    public int initialFluidExtract;

    // Face config
    public Map<Direction, Integer> energyFaceMode = new HashMap<>();
    public Map<Direction, Integer> itemFaceMode = new HashMap<>();
    public Map<Direction, Integer> fluidFaceMode = new HashMap<>();

    public int redstoneMode = RedstoneMode.OFF;
    private boolean active = false;

    public CmMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        data = new SyncedIntArray(40);
        for (Direction d : Direction.values()) {
            energyFaceMode.put(d, FaceOption.BOTH);
            itemFaceMode.put(d, FaceOption.BOTH);
            fluidFaceMode.put(d, FaceOption.BOTH);
        }
    }

    public abstract int inventorySize();
    public abstract int machineType();

    // KFE helper
    public static int kFE(double k) {
        return (int) (1000 * k);
    }

    public void setCapacity(int energy) {
        initialEnergyStorage = energy;
        maxStorageEnergy = energy;
        energyStorage = new MachineEnergyStorage(energy, Math.max(energy / 100, 1), Math.max(energy / 100, 1));
    }

    public void setEfficiency(int eff) {
        initialEfficientIn = efficientIn = eff;
    }

    public void initMachine() {
        if (energyStorage == null) {
            energyStorage = new MachineEnergyStorage(
                    initialEnergyStorage > 0 ? initialEnergyStorage : 10000,
                    maxReceiveEnergy > 0 ? maxReceiveEnergy : 100,
                    maxExtractEnergy > 0 ? maxExtractEnergy : 100);
        }
        if (itemHandler == null) {
            itemHandler = new MachineItemHandler(inventorySize());
        }
    }

    public boolean hasUpgrade() { return true; }
    public boolean hasSideBar() { return true; }

    // Efficiency
    public int getActualEfficiency() { return data.get(EFF_AUC); }
    public double getActualEfficiencyPercent() { return 1.0; }

    public boolean isActive() { return active; }
    public void setActive(boolean a) { active = a; }

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
        return energyStorage.getEnergyStored() >= efficientIn;
    }

    public void doBaseData() {
        initMachine();
        energyStorage.setMaxReceive(maxReceiveEnergy);
        energyStorage.setMaxExtract(maxExtractEnergy);

        data.set(MAX_ENERGY, maxStorageEnergy);
        data.set(E_REC, maxReceiveEnergy);
        data.set(E_EXT, maxExtractEnergy);
        data.set(EFF, efficientIn);
        data.set(I_REC, maxReceiveItem);
        data.set(I_EXT, maxExtractItem);
        data.set(F_REC, maxReceiveFluid);
        data.set(F_EXT, maxExtractFluid);

        if (data.get(ENERGY) > maxStorageEnergy) {
            data.set(ENERGY, maxStorageEnergy);
        }
        data.set(EFF_AUC, efficientIn);

        if (getAliveTime() % 4 == 0) {
            data.set(EFF_AUC, efficientIn);
        }
    }

    // Slots
    public IngredientType slotType(int slot) { return IngredientType.IGNORE; }
    public boolean valid(int slot, ItemStack stack) { return true; }
    public IngredientType tankType(int tank) { return IngredientType.IGNORE; }
    public boolean valid(int slot, FluidStack stack) { return true; }

    // Capability access
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) { return energyStorage; }
    public IItemHandler getItemHandler(@Nullable Direction side) { return itemHandler; }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (tanks.isEmpty()) return null;
        return tanks.size() == 1 ? tanks.get(0) : new IFluidHandler() {
            @Override public int getTanks() { return tanks.size(); }
            @Override public FluidStack getFluidInTank(int tank) { return tanks.get(tank).getFluid(); }
            @Override public int getTankCapacity(int tank) { return tanks.get(tank).getCapacity(); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { return tanks.get(tank).isFluidValid(stack); }
            @Override public int fill(FluidStack resource, FluidAction action) {
                for (var t : tanks) { int f = t.fill(resource, action); if (f > 0) return f; }
                return 0;
            }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) {
                for (var t : tanks) { var d = t.drain(resource.getAmount(), action); if (!d.isEmpty()) return d; }
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int maxDrain, FluidAction action) {
                for (var t : tanks) { var d = t.drain(maxDrain, action); if (!d.isEmpty()) return d; }
                return FluidStack.EMPTY;
            }
        };
    }

    // NBT
    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        if (energyStorage != null) energyStorage.setEnergy(tag.getInt("energy"));
        if (itemHandler != null) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        redstoneMode = tag.getInt("redstone");
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        if (energyStorage != null) tag.putInt("energy", energyStorage.getEnergyStored());
        if (itemHandler != null) tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("redstone", redstoneMode);
    }

    // MenuProvider
    @Override
    public Component getDisplayName() { return component; }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null; // Will be implemented when container system is ported
    }
}
