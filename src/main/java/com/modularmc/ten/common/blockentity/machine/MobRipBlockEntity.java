package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.List;

public class MobRipBlockEntity extends RadiusMachineBlockEntity {

    public MobRipBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
        initialRadius = 8;
        radius = 8;
    }

    @Override
    public int machineType() {
        return MachineType.MOB_RIPPER;
    }

    @Override
    public int inventorySize() {
        return 13;
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot == 0 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        if (slot == 0) {
            return stack.has(DataComponents.TOOL);
        }
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
        // 布局与 Quarry 逐坐标对齐（用户确认两者基本一致）：
        // 输入槽 (43,34)、12 输出槽 3×4 网格 (79/97/115/133 × 16/34/52)。
        // 输入槽中心 y=43 与输出网格第二行中心对齐，x 间距 18 不重叠。
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 79, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 133, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 5, 79, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 6, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 7, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 8, 133, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 9, 79, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 11, 115, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 12, 133, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // 输出网格底 y=70，进度条 y=74（间距 4）避免贴边；中心 x=88 与输出列中心对齐
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 74, true));
        });
    }

    @Override
    public void applyEffect() {
        if (level == null) return;
        int B = getLockedBatchSize();

        // P3-T1c: Check if a weapon was present at operation start.
        // If weapon WAS valid and breaks during iterations, stop remaining loop
        // rather than falling back to fist damage. If no weapon was ever present,
        // preserve original semantics (allow operation without weapon).
        boolean weaponWasPresentAtStart = hasValidWeapon();

        for (int i = 0; i < B; i++) {
            // P3-T1c: If weapon was originally present but now empty/broken → stop
            if (weaponWasPresentAtStart && !hasValidWeapon()) {
                break;
            }
            if (!tryHurtOneEntity()) {
                // If no valid entity found, continue looping — other iterations
                // may still find valid targets (entities can be re-selected)
                continue;
            }
        }
    }

    /**
     * Check whether the weapon in slot 0 is still valid for use.
     * A weapon is considered valid if it exists, is not empty, and has a TOOL component.
     * P3-T1c: If the weapon was originally present but has broken (empty/damaged),
     * remaining iterations should stop rather than falling back to fist damage.
     *
     * @return true if a valid weapon is available
     */
    private boolean hasValidWeapon() {
        ItemStack weapon = itemHandler.getStackInSlot(0);
        return !weapon.isEmpty() && weapon.has(DataComponents.TOOL);
    }

    /**
     * Try to select and damage one entity within range.
     *
     * @return true if an entity was damaged, false if no valid target found
     */
    private boolean tryHurtOneEntity() {
        if (level == null) return false;
        AABB box = (new AABB(worldPosition)).inflate(radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        // Filter out dead entities and creative players
        entities.removeIf(e -> !e.isAlive() || (e instanceof Player p && p.isCreative()));
        if (entities.isEmpty()) return false;

        LivingEntity target = SafeOperationHelper.randomInCollection(entities);
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player p && p.isCreative()) return false;

        ItemStack weapon = itemHandler.getStackInSlot(0);
        float damage = 0.5f;
        if (!weapon.isEmpty() && weapon.has(DataComponents.TOOL)) {
            damage = 1.0f;
        }
        target.hurt(target.damageSources().cactus(), damage);
        // Only damage the weapon item if it actually exists (not ItemStack.EMPTY)
        if (!weapon.isEmpty()) {
            ItemNBTHelper.damage(weapon, level, 1);
        }
        return true;
    }

    @Override
    public double effectInterval() {
        return 3;
    }
}
