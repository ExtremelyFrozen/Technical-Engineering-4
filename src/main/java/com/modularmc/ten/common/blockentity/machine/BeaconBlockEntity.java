package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupPotion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

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
    public int machineType() {
        return MachineType.BEACON;
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
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 输入槽 (79,32)：中心 x=88 与下方宽进度条竖中线 (48+40) 对齐；y=32 中心 41 符合主区中线
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 79, 32));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // 宽进度条 (48,57)：输入槽在上、进度条在下，上下平行（垂直分离 7px），而非同一水平带平齐
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 57, true));
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this));
        });
    }

    /** 生效范围：以自身为中心的 AABB 范围框（inflate(radius)，与 applyEffect 查询框一致）。 */
    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        return List.of(new net.minecraft.world.phys.AABB(worldPosition).inflate(radius));
    }

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

        int amplifier = hasUpgrade(LevelupPotion.class) ? 1 : 0;
        int B = getLockedBatchSize();

        for (Player player : players) {
            effects.forEach(effect -> {
                // 时长 × B：long 中间量 + int 安全钳位（26.1.2 对齐，能耗×B 时长兑现）
                long durationLong = 400L * B;
                int durationClamped = (int) Math.min(durationLong, Integer.MAX_VALUE);
                player.addEffect(new MobEffectInstance(effect.getEffect(), durationClamped, amplifier, true, true));
            });
        }
    }

    @Override
    public double effectInterval() {
        return 10;
    }

    @Override
    public boolean conditionStart() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }
}
