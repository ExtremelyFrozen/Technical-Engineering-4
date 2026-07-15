package com.modularmc.ten.test.ui;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import net.neoforged.testframework.gametest.GameTest;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;

import java.util.List;

@TestHolder(value = TEN.MOD_ID + ":machine_block_ui")
public class MachineBlockUITest {

    @GameTest(required = true)
    public void machineBlockOpensModularUI(ExtendedGameTestHelper helper) {
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

    @GameTest(required = true)
    public void plainPipeDoesNotOpenUI(ExtendedGameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 1, 1);
        helper.setBlock(pipePos, TENBlocks.PIPE.get());
        PipeBlockEntity pipe = helper.getBlockEntity(pipePos, PipeBlockEntity.class);
        helper.assertTrue(pipe != null && !pipe.hasUi(), "Expected plain pipe to report hasUi=false");
        helper.succeed();
    }

    @GameTest(required = true)
    public void filteredPipesOpenUI(ExtendedGameTestHelper helper) {
        List<Block> pipes = List.of(TENBlocks.PIPE_WHITE.get(), TENBlocks.PIPE_BLACK.get());
        var player = helper.makeMockPlayer(GameType.CREATIVE);

        for (int index = 0; index < pipes.size(); index++) {
            BlockPos pipePos = new BlockPos(index, 1, 1);
            helper.setBlock(pipePos, pipes.get(index));
            PipeBlockEntity pipe = helper.getBlockEntity(pipePos, PipeBlockEntity.class);
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

    @GameTest(required = true)
    public void energyCablesDoNotOpenUI(ExtendedGameTestHelper helper) {
        List<Block> cables = List.of(
                TENBlocks.CABLE.get(),
                TENBlocks.CABLE_QUARTZ.get(),
                TENBlocks.CABLE_AZURE.get(),
                TENBlocks.CABLE_STAR.get());

        for (int index = 0; index < cables.size(); index++) {
            BlockPos cablePos = new BlockPos(index, 1, 1);
            helper.setBlock(cablePos, cables.get(index));
            CableBlockEntity cable = helper.getBlockEntity(cablePos, CableBlockEntity.class);
            helper.assertTrue(cable != null && !cable.hasUi(), "Expected energy cable to report hasUi=false for " + cables.get(index).getName().getString());
        }
        helper.succeed();
    }
}
