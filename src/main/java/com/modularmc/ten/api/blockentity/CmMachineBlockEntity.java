package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.capability.FluidHandlerResourceAdapter;
import com.modularmc.ten.api.capability.MachineEnergyStorage;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.option.RedstoneMode;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.IUpgradableMachine;
import com.modularmc.ten.common.item.upgrades.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.mojang.serialization.Codec;
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
    public int upgSize = MAX_UPGRADE_SLOTS; // UPGSIZE — always 6, all slots permanently unlocked
    @Persisted
    @DescSynced
    public int facingVal = 2;       // FACE (Direction.NORTH)
    @Persisted
    @DescSynced
    public int redstoneMode = RedstoneMode.OFF;
    @Persisted
    @DescSynced
    public boolean active = false;

    // Face config for client display (synced via @DescSynced)
    @Persisted
    @DescSynced
    public int[] energyFaceData = new int[6];
    @Persisted
    @DescSynced
    public int[] itemFaceData = new int[6];
    @Persisted
    @DescSynced
    public int[] fluidFaceData = new int[6];

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
    public int upgradeSize = MAX_UPGRADE_SLOTS;
    public int initialUpgradeSize = MAX_UPGRADE_SLOTS;
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

    // Cached ResourceHandler adapters for the new capability boundary.
    // Lazily constructed and invalidated on machine re-init.
    private ResourceHandler<FluidResource> fluidResourceHandler;
    private EnumMap<Direction, ResourceHandler<FluidResource>> sidedFluidResourceHandlers;

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
        upgradeHandler.setChangeListener(this::markDirty);

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
    protected void loadAdditional(ValueInput input) {
        initMachine();
        super.loadAdditional(input);
    }

    /**
     * Whether this machine has upgrade slots at all (fixed capability).
     * Controls whether {@link #buildMachineUI} adds the 6 upgrade slot UI.
     * <p>
     * Override to {@code false} for machines that cannot accept upgrades
     * (e.g. Cell, CreativeCell, AbstractChannel). Normal machines and engines
     * inherit the default {@code true} — their 6 slots are always shown.
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
        return effAuc;
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

    public Direction getFacing() {
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
        // ── Single apply: reset then apply exactly once per doBaseData cycle ──
        applyUpgradeEffects();

        energyStorage.setMaxReceive(maxReceiveEnergy);
        energyStorage.setMaxExtract(maxExtractEnergy);

        // ── Write to ldlib2 @DescSynced fields ──
        progress = Math.max(progress, 0);
        maxEnergyStored = maxStorageEnergy;
        energyStored = energyStorage.getEnergyStored();
        energyRec = maxReceiveEnergy;
        energyExt = maxExtractEnergy;
        eff = efficientIn;
        itemRec = maxReceiveItem;
        itemExt = maxExtractItem;
        fluidRec = maxReceiveFluid;
        fluidExt = maxExtractFluid;
        upgSize = MAX_UPGRADE_SLOTS;

        if (energyStorage.getEnergyStored() > maxStorageEnergy) {
            energyStorage.setEnergy(maxStorageEnergy);
        }
        effAuc = efficientIn;
        if (getAliveTime() % 4 == 0) {
            effAuc = efficientIn;
        }

        // ── Sync face maps to arrays for client ──
        for (Direction d : Direction.values()) {
            int idx = d.get3DDataValue();
            energyFaceData[idx] = energyFaceMode.getOrDefault(d, initialFaceModeEnergy());
            itemFaceData[idx] = itemFaceMode.getOrDefault(d, initialFaceModeItem());
            fluidFaceData[idx] = fluidFaceMode.getOrDefault(d, initialFaceModeFluid());
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
    }

    /**
     * Apply effects from all installed upgrades in all 6 slots.
     * <p>
     * Incompatible upgrades (e.g. from old saves where machine type has changed,
     * or upgrades that fail {@link UpgradeItem#canApply}) are silently skipped.
     * Upgrades that don't pass canApply still have their effect() skipped to
     * prevent unintended stat modifications, but the item remains in the slot
     * so the player can retrieve it.
     */
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
    }

    /**
     * All 6 upgrade slots are permanently unlocked.
     * The old slot-unlock system has been removed.
     */
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
                return energyStorage.receiveEnergy(Math.min(maxReceive, maxReceiveEnergy), simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if (!canExternalExtract()) return 0;
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

    // ───── Fluid ResourceHandler (cached adapters for capability boundary) ─────

    /**
     * Returns a {@link ResourceHandler<FluidResource>} for the fluid capability boundary,
     * backed by {@link FluidHandlerResourceAdapter} with side-based permissions.
     *
     * <p>
     * Results are lazily cached: one unsided instance for {@code side == null},
     * and one per {@link Direction} for sided access. The cache is stable for the
     * lifetime of the block entity — the fields are never reset to null. All
     * adapters share the same {@link #tanks} list (no data copy) and evaluate
     * face mode and redstone signal dynamically through lambda suppliers, so
     * permission changes take effect on the next call without cache invalidation.
     *
     * @param side the direction, or null for unsided access
     * @return the cached ResourceHandler, or null if no tanks are available
     */
    public ResourceHandler<FluidResource> getFluidResourceHandler(@Nullable Direction side) {
        if (tanks.isEmpty()) return null;
        if (side == null) {
            if (fluidResourceHandler == null) {
                fluidResourceHandler = createFluidResourceHandler(null);
            }
            return fluidResourceHandler;
        }
        if (sidedFluidResourceHandlers == null) {
            sidedFluidResourceHandlers = new EnumMap<>(Direction.class);
        }
        return sidedFluidResourceHandlers.computeIfAbsent(side, this::createFluidResourceHandler);
    }

    /**
     * Creates a new {@link FluidHandlerResourceAdapter} for the given side.
     * Permissions: side==null → allowed; otherwise → signalAllowRun + face config.
     * Commit callback: {@link #markDirty()} (inherited from {@link CmBlockEntity}).
     */
    private FluidHandlerResourceAdapter createFluidResourceHandler(@Nullable Direction side) {
        return new FluidHandlerResourceAdapter(
                tanks,
                this::tankType,
                (index, stack) -> valid(index.intValue(), stack),
                () -> side == null || (signalAllowRun() && canReceiveFluid(side)),
                () -> side == null || (signalAllowRun() && canExtractFluid(side)),
                this::markDirty);
    }

    // ───── NBT ─────
    @Override
    protected void readTileData(ValueInput input) {
        if (energyStorage != null) energyStorage.setEnergy(input.getInt("energy").orElse(0));
        if (itemHandler != null) {
            input.read("inventory", Codec.list(ItemStack.OPTIONAL_CODEC)).ifPresent(stacks -> {
                for (int i = 0; i < Math.min(stacks.size(), itemHandler.getSlots()); i++) {
                    itemHandler.setStackInSlot(i, stacks.get(i));
                }
            });
        }
        if (upgradeHandler != null) {
            input.read("upgrades", Codec.list(ItemStack.OPTIONAL_CODEC)).ifPresent(stacks -> {
                for (int i = 0; i < Math.min(stacks.size(), upgradeHandler.getSlots()); i++) {
                    upgradeHandler.setStackInSlot(i, stacks.get(i));
                }
            });
        }
        upgradeSize = MAX_UPGRADE_SLOTS; // Always 6; old upgrade_size NBT is ignored
        // Read old upgrade_size for forward compat (value discarded, always 6)
        input.getInt("upgrade_size").ifPresent(oldSize -> { /* ignored — all 6 slots always unlocked */ });
        for (Direction direction : Direction.values()) {
            int idx = direction.get3DDataValue();
            energyFaceMode.put(direction, input.getInt("direEnergy" + idx).orElse(initialFaceModeEnergy()));
            itemFaceMode.put(direction, input.getInt("direItem" + idx).orElse(initialFaceModeItem()));
            fluidFaceMode.put(direction, input.getInt("direFluid" + idx).orElse(initialFaceModeFluid()));
        }
        loadSerializedHandlers(input);
    }

    @Override
    protected void writeTileData(ValueOutput output) {
        if (energyStorage != null) output.store("energy", Codec.INT, energyStorage.getEnergyStored());
        if (itemHandler != null) {
            var stacks = new java.util.ArrayList<ItemStack>();
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                stacks.add(itemHandler.getStackInSlot(i));
            }
            output.store("inventory", Codec.list(ItemStack.OPTIONAL_CODEC), stacks);
        }
        if (upgradeHandler != null) {
            var stacks = new java.util.ArrayList<ItemStack>();
            for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                stacks.add(upgradeHandler.getStackInSlot(i));
            }
            output.store("upgrades", Codec.list(ItemStack.OPTIONAL_CODEC), stacks);
        }
        output.store("upgrade_size", Codec.INT, MAX_UPGRADE_SLOTS);
        for (Direction direction : Direction.values()) {
            int idx = direction.get3DDataValue();
            output.store("direEnergy" + idx, Codec.INT, energyFaceMode.getOrDefault(direction, initialFaceModeEnergy()));
            output.store("direItem" + idx, Codec.INT, itemFaceMode.getOrDefault(direction, initialFaceModeItem()));
            output.store("direFluid" + idx, Codec.INT, fluidFaceMode.getOrDefault(direction, initialFaceModeFluid()));
        }
        saveSerializedHandlers(output);
    }

    // ───── @RPCMethod: 替换自定义网络包 ─────
    /** C→S: 切换红石模式 */
    @RPCMethod
    public void rpcSetRedstoneMode(RPCSender sender, int mode) {
        if (sender.isServer()) {
            redstoneMode = mode;
            setChanged();
            // 转发 face info 给所有追踪玩家
            for (Direction direction : Direction.values()) {
                int idx = direction.get3DDataValue();
                rpcToTracking("rpcSyncFaceInfo", idx,
                        energyFaceMode.getOrDefault(direction, 0),
                        itemFaceMode.getOrDefault(direction, 0),
                        fluidFaceMode.getOrDefault(direction, 0));
            }
        }
    }

    /** C→S: 切换面配置 */
    @RPCMethod
    public void rpcCycleFaceMode(RPCSender sender, int changeType, int dirIndex) {
        if (sender.isServer()) {
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
            rpcToTracking("rpcSyncFaceInfo", dirIndex,
                    energyFaceMode.getOrDefault(direction, 0),
                    itemFaceMode.getOrDefault(direction, 0),
                    fluidFaceMode.getOrDefault(direction, 0));
        }
    }

    /** S→C: 同步面配置到客户端 */
    @RPCMethod
    public void rpcSyncFaceInfo(RPCSender sender, int dirIndex, int energyMode, int itemMode, int fluidMode) {
        if (!sender.isServer()) {
            energyFaceData[dirIndex] = energyMode;
            itemFaceData[dirIndex] = itemMode;
            fluidFaceData[dirIndex] = fluidMode;
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
                                             Identifier background,
                                             Consumer<UIElement> inventoryBuilder,
                                             Consumer<UIElement> contentBuilder) {
        initMachine();
        var root = TENMachineBlockUIFactory.createRoot(background);
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
            TENMachineBlockUIFactory.addUpgradeSlots(root, this);
        }
        TENMachineBlockUIFactory.addPlayerInventory(root);
        TENMachineBlockUIFactory.addCommonSidebar(root, holder, this, new TENMachineBlockUIFactory.UIState(holder));
        contentBuilder.accept(root);
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    // ───── Serialized handlers (tanks) ─────
    protected void loadSerializedHandlers(ValueInput input) {
        for (int i = 0; i < tanks.size(); i++) {
            final int idx = i;
            input.read("tank" + i, FluidStack.OPTIONAL_CODEC).ifPresent(fluid -> {
                tanks.get(idx).setFluid(fluid);
            });
        }
    }

    protected void saveSerializedHandlers(ValueOutput output) {
        for (int i = 0; i < tanks.size(); i++) {
            output.store("tank" + i, FluidStack.OPTIONAL_CODEC, tanks.get(i).getFluid());
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

    // Guard flag: true when the chunk is being unloaded (setRemoved should NOT drop contents in that case).
    private transient boolean chunkUnloading = false;
    // Guard flag: set by BaseMachineBlock.destroy() to prevent double-drop when
    // player breaking also triggers setRemoved().
    private transient boolean destroyDropsHandled = false;

    /**
     * Called by {@code BaseMachineBlock.destroy()} to signal that drops have
     * already been handled, so {@link #setRemoved()} should not drop again.
     */
    public void markDestroyDropsHandled() {
        this.destroyDropsHandled = true;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.chunkUnloading = true;
    }

    @Override
    public void setRemoved() {
        // Drop contents when the block is removed/replaced (NOT during chunk unload, where
        // onChunkUnloaded() is called first and sets the guard flag).
        // Also skip if BaseMachineBlock.destroy() already handled the drops.
        if (!chunkUnloading && !destroyDropsHandled && level != null && !level.isClientSide()) {
            dropAllContents();
        }
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        this.chunkUnloading = false;
        this.destroyDropsHandled = false;
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

    /**
     * Apply a percent-based throughput/storage bonus to the machine.
     * <p>
     * The slotIncrease parameter is deprecated/ignored — all 6 upgrade slots
     * are permanently unlocked. This parameter is kept in the signature for
     * backward compatibility with existing {@link UpgradeItem#effect} calls
     * that pass a slot increase value.
     *
     * @param percent      the throughput multiplier (0.2 = +20%, -0.1 = -10%)
     * @param slotIncrease ignored; kept for API compatibility
     * @return true
     */
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
        // slotIncrease is ignored: all 6 slots are permanently unlocked
        upgradeSize = MAX_UPGRADE_SLOTS;
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
