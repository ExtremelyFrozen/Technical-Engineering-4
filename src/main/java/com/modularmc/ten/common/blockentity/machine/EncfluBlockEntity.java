package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class EncfluBlockEntity extends ProcessingMachineBlockEntity {

    public EncfluBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(100);
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(1000));
    }

    @Override
    public int machineType() {
        return MachineType.ENCHANTMENT_FLUSHER;
    }

    @Override
    public int inventorySize() {
        return 3;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot == 1) {
            return IngredientType.INPUT;
        }
        if (slot == 0) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty() && !stack.isEnchanted()) {
                return IngredientType.OUTPUT;
            }
            return IngredientType.INPUT;
        }
        if (slot == 2) {
            return IngredientType.OUTPUT;
        }
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        if (slot == 1) {
            return stack.isEnchantable() || stack.is(Items.BOOK);
        }
        if (slot == 0) {
            return stack.isEnchanted();
        }
        return false;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 2 输入竖排（39,14 / 39,50，块 14~68 中线 41 与能量条对齐），输出大槽
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 39, 14));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 39, 50));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 2, 113, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // mini 装饰（静态整件 8x54）：紧贴输入槽右缘，标示上下输入槽流向（目标在下），不承载进度
            root.addChild(TENMachineBlockUIFactory.verticalProgressMini(39, 14, 18, TENConstants.PROGRESS_ARROW_MINI_ENCFLU_BG, false));
            // 进度箭头（modular smelter 素材 22x16）：encflu 映射 PROGRESS_ARROW_SMELTER 素材族
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 76, 33, true));
        });
    }

    @Override
    public int baseTickTime() {
        return 800;
    }

    @Override
    public boolean conditionStart() {
        ItemStack tool = itemHandler.getStackInSlot(0);
        ItemStack target = itemHandler.getStackInSlot(1);
        ItemStack output = itemHandler.getStackInSlot(2);

        // 基础输入校验——无效则清锁停机
        if (!tool.isEnchanted() || !(target.isEnchantable() || target.is(Items.BOOK)) || !output.isEmpty()) {
            clearLockedBatch();
            return false;
        }

        // 无锁时才计算并锁定 B + maxProgress
        if (!hasLockedBatch()) {
            maxProgress = Math.max(1, (int) Math.ceil(baseTickTime() * durationMultiplier));
            lockMaxProgressForNewOperation(maxProgress);

            int B_theory = 1 + batch;

            // B_byTool: 工具槽不可堆叠，仅支持 1 单位
            int B_byTool = 1;

            // B_byOutput: 输出槽必须为空
            int B_byOutput = output.isEmpty() ? B_theory : 0;

            // B_byTank: floor(罐剩余容量 / 每组 XP)
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
            int xpPerGroup = enchantments.isEmpty() ? 25 : Math.max(1, enchantments.size() * 25);
            int B_byTank = 0;
            if (!tanks.isEmpty()) {
                int availTank = tanks.get(0).getCapacity() - tanks.get(0).getFluidAmount();
                B_byTank = xpPerGroup > 0 ? availTank / xpPerGroup : 0;
            }

            // B_byEnergy: 当前储能可持续的 tick 数
            int baseFe = Math.max(1, getActualEfficiency());
            int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFe : 0;

            // B_byTool=1 保证实际 B=1（工具不可堆叠，无批量复制）
            if (!validateAndLockB(B_theory, B_byTool, Integer.MAX_VALUE, Math.min(B_byOutput, B_byTank), B_byEnergy)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean cooking() {
        // 进度推进前检查输出槽与罐容量（罐满时停滞，不消耗目标空转）
        ItemStack output = itemHandler.getStackInSlot(2);
        if (!output.isEmpty()) return true; // 输出槽被占 → 阻塞

        ItemStack tool = itemHandler.getStackInSlot(0);
        if (!tool.isEnchanted()) return true; // 工具耗尽 → 阻塞

        // 检查 XP 罐至少能容纳一组
        if (!tanks.isEmpty()) {
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
            int xpPerGroup = enchantments.isEmpty() ? 25 : Math.max(1, enchantments.size() * 25);
            FluidStack xpFluid = new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_XP.getSource(), xpPerGroup);
            int filled = tanks.get(0).fill(xpFluid, IFluidHandler.FluidAction.SIMULATE);
            if (filled < xpPerGroup) return true; // 罐满 → 阻塞
        }

        return false;
    }

    @Override
    public void onCookFinish() {
        // ── Phase 0: 批量大小 ──
        int B = getLockedBatchSize();

        // ── Phase 1: 读快照（副本）──
        ItemStack tool = itemHandler.getStackInSlot(0).copy();
        ItemStack target = itemHandler.getStackInSlot(1).copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(2).copy();

        if (tool.isEmpty() || target.isEmpty()) {
            return;
        }

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
        if (enchantments.isEmpty()) {
            return;
        }

        // ── Phase 2: 复验输出槽为空 ──
        if (!currentOutput.isEmpty()) {
            return;
        }

        // ── Phase 3: 无副作用构建产出 ──
        ItemStack output;
        if (target.is(Items.BOOK)) {
            output = Items.ENCHANTED_BOOK.getDefaultInstance();
        } else {
            output = target.copyWithCount(1);
        }
        EnchantmentHelper.setEnchantments(output, enchantments);

        int xpAmount = Math.max(1, enchantments.size() * 25);

        // 剥离附魔后的工具（物品保留）：用 set(EMPTY) 而非 remove(ENCHANTMENTS)。
        // 1.21.1 isEnchantable() 语义 = has(ENCHANTABLE) && get(ENCHANTMENTS)!=null && empty；
        // remove 会让 get() 返回 null（非 EMPTY），isEnchantable 恒 false → 工具永久失去可附魔性；
        // set EMPTY 保留组件存在性，与 effectiveToolForDrops 的对照实现一致。
        // NOTE: 不可额外移除 STORED_ENCHANTMENTS——那是储存附魔（附魔书类）专属组件，
        // 非 book 物品误删该组件会破坏物品组件表，导致后续无法再附魔（可附魔性判定失败）。
        ItemStack strippedTool = tool.copy();
        strippedTool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        // 消耗 B 个目标后的数量
        ItemStack targetAfter = target.copy();
        targetAfter.shrink(B);
        if (targetAfter.getCount() < 0) {
            targetAfter = ItemStack.EMPTY;
        }

        // ── Phase 4: 复验 XP 罐空间（SIMULATE）──
        if (!tanks.isEmpty()) {
            int totalXp = xpAmount * B;
            FluidStack totalXpFluid = new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_XP.getSource(), totalXp);
            int filled = tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.SIMULATE);
            if (filled < totalXp) {
                return;
            }
        }

        // ── Phase 5: 保存回滚快照 ──
        ItemStack slot0Snapshot = itemHandler.getStackInSlot(0).copy();
        ItemStack slot1Snapshot = itemHandler.getStackInSlot(1).copy();
        ItemStack slot2Snapshot = itemHandler.getStackInSlot(2).copy();
        FluidStack tankSnapshot = tanks.isEmpty() ? FluidStack.EMPTY : tanks.get(0).getFluid().copy();

        try {
            // ── Phase 6: 先消耗（剥离工具附魔一次，消耗 B 目标）──
            itemHandler.setStackInSlot(0, strippedTool);
            itemHandler.setStackInSlot(1, targetAfter);

            // ── Phase 7: 后产出（整批 1 份输出 + B×XP 流体）──
            ItemStack batchOutput = output.copyWithCount(B);
            itemHandler.setStackInSlot(2, batchOutput);

            if (!tanks.isEmpty()) {
                FluidStack totalXpFluid = new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_XP.getSource(), xpAmount * B);
                tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.EXECUTE);
            }

            clearLockedBatch();
        } catch (Exception e) {
            // ── 任一异常回滚 ──
            itemHandler.setStackInSlot(0, slot0Snapshot);
            itemHandler.setStackInSlot(1, slot1Snapshot);
            itemHandler.setStackInSlot(2, slot2Snapshot);
            if (!tanks.isEmpty()) {
                tanks.get(0).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                if (!tankSnapshot.isEmpty()) {
                    tanks.get(0).fill(tankSnapshot, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            throw new RuntimeException("Encflu onCookFinish failed and rolled back", e);
        }
    }
}
