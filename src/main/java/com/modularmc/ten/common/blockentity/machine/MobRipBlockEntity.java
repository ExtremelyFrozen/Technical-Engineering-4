package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

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
        return 1;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.INPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return stack.getItem() instanceof TieredItem || stack.getItem() instanceof SwordItem;
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
    public void applyEffect() {
        if (level == null) return;
        AABB box = (new AABB(worldPosition)).inflate(radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        if (entities.isEmpty()) return;

        LivingEntity target = SafeOperationHelper.randomInCollection(entities);
        if (target instanceof Player p && p.isCreative()) return;

        ItemStack weapon = itemHandler.getStackInSlot(0);
        float damage = 0.5f;
        if (!weapon.isEmpty()) {
            if (weapon.getItem() instanceof SwordItem sword) {
                damage = sword.getDamage(weapon);
            } else if (weapon.getItem() instanceof TieredItem tiered) {
                damage = tiered.getTier().getAttackDamageBonus();
            }
        }
        target.hurt(target.damageSources().cactus(), damage);
        ItemNBTHelper.damage(weapon, level, 1);
    }

    @Override
    public double effectInterval() {
        return 3;
    }
}
