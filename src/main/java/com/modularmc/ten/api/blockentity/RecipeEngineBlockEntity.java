package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 引擎基类：由燃料匹配（MatchFuel）或天空光照驱动产能，
 * 能量面强制 OUT 主动向相邻机器推送。
 */
public abstract class RecipeEngineBlockEntity extends EngineBlockEntity {

    public SlotInfo slotInfo = new SlotInfo(0, 0, 0, 0, 0, 0, 0, 0);
    protected FormsCombinedRecipe currentRecipe;

    public RecipeEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void setSlotInfo(SlotInfo slotInfo){
        this.slotInfo = slotInfo;
    }

    public abstract FormsCombinedRecipe findRecipe();

    @Override
    public void process() {
        if (!signalAllowRun() || !energyAllowRun()) return;

        if (fuel > 0) {
            energyStorage.receiveEnergy(getActualEfficiency(), false);
            fuel = Math.max(fuel - efficientIn, 0);
            setActive(true);
        } else if (conditionStart()) {
            int fuelVal = Optional.of(currentRecipe.time()).get();
            if (fuelVal > 0) {
                var plan = RecipeMachineBlockEntity.InputConsumptionPlan.build(
                    currentRecipe, slotInfo, itemHandler, tanks,
                    this::slotType, this::tankType, 1);
                if (plan == null || !plan.execute(itemHandler, tanks)) {
                    // Consumption failed internally — already rolled back by plan
                    TEN.LOGGER.error("InputConsumptionPlan.execute failed for recipe={}", currentRecipe.getId());
                    return;
                }
                fuel = fuelVal;
                maxFuel = fuelVal;
                setActive(true);
                return;
            }
        }
        setActive(false);
    }

    public int matchFuel(ItemStack stack, boolean simulate){
        return 0;
    }

    @Override
    protected boolean conditionStart() {
        if (level == null) return false;

        if (currentRecipe != null && currentRecipe.matches(itemHandler, tanks, this::slotType, this::tankType)) return true;

        currentRecipe = findRecipe();

        return currentRecipe != null;
    }
}
