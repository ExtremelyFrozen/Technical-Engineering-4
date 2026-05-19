package com.modularmc.ten.test.ui;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.test.util.TENGameTestHelpers;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

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
        ServerPlayer player = TENGameTestHelpers.makeTickingMockServerPlayerInLevel(helper, GameType.CREATIVE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        for (int index = 0; index < machineBlocks.size(); index++) {
            BlockPos machinePos = new BlockPos(index % 5, 1, index / 5);
            helper.setBlock(machinePos, machineBlocks.get(index));
            helper.useBlock(machinePos, player);

            if (!(player.containerMenu instanceof ModularUIContainerMenu)) {
                helper.fail("Expected machine interaction to open ModularUIContainerMenu for " + machineBlocks.get(index).getName().getString() + ", got " + player.containerMenu.getClass().getName());
                return;
            }
            player.closeContainer();
        }
        helper.succeed();
    }
}
