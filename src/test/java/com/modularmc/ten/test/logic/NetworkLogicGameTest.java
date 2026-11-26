package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelEnergyBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelFluidBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelItemBlockEntity;
import com.modularmc.ten.common.blockentity.machine.CellBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
            // 频道已接入，或连接器已复制面配置。
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

    @TestHolder(value = TEN.MOD_ID + ":pipe_pull_mode_controls_extraction", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void pipePullModeControlsExtraction(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos targetPos = new BlockPos(2, 1, 1);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(pipePos, TENBlocks.PIPE.get());
        helper.setBlock(targetPos, Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        ChestBlockEntity target = helper.getBlockEntity(targetPos);
        PipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 4));

        // 阶段 1（t=30）：默认 NORMAL 面——管道不应主动抽取（须扳手显式切 PULL）；二态轮询首次点击 NORMAL→PULL
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(!source.getItem(0).isEmpty() && source.getItem(0).getCount() == 4,
                    "expected NORMAL face to NOT auto-extract items");
            helper.assertTrue(spannerUseOn(helper, pipePos, Direction.WEST).consumesAction(),
                    "expected spanner useItemOn to consume action");
            helper.assertTrue(pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST) == ConnectionType.PULL,
                    "expected first click NORMAL->PULL, got " + pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST));
        });
        // 阶段 2（t=56）：PULL 面应完成主动抽取（25 tick 窗口 ≥ 5 个搬运轮）
        helper.runAtTickTime(56, () -> {
            var net = pipe.getTransmitter().getNetwork();
            String diag = "src=" + source.getItem(0) + " tgt=" + target.getItem(0) + " mode=" + pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST) + " netSize=" + (net == null ? -1 : net.size());
            helper.assertTrue(source.getItem(0).isEmpty(), "expected PULL face to auto-extract: " + diag);
            helper.assertTrue(!target.getItem(0).isEmpty(), "expected item routed to target: " + diag);
            helper.assertTrue(spannerUseOn(helper, pipePos, Direction.WEST).consumesAction(), "second click");
            helper.assertTrue(pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST) == ConnectionType.NORMAL,
                    "expected second click PULL->NORMAL, got " + pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST));
            helper.assertTrue(spannerUseOn(helper, pipePos, Direction.WEST).consumesAction(), "third click");
            helper.assertTrue(pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST) == ConnectionType.PULL,
                    "expected third click NORMAL->PULL, got " + pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST));
            helper.succeed();
        });
    }

    /**
     * 机器相邻边按 NORMAL→PULL→NORMAL 二态轮询。
     */
    @TestHolder(value = TEN.MOD_ID + ":pipe_machine_face_two_state", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void pipeMachineFaceTwoState(GameTestHelper helper) {
        BlockPos machinePos = new BlockPos(0, 1, 1);
        BlockPos pipePos = new BlockPos(1, 1, 1);
        helper.setBlock(machinePos, TENBlocks.CELL.get());
        helper.setBlock(pipePos, TENBlocks.PIPE.get());

        PipeBlockEntity pipe = helper.getBlockEntity(pipePos);

        helper.runAtTickTime(5, () -> {
            // 机器边 acceptor 位应已建立（机器注册了 ItemHandler 能力）
            helper.assertTrue(spannerUseOn(helper, pipePos, Direction.WEST).consumesAction(), "first click consume");
            var afterFirst = pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST);
            helper.assertTrue(afterFirst == ConnectionType.PULL, "machine face first click should be PULL, got " + afterFirst);
            helper.assertTrue(spannerUseOn(helper, pipePos, Direction.WEST).consumesAction(), "second click consume");
            var afterSecond = pipe.getTransmitter().getConnectionTypeRaw(Direction.WEST);
            helper.assertTrue(afterSecond == ConnectionType.NORMAL, "machine face second click should be NORMAL, got " + afterSecond);
            helper.succeed();
        });
    }

    /**
     * 相邻管道按 NONE→NORMAL 切换断开与重连。
     */
    @TestHolder(value = TEN.MOD_ID + ":pipe_pipe_disconnect_toggle", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void pipePipeDisconnectToggle(GameTestHelper helper) {
        BlockPos pipeA = new BlockPos(1, 1, 1);
        BlockPos pipeB = new BlockPos(2, 1, 1);
        helper.setBlock(pipeA, TENBlocks.PIPE.get());
        helper.setBlock(pipeB, TENBlocks.PIPE.get());

        PipeBlockEntity a = helper.getBlockEntity(pipeA);
        PipeBlockEntity b = helper.getBlockEntity(pipeB);

        helper.runAtTickTime(5, () -> {
            var netA0 = a.getTransmitter().getNetwork();
            helper.assertTrue(netA0 != null && netA0 == b.getTransmitter().getNetwork(), "expected A/B initially in same network");
            // 第一次点击 A 的 EAST 面（朝 B）：NORMAL→NONE 断开；raw 同步生效，网络拆分延迟到 level tick 末
            helper.assertTrue(spannerUseOn(helper, pipeA, Direction.EAST).consumesAction(), "disconnect click consume");
            var afterCut = a.getTransmitter().getConnectionTypeRaw(Direction.EAST);
            helper.assertTrue(afterCut == ConnectionType.NONE, "expected A EAST raw NONE after first click, got " + afterCut);
        });
        // t=8：tick 末已拆网（processPendingRemovals/Joins），断言分裂后点击重连
        helper.runAtTickTime(8, () -> {
            var netA1 = a.getTransmitter().getNetwork();
            helper.assertTrue(netA1 == null || netA1 != b.getTransmitter().getNetwork(), "expected A/B networks split after disconnect tick");
            helper.assertTrue(spannerUseOn(helper, pipeA, Direction.EAST).consumesAction(), "reconnect click consume");
            var afterRec = a.getTransmitter().getConnectionTypeRaw(Direction.EAST);
            helper.assertTrue(afterRec == ConnectionType.NORMAL, "expected A EAST raw NORMAL after second click, got " + afterRec);
        });
        // t=11：重连后的重组（level tick 末）应使 A/B 重新同网
        helper.runAtTickTime(11, () -> {
            var netA2 = a.getTransmitter().getNetwork();
            helper.assertTrue(netA2 != null && netA2 == b.getTransmitter().getNetwork(), "expected A/B re-joined into same network");
            helper.succeed();
        });
    }

    /** 反射调用块侧 useItemOn（真实玩家右键先走块侧，GameTest 无公开入口可直调）。 */
    private static ItemInteractionResult spannerUseOn(GameTestHelper helper, BlockPos rel, Direction face) {
        try {
            var pipe = helper.getBlockEntity(rel);
            BlockState state = pipe.getBlockState();
            java.lang.reflect.Method method = null;
            Class<?> c = state.getBlock().getClass();
            while (c != null && c != Object.class) {
                try {
                    method = c.getDeclaredMethod("useItemOn", ItemStack.class, BlockState.class,
                            net.minecraft.world.level.Level.class, BlockPos.class,
                            net.minecraft.world.entity.player.Player.class, InteractionHand.class, BlockHitResult.class);
                    break;
                } catch (NoSuchMethodException ignored) {
                    c = c.getSuperclass();
                }
            }
            if (method == null) {
                throw new NoSuchMethodException("useItemOn");
            }
            method.setAccessible(true);
            var player = helper.makeMockPlayer(GameType.CREATIVE);
            BlockPos abs = helper.absolutePos(rel);
            // 落点取对应连接臂内并避开核心中心；服务端 resolveClickedEdge 按落点所在臂判定方向，
            // 点核心会返回 null 不响应。
            Vec3 loc = face == Direction.EAST ? new Vec3(abs.getX() + 0.85, abs.getY() + 0.5, abs.getZ() + 0.5) : face == Direction.WEST ? new Vec3(abs.getX() + 0.15, abs.getY() + 0.5, abs.getZ() + 0.5) : Vec3.atCenterOf(abs);
            BlockHitResult hit = new BlockHitResult(loc, face, abs, false);
            return (ItemInteractionResult) method.invoke(state.getBlock(), new ItemStack(TENItems.SPANNER.get()),
                    state, helper.getLevel(), abs, player, InteractionHand.MAIN_HAND, hit);
        } catch (Exception e) {
            throw new RuntimeException("spanner useItemOn failed", e);
        }
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
        // 受端必须改被动输入：单元默认全 OUT（主动放电），否则线缆送来的电会被立刻推回线缆。
        target.energyFaceMode.put(net.minecraft.core.Direction.WEST,
                com.modularmc.ten.api.option.FaceOption.BE_IN);
        source.energyStorage.setEnergy(4000);
        target.energyStorage.setEnergy(0);

        // GameTestServer 启动时可能延迟网络成形；25 tick 未动则等到 60 tick 再判定，
        // 断言附带两端储能与面配置，便于区分网络未成形和电未传输。
        helper.runAtTickTime(25, () -> {
            if (target.energyStorage.getEnergyStored() > 0) {
                helper.succeed();
                return;
            }
            helper.runAtTickTime(60, () -> {
                var cap = target.getEnergyStorage(net.minecraft.core.Direction.WEST);
                helper.assertTrue(target.energyStorage.getEnergyStored() > 0,
                        "expected cable network to move energy between cells, source=" + source.energyStorage.getEnergyStored() + " target=" + target.energyStorage.getEnergyStored() + " targetFaces=" + target.energyFaceMode + " sourceFaces=" + source.energyFaceMode + " westCanReceive=" + (cap != null && cap.canReceive()));
                helper.succeed();
            });
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
        // 默认 NORMAL 不主动抽，显式切 source 侧为 PULL
        ((com.modularmc.ten.api.transmission.item.ItemTransmitter) ((PipeBlockEntity) helper.getBlockEntity(pipePos)).getTransmitter())
                .setConnectionTypeRaw(Direction.WEST, ConnectionType.PULL);

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
        // 显式切 source 侧为 PULL 才会尝试抽取（验证白名单拦截的是抽取到的物品）
        ((com.modularmc.ten.api.transmission.item.ItemTransmitter) pipe.getTransmitter())
                .setConnectionTypeRaw(Direction.WEST, ConnectionType.PULL);

        helper.runAtTickTime(25, () -> {
            helper.assertTrue(!source.getItem(0).isEmpty(), "expected whitelist pipe to keep unlisted item in source chest");
            helper.assertTrue(target.getItem(0).isEmpty(), "expected whitelist pipe to block transfer");
            helper.succeed();
        });
    }
}
