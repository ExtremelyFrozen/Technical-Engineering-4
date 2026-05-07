package com.modularmc.ten.core.blockentity.machine;

import com.modularmc.ten.lib.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.lib.option.IngredientType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class BeaconBlockEntity extends RadiusMachineBlockEntity {

    public BeaconBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(300);
        initialRadius = 32;
        radius = 32;
    }

    @Override
    public int inventorySize() { return 1; }

    @Override
    public IngredientType slotType(int slot) { return IngredientType.INPUT; }

    @Override
    public boolean valid(int slot, ItemStack stack) { return true; }

    @Override
    public IngredientType tankType(int tank) { return IngredientType.IGNORE; }
    @Override
    public boolean valid(int slot, FluidStack stack) { return true; }

    @Override
    public void applyEffect() {
        if (level == null) return;
        AABB box = (new AABB(worldPosition)).inflate(radius);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        PotionContents potion = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (potion == PotionContents.EMPTY) return;

        var effects = potion.getAllEffects();
        if (!effects.iterator().hasNext()) return;

        for (Player player : players) {
            effects.forEach(effect -> {
                player.addEffect(new MobEffectInstance(effect.getEffect(), 400, 0, true, true));
            });
        }
    }

    @Override
    public double effectInterval() { return 10; }

    @Override
    public boolean conditionStart() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }
}
