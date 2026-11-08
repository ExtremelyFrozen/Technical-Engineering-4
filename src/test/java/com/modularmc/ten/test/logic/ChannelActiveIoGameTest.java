package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity;
import com.modularmc.ten.common.channel.SharedStorage;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 频道主动 IO（拉取 / 推送）与破坏掉落。
 *
 * <p>
 * 频道面默认全关、接入后才暴露能力。主动输入把相邻容器搬进<b>共享存储</b>，主动输出把
 * <b>共享存储</b>搬进相邻容器——两个方向都必须落在共享存储上，写本地缓冲会出现
 * 「GUI 槽位不刷新」（接入态 UI 绑定的是共享 handler）。
 */
@GameTestHolder(TEN.MOD_ID)
public class ChannelActiveIoGameTest {

    private static final BlockPos CHANNEL_POS = new BlockPos(1, 1, 1);
    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos NEIGHBOUR_POS = CHANNEL_POS.relative(FRONT);

    // ───── 主动输入：相邻容器 → 共享存储 ─────

    @TestHolder(value = TEN.MOD_ID + ":channel_active_pull_fills_shared_item_storage", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelActivePullFillsSharedItemStorage(GameTestHelper helper) {
        helper.setBlock(NEIGHBOUR_POS, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(NEIGHBOUR_POS);
        if (chest == null) {
            helper.fail("Expected a chest at " + NEIGHBOUR_POS);
            return;
        }
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 16));

        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_ITEM.get(), "pull_items");
        if (channel == null) return;
        channel.itemFaceMode.put(FRONT, FaceOption.IN);

        channel.tick();

