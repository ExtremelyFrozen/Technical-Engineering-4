package com.modularmc.ten.common.blockentity.machine;

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
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.List;

/**
 * 冷却器（Cooler）。
 * 槽 0 放冷却剂（冰/浮冰/蓝冰等，见 {@link Coolants}）。每 intervalSeconds 秒，
 * 减少正面一格机器的 reductionTicks tick 耗时（推进 progress）；冷却剂按 uses 次消耗。
 */
public class CoolerBlockEntity extends EffectMachineBlockEntity {

    /**
     * 当前冷却剂剩余作用次数。0 表示需要新冷却剂。
     * 进度栏在客户端求值，故必须 @DescSynced（LDLib2 只同步 @DescSynced/RPC，见
     * CmMachineBlockEntity#syncUpgradesToClients 的探针结论）。
     */
    @Persisted
    @DescSynced
    public int usesRemaining = 0;

    /** 当前冷却剂一份的总作用次数；0 表示槽内无冷却剂。与 usesRemaining 一起构成进度栏数据源。 */
    @DescSynced
    public int coolantTotalUses = 0;

    /** 当前冷却剂物品的注册 key（BuiltInRegistries.ITEM）。换冷却剂时据此重置 usesRemaining。 */
    @Persisted
    public String coolantItemKey = "";

    public CoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
    }

    @Override
    public int machineType() {
        return MachineType.COOLER;
    }

    @Override
    public int inventorySize() {
        return 1;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.INPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return Coolants.isCoolant(stack);
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
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        Direction facing = getFacing();
        int mx = worldPosition.getX(), my = worldPosition.getY(), mz = worldPosition.getZ();
        int x1, y1, z1, x2, y2, z2;
        switch (facing) {
            case NORTH -> {
                x1 = mx;
                x2 = mx + 1;
                y1 = my;
                y2 = my + 1;
                z1 = mz - 1;
                z2 = mz;
            }
            case SOUTH -> {
                x1 = mx;
                x2 = mx + 1;
                y1 = my;
                y2 = my + 1;
                z1 = mz + 1;
                z2 = mz + 2;
            }
            case EAST -> {
                x1 = mx + 1;
                x2 = mx + 2;
                y1 = my;
                y2 = my + 1;
                z1 = mz;
                z2 = mz + 1;
            }
            default -> {
                x1 = mx - 1;
                x2 = mx;
                y1 = my;
                y2 = my + 1;
                z1 = mz;
                z2 = mz + 1;
            }
        }
        return List.of(new net.minecraft.world.phys.AABB(x1, y1, z1, x2, y2, z2));
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 输入槽（冷却剂）居中，同模拟信标
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 79, 32));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // 冷却剂消耗进度栏：主进度条上方平行布置（progress_bar_wide_coolant）
            root.addChild(TENMachineBlockUIFactory.coolantProgressBar(this, 48, 50));
            // 主进度条（冷却周期）：同模拟信标位置
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 57, true));
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this));
        });
    }

    /**
     * 服务端每 tick 刷新冷却剂状态与客户端镜像。放入/更换冷却剂当刻即完成次数初始化
     * （旧实现把初始化放在 applyEffect 里，代价是白丢一个完整周期），取出则立即清零。
     */
    @Override
    public void doBaseData() {
        super.doBaseData();
        ItemStack stack = itemHandler == null ? ItemStack.EMPTY : itemHandler.getStackInSlot(0);
        Coolant data = Coolants.get(stack);
        if (data == null) {
            coolantItemKey = "";
            usesRemaining = 0;
            coolantTotalUses = 0;
        } else {
            String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!key.equals(coolantItemKey) || usesRemaining <= 0) {
                coolantItemKey = key;
                usesRemaining = data.uses();
            }
            coolantTotalUses = data.uses();
        }
        markDirty("usesRemaining");
        markDirty("coolantTotalUses");
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null || level.isClientSide()) return;
        ItemStack coolant = itemHandler.getStackInSlot(0);
        Coolant data = Coolants.get(coolant);
        if (data == null || usesRemaining <= 0) {
            return;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.relative(getFacing()));
        if (!(be instanceof CmMachineBlockEntity targetMachine && targetMachine.maxProgress > 0)) {
            return;
        }
        int delta = safeMultiply(data.reductionTicks(), getLockedBatchSize());
        // 经钩子推进：多流水线机器（熔炼机）会自行把 tick 分派到各条 lane。
        targetMachine.advanceProgress(delta);
        usesRemaining--;
        if (usesRemaining <= 0) {
            coolant.shrink(1);
            itemHandler.setStackInSlot(0, coolant);
        }
        setChanged();
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

    /** 冷却剂进度（GUI 显示）：剩余次数 / 总次数。两个字段都是服务端写入的 @DescSynced 镜像。 */
    public float getCoolantPercent() {
        if (coolantTotalUses <= 0 || usesRemaining <= 0) return 0f;
        return (float) Math.min(usesRemaining, coolantTotalUses) / coolantTotalUses;
    }

    private Coolant currentCoolant() {
        if (itemHandler == null) return null;
        return Coolants.get(itemHandler.getStackInSlot(0));
    }
}
