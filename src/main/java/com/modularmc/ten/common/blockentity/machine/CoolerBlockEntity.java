package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.EffectMachineBlockEntity;
import com.modularmc.ten.api.option.Coolant;
import com.modularmc.ten.api.option.Coolants;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.List;

/**
 * 冷却器（Cooler）。
 * 槽 0 放冷却剂（冰/浮冰/蓝冰等，见 {@link Coolants}）。每 intervalSeconds 秒，
 * 减少正面一格机器的 reductionTicks tick 耗时（推进 progress）；冷却剂按 uses 次消耗。
 */
public class CoolerBlockEntity extends EffectMachineBlockEntity {

    /** 当前冷却剂剩余作用次数。0 表示需要新冷却剂或刚放入待初始化。 */
    @Persisted
    public int usesRemaining = 0;

    /** 当前冷却剂物品的注册 key（BuiltInRegistries.ITEM）。换冷却剂时据此重置 usesRemaining。 */
    @Persisted
    public String coolantItemKey = "";

    public CoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
    }

    @Override public int machineType() { return MachineType.COOLER; }
    @Override public int inventorySize() { return 1; }
    @Override public IngredientType slotType(int slot) { return IngredientType.INPUT; }
    @Override public boolean valid(int slot, ItemStack stack) { return Coolants.isCoolant(stack); }
    @Override public IngredientType tankType(int tank) { return IngredientType.IGNORE; }
    @Override public boolean valid(int slot, FluidStack stack) { return true; }

    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        Direction facing = getFacing();
        int mx = worldPosition.getX(), my = worldPosition.getY(), mz = worldPosition.getZ();
        int x1, y1, z1, x2, y2, z2;
        switch (facing) {
            case NORTH -> { x1 = mx; x2 = mx + 1; y1 = my; y2 = my + 1; z1 = mz - 1; z2 = mz; }
            case SOUTH -> { x1 = mx; x2 = mx + 1; y1 = my; y2 = my + 1; z1 = mz + 1; z2 = mz + 2; }
            case EAST  -> { x1 = mx + 1; x2 = mx + 2; y1 = my; y2 = my + 1; z1 = mz; z2 = mz + 1; }
            default    -> { x1 = mx - 1; x2 = mx; y1 = my; y2 = my + 1; z1 = mz; z2 = mz + 1; }
        }
        return List.of(new net.minecraft.world.phys.AABB(x1, y1, z1, x2, y2, z2));
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TEN.id("textures/gui/machine_gui.png"), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 79, 32));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 8, 18, 14, 46, 0, 0, true));
            // 主进度条（冷却周期）同模拟信标位置
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 48, 57, 80, 5, 97, 0, false));
        });
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null || level.isClientSide()) return;
        ItemStack coolant = itemHandler.getStackInSlot(0);
        Coolant data = Coolants.get(coolant);
        if (data == null) {
            coolantItemKey = "";
            usesRemaining = 0;
            setChanged();
            return;
        }
        String key = BuiltInRegistries.ITEM.getKey(coolant.getItem()).toString();
        if (!key.equals(coolantItemKey)) {
            coolantItemKey = key;
            usesRemaining = data.uses();
            setChanged();
            return;
        }
        if (usesRemaining <= 0) {
            usesRemaining = data.uses();
            setChanged();
            return;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.relative(getFacing()));
        if (be instanceof CmMachineBlockEntity targetMachine && targetMachine.maxProgress > 0) {
            int delta = safeMultiply(data.reductionTicks(), getLockedBatchSize());
            targetMachine.progress = Math.min(targetMachine.maxProgress, targetMachine.progress + delta);
            targetMachine.setChanged();
            usesRemaining--;
            if (usesRemaining <= 0) {
                coolant.shrink(1);
                itemHandler.setStackInSlot(0, coolant);
                usesRemaining = 0;
            }
            setChanged();
        }
    }

    @Override
    public double effectInterval() {
        Coolant data = currentCoolant();
        return data != null ? data.intervalSeconds() : Integer.MAX_VALUE;
    }

    @Override
    public boolean conditionStart() {
        if (level == null || itemHandler == null) return false;
        if (!Coolants.isCoolant(itemHandler.getStackInSlot(0))) return false;
        BlockEntity be = level.getBlockEntity(worldPosition.relative(getFacing()));
        return be instanceof CmMachineBlockEntity targetMachine && targetMachine.maxProgress > 0 && targetMachine.isActive();
    }

    /** 冷却剂进度（GUI 显示）：当前剩余作用次数 / 总作用次数。 */
    public float getCoolantPercent() {
        Coolant data = currentCoolant();
        if (data == null || usesRemaining <= 0) return 0f;
        return (float) usesRemaining / data.uses();
    }

    private Coolant currentCoolant() {
        if (itemHandler == null) return null;
        return Coolants.get(itemHandler.getStackInSlot(0));
    }
}