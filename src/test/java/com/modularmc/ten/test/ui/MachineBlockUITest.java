package com.modularmc.ten.test.ui;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.testframework.annotation.TestHolder;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(TEN.MOD_ID)
public class MachineBlockUITest {

    @TestHolder(value = TEN.MOD_ID + ":machine_block_opens_modular_ui", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void machineBlockOpensModularUI(GameTestHelper helper) {
        List<Block> machineBlocks = List.of(
                TENBlocks.MACHINE_SMELTER.get(),
                TENBlocks.MACHINE_PULVERIZER.get(),
                TENBlocks.MACHINE_COMPRESSOR.get(),
                TENBlocks.MACHINE_REFINER.get(),
                TENBlocks.MACHINE_INDUCTION_FURNACE.get(),
                TENBlocks.MACHINE_PSIONICANT.get(),
                TENBlocks.MACHINE_BEACON.get(),
                TENBlocks.MACHINE_MOB_RIPPER.get(),
                TENBlocks.MACHINE_QUARRY.get(),
                TENBlocks.MACHINE_ENCHFLU.get(),
                TENBlocks.MACHINE_CONDENSER.get(),
                TENBlocks.MACHINE_FARM.get(),
                TENBlocks.ENGINE_EXTRACTION.get(),
                TENBlocks.ENGINE_METAL.get(),
                TENBlocks.ENGINE_BIOMASS.get(),
                TENBlocks.ENGINE_SOLAR.get(),
                TENBlocks.CELL.get(),
                TENBlocks.CREATIVE_CELL.get());
        var player = helper.makeMockPlayer(GameType.CREATIVE);

        for (int index = 0; index < machineBlocks.size(); index++) {
            BlockPos machinePos = new BlockPos(index % 5, 1, index / 5);
            helper.setBlock(machinePos, machineBlocks.get(index));
            var block = helper.getBlockState(machinePos).getBlock();
            if (!(block instanceof BaseMachineBlock baseMachineBlock)) {
                helper.fail("Expected BaseMachineBlock for " + machineBlocks.get(index).getName().getString());
                return;
            }
            var holder = new BlockUIMenuType.BlockUIHolder(baseMachineBlock, player, helper.absolutePos(machinePos), helper.getBlockState(machinePos));
            var ui = baseMachineBlock.createUI(holder);
            if (ui == null) {
                helper.fail("Expected machine block to create ModularUI for " + machineBlocks.get(index).getName().getString());
                return;
            }
        }
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":plain_pipe_does_not_open_ui", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void plainPipeDoesNotOpenUI(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 1, 1);
        helper.setBlock(pipePos, TENBlocks.PIPE.get());
        PipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(pipe != null && !pipe.hasUi(), "Expected plain pipe to report hasUi=false");
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":filtered_pipes_open_ui", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void filteredPipesOpenUI(GameTestHelper helper) {
        List<Block> pipes = List.of(TENBlocks.PIPE_WHITE.get(), TENBlocks.PIPE_BLACK.get());
        var player = helper.makeMockPlayer(GameType.CREATIVE);

        for (int index = 0; index < pipes.size(); index++) {
            BlockPos pipePos = new BlockPos(index, 1, 1);
            helper.setBlock(pipePos, pipes.get(index));
            PipeBlockEntity pipe = helper.getBlockEntity(pipePos);
            if (pipe == null || !pipe.hasUi()) {
                helper.fail("Expected filtered pipe to report hasUi=true for " + pipes.get(index).getName().getString());
                return;
            }
            var block = helper.getBlockState(pipePos).getBlock();
            if (!(block instanceof BaseMachineBlock baseMachineBlock)) {
                helper.fail("Expected BaseMachineBlock for filtered pipe " + pipes.get(index).getName().getString());
                return;
            }
            var holder = new BlockUIMenuType.BlockUIHolder(baseMachineBlock, player, helper.absolutePos(pipePos), helper.getBlockState(pipePos));
            var ui = baseMachineBlock.createUI(holder);
            if (ui == null) {
                helper.fail("Expected filtered pipe to create ModularUI for " + pipes.get(index).getName().getString());
                return;
            }
        }
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":energy_cables_do_not_open_ui", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void energyCablesDoNotOpenUI(GameTestHelper helper) {
        List<Block> cables = List.of(
                TENBlocks.CABLE.get(),
                TENBlocks.CABLE_QUARTZ.get(),
                TENBlocks.CABLE_AZURE.get(),
                TENBlocks.CABLE_STAR.get());

        for (int index = 0; index < cables.size(); index++) {
            BlockPos cablePos = new BlockPos(index, 1, 1);
            helper.setBlock(cablePos, cables.get(index));
            CableBlockEntity cable = helper.getBlockEntity(cablePos);
            helper.assertTrue(cable != null && !cable.hasUi(), "Expected energy cable to report hasUi=false for " + cables.get(index).getName().getString());
        }
        helper.succeed();
    }

    /** 升级槽的快捷移动准入跟随面板展开态，且展开态按玩家隔离。 */
    @TestHolder(value = TEN.MOD_ID + ":upgrade_slot_quick_move_follows_panel_state", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void upgradeSlotQuickMoveFollowsPanelState(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, TENBlocks.MACHINE_PULVERIZER.get());
        CmMachineBlockEntity machine = helper.getBlockEntity(pos);
        if (machine == null) {
            helper.fail("Expected a machine block entity for pulverizer");
            return;
        }
        var block = helper.getBlockState(pos).getBlock();
        if (!(block instanceof BaseMachineBlock baseMachineBlock)) {
            helper.fail("Expected BaseMachineBlock for pulverizer");
            return;
        }
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var holder = new BlockUIMenuType.BlockUIHolder(baseMachineBlock, player, helper.absolutePos(pos), helper.getBlockState(pos));
        var modularUI = baseMachineBlock.createUI(holder);

        List<ItemSlot> allSlots = new ArrayList<>();
        collectItemSlots(modularUI.ui.getRootElement(), allSlots);
        List<ItemSlot> upgradeSlots = allSlots.stream()
                .filter(s -> bindsHandler(s, machine.upgradeHandler))
                .toList();
        helper.assertTrue(upgradeSlots.size() == CmMachineBlockEntity.MAX_UPGRADE_SLOTS,
                "Expected 6 upgrade slots in the machine UI");
        if (upgradeSlots.size() != CmMachineBlockEntity.MAX_UPGRADE_SLOTS) {
            return;
        }
        // 服务端 UI 树不做样式计算，SlotStyle.quickMovePriority 回退为 0；
        // 容器按 UI 树遍历顺序注册槽位，所以升级槽必须先于所有机器槽。
        int lastUpgradeIndex = -1;
        int firstMachineIndex = Integer.MAX_VALUE;
        for (int i = 0; i < allSlots.size(); i++) {
            if (bindsHandler(allSlots.get(i), machine.upgradeHandler)) {
                lastUpgradeIndex = i;
            } else if (bindsHandler(allSlots.get(i), machine.itemHandler)) {
                firstMachineIndex = Math.min(firstMachineIndex, i);
            }
        }
        helper.assertTrue(firstMachineIndex < Integer.MAX_VALUE && lastUpgradeIndex < firstMachineIndex,
                "升级槽必须先于机器槽注册，服务端容器才会先尝试升级槽");

        ItemStack upgrade = TENItems.AUGMENTED_LEVELUP.get().getDefaultInstance();
        Slot firstUpgradeSlot = upgradeSlots.get(0).getSlot();
        machine.setUpgradePanelOpenFor(player, false);
        helper.assertTrue(!firstUpgradeSlot.mayPlace(upgrade), "收起时升级槽不得接收快捷移动");
        machine.setUpgradePanelOpenFor(player, true);
        helper.assertTrue(firstUpgradeSlot.mayPlace(upgrade), "展开时升级槽应接收快捷移动");
        var otherPlayer = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertTrue(!machine.isUpgradePanelOpenFor(otherPlayer), "展开态不得串给其他玩家");
        machine.setUpgradePanelOpenFor(player, false);
        helper.assertTrue(!firstUpgradeSlot.mayPlace(upgrade), "重新收起后升级槽应再次排除");

        // 端到端：真实容器执行 quickMoveStack，验证展开态下的实际落点
        var menu = holder.createMenu(0, player.getInventory(), player);
        int playerSlotIndex = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot candidate = menu.slots.get(i);
            if (candidate.container == player.getInventory() && candidate.getContainerSlot() == 0) {
                playerSlotIndex = i;
                break;
            }
        }
        helper.assertTrue(playerSlotIndex >= 0, "Expected a player inventory slot in the machine menu");
        if (playerSlotIndex < 0) {
            return;
        }
        menu.slots.get(playerSlotIndex).set(upgrade.copy());
        menu.quickMoveStack(player, playerSlotIndex);
        helper.assertTrue(machine.upgradeHandler.getStackInSlot(0).isEmpty(),
                "收起时快捷移动不得把升级送进升级槽");

        machine.setUpgradePanelOpenFor(player, true);
        menu.slots.get(playerSlotIndex).set(upgrade.copy());
        menu.quickMoveStack(player, playerSlotIndex);
        helper.assertTrue(!machine.upgradeHandler.getStackInSlot(0).isEmpty(),
                "展开时快捷移动应把升级送进升级槽");
        helper.succeed();
    }

    private static boolean bindsHandler(ItemSlot itemSlot, IItemHandler handler) {
        return itemSlot.getSlot() instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() == handler;
    }

    private static void collectItemSlots(UIElement element, List<ItemSlot> out) {
        if (element instanceof ItemSlot itemSlot) {
            out.add(itemSlot);
        }
        for (UIElement child : element.getChildren()) {
            collectItemSlots(child, out);
        }
    }
}
