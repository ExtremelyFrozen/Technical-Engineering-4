package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.capability.MachineEnergyStorage;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.option.RedstoneMode;
import com.modularmc.ten.api.wrapper.SyncedIntArray;
import com.modularmc.ten.common.item.upgrades.IUpgradableMachine;
import com.modularmc.ten.common.item.upgrades.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CmMachineBlockEntity extends CmBlockEntity implements MenuProvider, IUpgradableMachine {

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
    public MachineItemHandler upgradeHandler;
    public int maxReceiveItem;
    public int initialItemReceive;
    public int maxExtractItem;
    public int initialItemExtract;
    public int upgradeSize = 1;
    public int initialUpgradeSize = 1;
    public static final int MAX_UPGRADE_SLOTS = 6;

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
    private int facing = Direction.NORTH.get3DDataValue();
    private boolean machineInitialised = false;
    private IFluidHandler combinedFluidHandler;

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

        if (initialEnergyStorage <= 0) {
            initialEnergyStorage = 10000;
        }
        if (initialEnergyReceive <= 0) {
            initialEnergyReceive = Math.max(initialEnergyStorage / 200, 1);
        }
        if (initialEnergyExtract <= 0) {
            initialEnergyExtract = Math.max(initialEnergyStorage / 200, 1);
        }
        if (maxStorageEnergy <= 0) {
            maxStorageEnergy = initialEnergyStorage;
        }
        if (maxReceiveEnergy <= 0) {
            maxReceiveEnergy = initialEnergyReceive;
        }
        if (maxExtractEnergy <= 0) {
            maxExtractEnergy = initialEnergyExtract;
        }
        if (itemHandler == null) {
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
        upgradeHandler.setChangeListener(this::markDirty);

        if (energyStorage == null) {
            energyStorage = new MachineEnergyStorage(maxStorageEnergy, maxReceiveEnergy, maxExtractEnergy);
        }
        energyStorage.setChangeListener(this::markDirty);

        if (maxReceiveItem <= 0) {
            maxReceiveItem = initialItemReceive > 0 ? initialItemReceive : 8;
        }
        if (maxExtractItem <= 0) {
            maxExtractItem = initialItemExtract > 0 ? initialItemExtract : 8;
        }
        initialItemReceive = maxReceiveItem;
        initialItemExtract = maxExtractItem;

        if (maxReceiveFluid <= 0) {
            maxReceiveFluid = initialFluidReceive > 0 ? initialFluidReceive : 100;
        }
        if (maxExtractFluid <= 0) {
            maxExtractFluid = initialFluidExtract > 0 ? initialFluidExtract : 100;
        }
        initialFluidReceive = maxReceiveFluid;
        initialFluidExtract = maxExtractFluid;

        for (var tank : tanks) {
            tank.setChangeListener(this::markDirty);
        }
        combinedFluidHandler = createCombinedFluidHandler();
    }

    public boolean hasUpgrade() {
        return true;
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

    // Efficiency
    public int getActualEfficiency() {
        return data.get(EFF_AUC);
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
        if (facing == value) return;
        facing = value;
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(com.modularmc.ten.common.block.machine.HorizontalMachineBlock.FACING) && direction.getAxis().isHorizontal()) {
                level.setBlock(worldPosition,
                        state.setValue(com.modularmc.ten.common.block.machine.HorizontalMachineBlock.FACING, direction),
                        Block.UPDATE_ALL);
            } else if (state.hasProperty(com.modularmc.ten.common.block.machine.DirectionalMachineBlock.FACING)) {
                level.setBlock(worldPosition,
                        state.setValue(com.modularmc.ten.common.block.machine.DirectionalMachineBlock.FACING, direction),
                        Block.UPDATE_ALL);
            }
        }
        markDirty();
    }

    public Direction getFacing() {
        return Direction.from3DDataValue(facing);
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
        return FaceOption.isIn(energyFaceMode.getOrDefault(side, FaceOption.OFF)) || energyFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
    }

    protected boolean canExtractEnergy(@Nullable Direction side) {
        if (!hasFaceCapabilityEnergy(side)) return false;
        if (side == null) return true;
        return FaceOption.isOut(energyFaceMode.getOrDefault(side, FaceOption.OFF)) || energyFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
    }

    protected boolean canReceiveItem(@Nullable Direction side) {
        if (!hasFaceCapabilityItem(side)) return false;
        if (side == null) return true;
        return FaceOption.isIn(itemFaceMode.getOrDefault(side, FaceOption.OFF)) || itemFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
    }

    protected boolean canExtractItem(@Nullable Direction side) {
        if (!hasFaceCapabilityItem(side)) return false;
        if (side == null) return true;
        return FaceOption.isOut(itemFaceMode.getOrDefault(side, FaceOption.OFF)) || itemFaceMode.getOrDefault(side, FaceOption.OFF) == FaceOption.BOTH;
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
        return switch (machineType()) {
            case com.modularmc.ten.api.option.MachineType.GENERATOR, com.modularmc.ten.api.option.MachineType.ENGINE_SOLAR, com.modularmc.ten.api.option.MachineType.ENGINE_EXTRACTION, com.modularmc.ten.api.option.MachineType.ENGINE_METAL, com.modularmc.ten.api.option.MachineType.ENGINE_BIOMASS -> energyStorage.getEnergyStored() + getActualEfficiency() <= maxStorageEnergy;
            default -> energyStorage.getEnergyStored() >= efficientIn;
        };
    }

    public void doBaseData() {
        initMachine();
        if (energyStorage == null) return;
        resetUpgradeEffects();
        applyUpgradeEffects();
        maxStorageEnergy = initialEnergyStorage;
        maxReceiveEnergy = initialEnergyReceive;
        maxExtractEnergy = initialEnergyExtract;
        maxReceiveItem = initialItemReceive;
        maxExtractItem = initialItemExtract;
        maxReceiveFluid = initialFluidReceive;
        maxExtractFluid = initialFluidExtract;

        applyUpgradeEffects();

        energyStorage.setMaxReceive(maxReceiveEnergy);
        energyStorage.setMaxExtract(maxExtractEnergy);

        data.set(PROGRESS, Math.max(data.get(PROGRESS), 0));
        data.set(MAX_ENERGY, maxStorageEnergy);
        data.set(ENERGY, energyStorage.getEnergyStored());
        data.set(E_REC, maxReceiveEnergy);
        data.set(E_EXT, maxExtractEnergy);
        data.set(EFF, efficientIn);
        data.set(I_REC, maxReceiveItem);
        data.set(I_EXT, maxExtractItem);
        data.set(F_REC, maxReceiveFluid);
        data.set(F_EXT, maxExtractFluid);
        data.set(RED_MODE, redstoneMode);
        data.set(FACE, facing);
        data.set(UPGSIZE, upgradeSize);

        if (energyStorage.getEnergyStored() > maxStorageEnergy) {
            energyStorage.setEnergy(maxStorageEnergy);
        }
        data.set(EFF_AUC, efficientIn);

        if (getAliveTime() % 4 == 0) {
            data.set(EFF_AUC, efficientIn);
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
        return stack.getItem() instanceof UpgradeItem && slot < Math.max(1, Math.min(upgradeSize, MAX_UPGRADE_SLOTS));
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
        upgradeSize = Math.max(1, Math.min(initialUpgradeSize, MAX_UPGRADE_SLOTS));
    }

    protected void applyUpgradeEffects() {
        if (!hasUpgrade() || upgradeHandler == null) {
            return;
        }
        int index = 0;
        while (index < upgradeSize && index < upgradeHandler.getSlots()) {
            ItemStack stack = upgradeHandler.getStackInSlot(index);
            if (stack.getItem() instanceof UpgradeItem upgradeItem) {
                upgradeItem.effect(this);
            }
            index++;
        }
        upgradeSize = Math.max(1, Math.min(upgradeSize, MAX_UPGRADE_SLOTS));
    }

    public int getUnlockedUpgradeSlots() {
        return Math.max(1, Math.min(upgradeSize, MAX_UPGRADE_SLOTS));
    }

    // Capability access
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (energyStorage == null) return null;
        return new IEnergyStorage() {

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if (!signalAllowRun() || !canReceiveEnergy(side)) return 0;
                return energyStorage.receiveEnergy(Math.min(maxReceive, maxReceiveEnergy), simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if (!signalAllowRun() || !canExtractEnergy(side)) return 0;
                return energyStorage.extractEnergy(Math.min(maxExtract, maxExtractEnergy), simulate);
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
                return canExtractEnergy(side);
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

    // NBT
    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        initMachine();
        if (energyStorage != null) energyStorage.setEnergy(tag.getInt("energy"));
        if (itemHandler != null) itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        if (upgradeHandler != null) upgradeHandler.deserializeNBT(registries, tag.getCompound("upgrades"));
        redstoneMode = tag.getInt("redstone");
        data.set(PROGRESS, tag.getInt("progress"));
        data.set(MAX_PROGRESS, tag.getInt("max_progress"));
        data.set(FUEL, tag.getInt("fuel"));
        data.set(MAX_FUEL, tag.getInt("max_fuel"));
        upgradeSize = tag.contains("upgrade_size") ? tag.getInt("upgrade_size") : initialUpgradeSize;
        facing = tag.contains("face") ? tag.getInt("face") : getFacing().get3DDataValue();
        active = tag.getBoolean("active");
        for (Direction direction : Direction.values()) {
            energyFaceMode.put(direction, tag.getInt("direEnergy" + direction.get3DDataValue()));
            itemFaceMode.put(direction, tag.getInt("direItem" + direction.get3DDataValue()));
            fluidFaceMode.put(direction, tag.getInt("direFluid" + direction.get3DDataValue()));
        }
        loadSerializedHandlers(tag, registries);
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        if (energyStorage != null) tag.putInt("energy", energyStorage.getEnergyStored());
        if (itemHandler != null) tag.put("inventory", itemHandler.serializeNBT(registries));
        if (upgradeHandler != null) tag.put("upgrades", upgradeHandler.serializeNBT(registries));
        tag.putInt("redstone", redstoneMode);
        tag.putInt("progress", data.get(PROGRESS));
        tag.putInt("max_progress", data.get(MAX_PROGRESS));
        tag.putInt("fuel", data.get(FUEL));
        tag.putInt("max_fuel", data.get(MAX_FUEL));
        tag.putInt("upgrade_size", upgradeSize);
        tag.putInt("face", facing);
        tag.putBoolean("active", active);
        for (Direction direction : Direction.values()) {
            tag.putInt("direEnergy" + direction.get3DDataValue(), energyFaceMode.getOrDefault(direction, initialFaceModeEnergy()));
            tag.putInt("direItem" + direction.get3DDataValue(), itemFaceMode.getOrDefault(direction, initialFaceModeItem()));
            tag.putInt("direFluid" + direction.get3DDataValue(), fluidFaceMode.getOrDefault(direction, initialFaceModeFluid()));
        }
        saveSerializedHandlers(tag, registries);
    }

    // MenuProvider
    @Override
    public Component getDisplayName() {
        return component != null ? component : getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.modularmc.ten.client.gui.CmContainerMachine(
                com.modularmc.ten.common.data.TENMenuTypes.MACHINE.get(), id, inv, this, worldPosition);
    }

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
            if (drained.isEmpty()) {
                drained = piece.copy();
            } else {
                drained.grow(piece.getAmount());
            }
            if (drained.getAmount() >= resource.getAmount()) {
                break;
            }
        }
        return drained;
    }

    protected FluidStack drainFluid(int maxDrain, IFluidHandler.FluidAction action) {
        FluidStack drained = FluidStack.EMPTY;
        for (int i = 0; i < tanks.size(); i++) {
            if (!tankType(i).canOut()) continue;
            FluidStack piece = tanks.get(i).drain(maxDrain - drained.getAmount(), action);
            if (piece.isEmpty()) continue;
            if (drained.isEmpty()) {
                drained = piece.copy();
            } else if (drained.is(piece.getFluid())) {
                drained.grow(piece.getAmount());
            }
            if (drained.getAmount() >= maxDrain) {
                break;
            }
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
            case "MACHINE_EFFECT" -> machineType() == MachineType.MACHINE_EFFECT || machineType() == MachineType.BEACON || machineType() == MachineType.MOB_RIPPER || machineType() == MachineType.FARM;
            case "FURNACE" -> machineType() == MachineType.FURNACE;
            case "BEACON" -> machineType() == MachineType.BEACON;
            case "QUARRY" -> machineType() == MachineType.QUARRY || machineType() == MachineType.FARM;
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
