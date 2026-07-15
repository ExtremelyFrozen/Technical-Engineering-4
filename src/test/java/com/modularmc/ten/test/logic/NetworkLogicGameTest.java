package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelEnergyBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelFluidBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelItemBlockEntity;
import com.modularmc.ten.common.blockentity.machine.CellBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import net.neoforged.testframework.gametest.GameTest;

@TestHolder(value = TEN.MOD_ID + ":network_logic")
public class NetworkLogicGameTest {

    @GameTest(required = true)
    public void channelConnectorLinksChannels(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_ENERGY.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_ENERGY.get());

        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TENItems.CHANNEL_CONNECTOR.get()));
        BlockPos absoluteSource = helper.absolutePos(sourcePos);
        BlockPos absoluteTarget = helper.absolutePos(targetPos);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absoluteSource), net.minecraft.core.Direction.UP, absoluteSource, false)));
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absoluteTarget), net.minecraft.core.Direction.UP, absoluteTarget, false)));

        helper.runAtTickTime(2, () -> {
            ChannelEnergyBlockEntity source = helper.getBlockEntity(sourcePos, ChannelEnergyBlockEntity.class);
            helper.assertTrue(source.hasOutputLink(absoluteTarget), "expected connector to bind output link");
            helper.succeed();
        });
    }

    @GameTest(required = true)
    public void channelEnergyTransfersWirelessly(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_ENERGY.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_ENERGY.get());

        ChannelEnergyBlockEntity source = helper.getBlockEntity(sourcePos, ChannelEnergyBlockEntity.class);
        ChannelEnergyBlockEntity target = helper.getBlockEntity(targetPos, ChannelEnergyBlockEntity.class);
        source.initMachine();
        target.initMachine();
        source.energyStorage.setEnergy(4000);
        source.linkOut(helper.absolutePos(targetPos));

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(target.energyStorage.getEnergyStored() > 0, "expected wireless channel to transfer energy");
            helper.succeed();
        });
    }

    @GameTest(required = true)
    public void channelItemTransfersWirelessly(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_ITEM.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_ITEM.get());

        ChannelItemBlockEntity source = helper.getBlockEntity(sourcePos, ChannelItemBlockEntity.class);
        ChannelItemBlockEntity target = helper.getBlockEntity(targetPos, ChannelItemBlockEntity.class);
        source.initMachine();
        target.initMachine();
        source.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 1));
        source.linkOut(helper.absolutePos(targetPos));

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(!target.itemHandler.getStackInSlot(0).isEmpty(), "expected wireless item channel to transfer items");
            helper.succeed();
        });
    }

    @GameTest(required = true)
    public void channelFluidTransfersWirelessly(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_FLUID.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_FLUID.get());

        ChannelFluidBlockEntity source = helper.getBlockEntity(sourcePos, ChannelFluidBlockEntity.class);
        ChannelFluidBlockEntity target = helper.getBlockEntity(targetPos, ChannelFluidBlockEntity.class);
        source.initMachine();
        target.initMachine();
        source.tanks.get(0).fill(new FluidStack(Fluids.WATER, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        source.linkOut(helper.absolutePos(targetPos));

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(target.tanks.get(0).getFluidAmount() > 0, "expected wireless fluid channel to transfer fluid");
            helper.succeed();
        });
    }

    @GameTest(required = true)
    public void cableMovesEnergyBetweenCells(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CELL.get());
        helper.setBlock(cablePos, TENBlocks.CABLE.get());
        helper.setBlock(targetPos, TENBlocks.CELL.get());

        CellBlockEntity source = helper.getBlockEntity(sourcePos, CellBlockEntity.class);
        CellBlockEntity target = helper.getBlockEntity(targetPos, CellBlockEntity.class);
        source.initMachine();
        target.initMachine();
        source.energyStorage.setEnergy(4000);
        target.energyStorage.setEnergy(0);

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(target.energyStorage.getEnergyStored() > 0, "expected cable network to move energy between cells");
            helper.succeed();
        });
    }

    @GameTest(required = true)
    public void pipeMovesItemsBetweenContainers(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(pipePos, TENBlocks.PIPE.get());
        helper.setBlock(targetPos, Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(sourcePos, ChestBlockEntity.class);
        ChestBlockEntity target = helper.getBlockEntity(targetPos, ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 1));

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(source.getItem(0).isEmpty(), "expected source chest to be drained by pipe");
            helper.assertTrue(!target.getItem(0).isEmpty(), "expected target chest to receive item from pipe");
            helper.succeed();
        });
    }

    @GameTest(required = true)
    public void pipeWhitelistBlocksUnlistedItems(ExtendedGameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(pipePos, TENBlocks.PIPE_WHITE.get());
        helper.setBlock(targetPos, Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(sourcePos, ChestBlockEntity.class);
        ChestBlockEntity target = helper.getBlockEntity(targetPos, ChestBlockEntity.class);
        PipeBlockEntity pipe = helper.getBlockEntity(pipePos, PipeBlockEntity.class);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 1));
        pipe.getFilterInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(!source.getItem(0).isEmpty(), "expected whitelist pipe to keep unlisted item in source chest");
            helper.assertTrue(target.getItem(0).isEmpty(), "expected whitelist pipe to block transfer");
            helper.succeed();
        });
    }
}