        SharedStorage shared = channel.sharedStorage();
        if (shared == null) {
            helper.fail("joined channel must expose its shared storage");
            return;
        }
        helper.assertTrue(countItems(shared.getItemHandler()) == 16,
                "shared storage must receive the pulled items, got " + countItems(shared.getItemHandler()));
        helper.assertTrue(countItems(channel.getChannelItemFacade()) == 16,
                "the UI facade must resolve to the same shared storage, got " + countItems(channel.getChannelItemFacade()));
        helper.assertTrue(countItems(chest) == 0,
                "source chest must be emptied by the active pull, got " + countItems(chest));
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_active_pull_fills_shared_fluid_storage", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelActivePullFillsSharedFluidStorage(GameTestHelper helper) {
        CmMachineBlockEntity source = placeMachine(helper, NEIGHBOUR_POS, TENBlocks.MACHINE_REFINER.get());
        if (source == null) return;
        // tank 1 是输出罐（canOut）；输入罐不参与外部抽取，不能当拉取源
        source.tanks.get(1).setFluid(new FluidStack(Fluids.WATER, 500));
        source.fluidFaceMode.put(FRONT.getOpposite(), FaceOption.BE_OUT);

        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_FLUID.get(), "pull_fluid");
        if (channel == null) return;
        channel.fluidFaceMode.put(FRONT, FaceOption.IN);

        channel.tick();

        SharedStorage shared = channel.sharedStorage();
        if (shared == null) {
            helper.fail("joined channel must expose its shared storage");
            return;
        }
        int stored = sharedFluid(shared);
        helper.assertTrue(stored == 500, "shared tanks must receive the pulled fluid, got " + stored);
        helper.assertTrue(source.tanks.get(1).getFluidAmount() == 0,
                "source tank must be drained, got " + source.tanks.get(1).getFluidAmount());
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_active_pull_fills_shared_energy_storage", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelActivePullFillsSharedEnergyStorage(GameTestHelper helper) {
        CmMachineBlockEntity source = placeMachine(helper, NEIGHBOUR_POS, TENBlocks.CREATIVE_CELL.get());
        if (source == null) return;
        // 创造单元「无限」来自每 tick 产能，而源块在测试里不 tick，直接给一份存量当作可用电
        source.energyStorage.setEnergy(50_000);
        source.energyFaceMode.put(FRONT.getOpposite(), FaceOption.BE_OUT);

        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_ENERGY.get(), "pull_energy");
        if (channel == null) return;
        channel.energyFaceMode.put(FRONT, FaceOption.IN);

        channel.tick();

        SharedStorage shared = channel.sharedStorage();
        if (shared == null) {
            helper.fail("joined channel must expose its shared storage");
            return;
        }
        helper.assertTrue(shared.getEnergy().getEnergyStored() > 0,
                "shared energy storage must receive the pulled energy, got " + shared.getEnergy().getEnergyStored());
        helper.succeed();
    }

    // ───── 主动输出：共享存储 → 相邻容器 ─────

    @TestHolder(value = TEN.MOD_ID + ":channel_active_push_empties_shared_item_storage", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelActivePushEmptiesSharedItemStorage(GameTestHelper helper) {
        helper.setBlock(NEIGHBOUR_POS, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(NEIGHBOUR_POS);
        if (chest == null) {
            helper.fail("Expected a chest at " + NEIGHBOUR_POS);
            return;
        }

        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_ITEM.get(), "push_items");
        if (channel == null) return;
        SharedStorage shared = channel.sharedStorage();
        if (shared == null) {
            helper.fail("joined channel must expose its shared storage");
            return;
        }
        shared.getItemHandler().insertItem(0, new ItemStack(Items.GOLD_INGOT, 12), false);
        channel.itemFaceMode.put(FRONT, FaceOption.OUT);

        channel.tick();

        helper.assertTrue(countItems(chest) == 12,
                "the active push must hand the shared items to the chest, got " + countItems(chest));
        helper.assertTrue(countItems(shared.getItemHandler()) == 0,
                "shared storage must be emptied by the active push, got " + countItems(shared.getItemHandler()));
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_active_push_empties_shared_fluid_storage", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelActivePushEmptiesSharedFluidStorage(GameTestHelper helper) {
        CmMachineBlockEntity sink = placeMachine(helper, NEIGHBOUR_POS, TENBlocks.MACHINE_REFINER.get());
        if (sink == null) return;
        sink.fluidFaceMode.put(FRONT.getOpposite(), FaceOption.BE_IN);

        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_FLUID.get(), "push_fluid");
        if (channel == null) return;
        SharedStorage shared = channel.sharedStorage();
        if (shared == null) {
            helper.fail("joined channel must expose its shared storage");
            return;
        }
        shared.getTanks().get(0).fill(new FluidStack(Fluids.WATER, 400), IFluidHandler.FluidAction.EXECUTE);
        channel.fluidFaceMode.put(FRONT, FaceOption.OUT);

        channel.tick();

        helper.assertTrue(sink.tanks.get(0).getFluidAmount() == 400,
                "the active push must fill the refiner input tank, got " + sink.tanks.get(0).getFluidAmount());
        helper.assertTrue(sharedFluid(shared) == 0,
                "shared tanks must be emptied by the active push, got " + sharedFluid(shared));
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_active_push_empties_shared_energy_storage", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelActivePushEmptiesSharedEnergyStorage(GameTestHelper helper) {
        CmMachineBlockEntity sink = placeMachine(helper, NEIGHBOUR_POS, TENBlocks.CELL.get());
        if (sink == null) return;
        sink.energyFaceMode.put(FRONT.getOpposite(), FaceOption.BE_IN);

        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_ENERGY.get(), "push_energy");
        if (channel == null) return;
        SharedStorage shared = channel.sharedStorage();
        if (shared == null) {
            helper.fail("joined channel must expose its shared storage");
            return;
        }
        shared.getEnergy().receiveEnergy(400, false);
        channel.energyFaceMode.put(FRONT, FaceOption.OUT);

        channel.tick();

        helper.assertTrue(sink.energyStorage.getEnergyStored() == 400,
                "the active push must charge the energy cell, got " + sink.energyStorage.getEnergyStored());
        helper.assertTrue(shared.getEnergy().getEnergyStored() == 0,
                "shared energy storage must be emptied by the active push, got " + shared.getEnergy().getEnergyStored());
        helper.succeed();
    }

    // ───── 破坏 ─────

    /** 已接入频道被破坏后必须真的消失（旧实现会在 setRemoved 里把方块写回成「未接入」）。 */
    @TestHolder(value = TEN.MOD_ID + ":channel_break_removes_block", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelBreakRemovesBlock(GameTestHelper helper) {
        AbstractChannelBlockEntity channel = placeChannel(helper, TENBlocks.CHANNEL_ITEM.get(), "break_channel");
        if (channel == null) return;

        BlockPos absolute = helper.absolutePos(CHANNEL_POS);
        helper.getLevel().destroyBlock(absolute, false);
        helper.assertTrue(helper.getLevel().getBlockState(absolute).isAir(),
                "breaking a joined channel must remove it, got " + helper.getLevel().getBlockState(absolute));
        helper.succeed();
    }

    // ───── 辅助 ─────

    private static AbstractChannelBlockEntity placeChannel(GameTestHelper helper, Block block,
                                                           String channelName) {
        helper.setBlock(CHANNEL_POS, block);
        var be = helper.getBlockEntity(CHANNEL_POS);
        if (!(be instanceof AbstractChannelBlockEntity channel)) {
            helper.fail("Expected a channel block entity at " + CHANNEL_POS);
            return null;
        }
        channel.initMachine();
        invalidate(helper, CHANNEL_POS);
        if (!channel.join(channelName)) {
            helper.fail("Failed to join test channel " + channelName);
            return null;
        }
        SharedStorage shared = channel.sharedStorage();
        if (shared != null) {
            clearShared(shared);
        }
        return channel;
    }

    /**
     * 共享存储随世界存档持久化（{@code kenergyengineering_channel_registry.dat} 在
     * {@code run/gametest} 世界里跨运行累积），同名频道会带着上一次运行的残留内容。
     * 每个测例先清空，否则「拉进来多少」的断言会按残留量翻倍。
     */
    private static void clearShared(SharedStorage shared) {
        switch (shared.type()) {
            case ITEM -> {
                var items = shared.getItemHandler();
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    items.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            case FLUID -> shared.getTanks().forEach(tank -> tank.setFluid(FluidStack.EMPTY));
            case ENERGY -> shared.getEnergy().setEnergy(0);
        }
    }

    private static CmMachineBlockEntity placeMachine(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        CmMachineBlockEntity machine = helper.getBlockEntity(pos);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + pos);
            return null;
        }
        machine.initMachine();
        invalidate(helper, pos);
        return machine;
    }

    /**
     * 初始化后 energyStorage 才存在；能力缓存若在 setBlock 阶段记过 null，必须此时失效，
     * 否则同一 tick 内的 capability 查询仍返回 null（GameTest 的 setBlock 不走常规更新链）。
     * GameTest 的 pos 是结构内相对坐标，要经 absolutePos 转世界坐标。
     */
    private static void invalidate(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().invalidateCapabilities(helper.absolutePos(pos));
    }

    private static int sharedFluid(SharedStorage shared) {
        return shared.getTanks().stream().mapToInt(tank -> tank.getFluidAmount()).sum();
    }

    private static int countItems(IItemHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            total += handler.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static int countItems(ChestBlockEntity chest) {
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            total += chest.getItem(slot).getCount();
        }
        return total;
    }
}
