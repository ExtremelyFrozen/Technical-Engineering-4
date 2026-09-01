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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

@GameTestHolder(TEN.MOD_ID)
public class NetworkLogicGameTest {

    @TestHolder(value = TEN.MOD_ID + ":channel_connector_links_channels", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelConnectorLinksChannels(GameTestHelper helper) {
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
            ChannelEnergyBlockEntity source = helper.getBlockEntity(sourcePos);
            // 新频道系统：连接器是复制/应用面配置，不再硬链接
            // 验证连接器复制面配置（非空即成功）
            helper.assertTrue(source.isJoined() || !source.energyFaceMode.isEmpty(), "expected connector to have copied face config");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_energy_shares_wirelessly", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelEnergySharesWirelessly(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_ENERGY.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_ENERGY.get());

        ChannelEnergyBlockEntity source = helper.getBlockEntity(sourcePos);
        ChannelEnergyBlockEntity target = helper.getBlockEntity(targetPos);
        source.initMachine();
        target.initMachine();
        // 先塞本地缓冲再 join：join 的 pushLocalToShared 把本地内容并入共享存储
        source.energyStorage.setEnergy(4000);
        source.join("test_energy");
        target.join("test_energy");

        helper.runAtTickTime(2, () -> {
            // 共享存储：source 存入的能量应能被 target 看到（同一共享存储）
            helper.assertTrue(source.sharedStorage() == target.sharedStorage(), "expected both channels to share the same storage");
            helper.assertTrue(source.sharedStorage().getEnergy().getEnergyStored() > 0, "expected shared storage to have energy");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_item_shares_wirelessly", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelItemSharesWirelessly(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_ITEM.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_ITEM.get());

        ChannelItemBlockEntity source = helper.getBlockEntity(sourcePos);
        ChannelItemBlockEntity target = helper.getBlockEntity(targetPos);
        source.initMachine();
        target.initMachine();
        // 先塞本地缓冲再 join：join 的 pushLocalToShared 把本地内容并入共享存储
        source.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 1));
        source.join("test_item");
        target.join("test_item");

        helper.runAtTickTime(2, () -> {
            // 共享存储：source 存入的物品应能被 target 看到
            helper.assertTrue(source.sharedStorage() == target.sharedStorage(), "expected both channels to share the same storage");
            helper.assertTrue(!source.sharedStorage().getItemHandler().getStackInSlot(0).isEmpty(), "expected shared storage to have items");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_fluid_shares_wirelessly", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelFluidSharesWirelessly(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(3, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CHANNEL_FLUID.get());
        helper.setBlock(targetPos, TENBlocks.CHANNEL_FLUID.get());

        ChannelFluidBlockEntity source = helper.getBlockEntity(sourcePos);
        ChannelFluidBlockEntity target = helper.getBlockEntity(targetPos);
        source.initMachine();
        target.initMachine();
        // 先塞本地缓冲再 join：join 的 pushLocalToShared 把本地内容并入共享存储
        source.tanks.get(0).fill(new FluidStack(Fluids.WATER, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        source.join("test_fluid");
        target.join("test_fluid");

        helper.runAtTickTime(2, () -> {
            // 共享存储：source 存入的流体应能被 target 看到
            helper.assertTrue(source.sharedStorage() == target.sharedStorage(), "expected both channels to share the same storage");
            helper.assertTrue(source.sharedStorage().getTanks().get(0).getFluidAmount() > 0, "expected shared storage to have fluid");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":cable_moves_energy_between_cells", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void cableMovesEnergyBetweenCells(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, TENBlocks.CELL.get());
        helper.setBlock(cablePos, TENBlocks.CABLE.get());
        helper.setBlock(targetPos, TENBlocks.CELL.get());

        CellBlockEntity source = helper.getBlockEntity(sourcePos);
        CellBlockEntity target = helper.getBlockEntity(targetPos);
        source.initMachine();
        target.initMachine();
        source.energyStorage.setEnergy(4000);
        target.energyStorage.setEnergy(0);

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(target.energyStorage.getEnergyStored() > 0, "expected cable network to move energy between cells");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":pipe_moves_items_between_containers", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void pipeMovesItemsBetweenContainers(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(pipePos, TENBlocks.PIPE.get());
        helper.setBlock(targetPos, Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        ChestBlockEntity target = helper.getBlockEntity(targetPos);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 1));

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(source.getItem(0).isEmpty(), "expected source chest to be drained by pipe");
            helper.assertTrue(!target.getItem(0).isEmpty(), "expected target chest to receive item from pipe");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":pipe_whitelist_blocks_unlisted_items", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void pipeWhitelistBlocksUnlistedItems(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(pipePos, TENBlocks.PIPE_WHITE.get());
        helper.setBlock(targetPos, Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        ChestBlockEntity target = helper.getBlockEntity(targetPos);
        PipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 1));
        pipe.getFilterInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(!source.getItem(0).isEmpty(), "expected whitelist pipe to keep unlisted item in source chest");
            helper.assertTrue(target.getItem(0).isEmpty(), "expected whitelist pipe to block transfer");
            helper.succeed();
        });
    }
}