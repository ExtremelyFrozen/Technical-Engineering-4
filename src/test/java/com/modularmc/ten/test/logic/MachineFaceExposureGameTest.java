package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.blockentity.channel.ChannelFluidBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelItemBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 面配置的「外部观察读」契约：外部设备经面 capability 读取机器内容时，
 * getStackInSlot / getFluidInTank 必须真实反映机器状态；只有写入与抽取受面权限门控。
 *
 * <p>
 * 反例来源：AE2 样板供应器的阻挡模式（PatternProviderLogic 的
 * {@code adapter.containsPatternInput(patternInputs)}）经 ExternalStorageFacade#getAvailableStacks
 * 逐槽 getStackInSlot 判定「上一批配方是否仍在目标机器里」。若机器/频道机在不可抽取面把读取一并藏空，
 * 供应器永远认为目标为空 → 阻挡失效、连发配方把机器塞满。
 */
@GameTestHolder(TEN.MOD_ID)
public class MachineFaceExposureGameTest {

    /** 精炼机：2 物品槽（0 输入 / 1 输出）、2 流体罐（0 输入 / 1 输出）。 */
    private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);
    private static final Direction VIEW_SIDE = Direction.NORTH;

    @TestHolder(value = TEN.MOD_ID + ":out_face_exposes_item_contents_to_readers", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void outFaceExposesItemContents(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefiner(helper);
        if (machine == null) {
            return;
        }
        // 主动输出(OUT)：机器自己推给相邻，不开放外部抽取
        machine.itemFaceMode.put(VIEW_SIDE, FaceOption.OUT);
        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));

        IItemHandler view = machineFace(helper);
        if (view == null) {
            return;
        }

        // 观察读必须真实：AE2 阻挡模式据此判断上一批是否仍在机器内
        helper.assertTrue(!view.getStackInSlot(0).isEmpty(),
                "OUT face must expose slot contents to external readers (AE2 blocking mode reads getStackInSlot)");
        // 插入/抽取权限不变：主动输出面仍可被塞入、仍不允许外部抽取
        helper.assertTrue(view.insertItem(0, new ItemStack(Items.IRON_INGOT, 1), true).isEmpty(),
                "OUT face must still accept external inserts");
        helper.assertTrue(view.extractItem(0, 1, true).isEmpty(),
                "OUT face must still deny external extraction");
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":passive_input_face_exposes_item_contents_to_readers", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void passiveInputFaceExposesItemContentsToReaders(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefiner(helper);
        if (machine == null) {
            return;
        }
        // 被动输入(BE_IN)：外部可塞入、不可抽取——AE2 喂料的常见配置
        machine.itemFaceMode.put(VIEW_SIDE, FaceOption.BE_IN);
        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));

        IItemHandler view = machineFace(helper);
        if (view == null) {
            return;
        }

        helper.assertTrue(!view.getStackInSlot(0).isEmpty(),
                "BE_IN face must expose slot contents to external readers (AE2 blocking mode reads getStackInSlot)");
        helper.assertTrue(view.insertItem(0, new ItemStack(Items.IRON_INGOT, 1), true).isEmpty(),
                "BE_IN face must still accept external inserts");
        helper.assertTrue(view.extractItem(0, 1, true).isEmpty(),
                "BE_IN face must still deny external extraction");
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":out_face_exposes_fluid_contents_to_readers", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void outFaceExposesFluidContents(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefiner(helper);
        if (machine == null) {
            return;
        }
        machine.fluidFaceMode.put(VIEW_SIDE, FaceOption.OUT);

        IFluidHandler view = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(MACHINE_POS), VIEW_SIDE);
        if (view == null) {
            helper.fail("Expected a fluid handler capability on the machine's OUT face");
            return;
        }

        int filled = view.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled > 0, "OUT face must still accept fluid (passive insert stays allowed)");
        // 观察读必须真实（对称于物品槽：AE2 流体阻挡同样逐罐读取）
        helper.assertTrue(totalFluid(view) > 0,
                "OUT face must expose tank contents to external readers");
        // 权限不变：主动输出面仍不允许外部抽取
        helper.assertTrue(view.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                "OUT face must still deny external drain");
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_out_face_exposes_item_contents_to_readers", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelOutFaceExposesItemContents(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TENBlocks.CHANNEL_ITEM.get());
        ChannelItemBlockEntity channel = helper.getBlockEntity(MACHINE_POS);
        if (channel == null) {
            helper.fail("Expected a channel item block entity at " + MACHINE_POS);
            return;
        }
        channel.initMachine();
        if (!channel.join("face_exp")) {
            helper.fail("Expected the channel machine to join the test channel");
            return;
        }
        // join 在后续 tick 生效，故断言延到 tick 2（与既有频道用例同节奏）
        helper.runAtTickTime(2, () -> {
            if (channel.sharedStorage() == null) {
                helper.fail("Expected the channel machine to be attached to a shared storage");
                return;
            }
            // 频道机共享面包装器与基类同构：同一契约必须成立
            Direction side = channel.getFacing();
            channel.itemFaceMode.put(side, FaceOption.OUT);
            channel.sharedStorage().getItemHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));

            IItemHandler view = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(MACHINE_POS), side);
            if (view == null) {
                helper.fail("Expected an item handler capability on the channel machine's facing side");
                return;
            }

            helper.assertTrue(!view.getStackInSlot(0).isEmpty(),
                    "channel machine OUT face must expose shared contents to external readers");
            helper.assertTrue(view.insertItem(0, new ItemStack(Items.IRON_INGOT, 1), true).isEmpty(),
                    "channel machine OUT face must still accept external inserts");
            helper.assertTrue(view.extractItem(0, 1, true).isEmpty(),
                    "channel machine OUT face must still deny external extraction");
            helper.succeed();
        });
    }

    @TestHolder(value = TEN.MOD_ID + ":channel_out_face_exposes_fluid_contents_to_readers", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelOutFaceExposesFluidContents(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TENBlocks.CHANNEL_FLUID.get());
        ChannelFluidBlockEntity channel = helper.getBlockEntity(MACHINE_POS);
        if (channel == null) {
            helper.fail("Expected a channel fluid block entity at " + MACHINE_POS);
            return;
        }
        channel.initMachine();
        if (!channel.join("face_fl")) {
            helper.fail("Expected the channel machine to join the test channel");
            return;
        }
        helper.runAtTickTime(2, () -> {
            if (channel.sharedStorage() == null) {
                helper.fail("Expected the channel machine to be attached to a shared storage");
                return;
            }
            Direction side = channel.getFacing();
            channel.fluidFaceMode.put(side, FaceOption.OUT);

            IFluidHandler view = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(MACHINE_POS), side);
            if (view == null) {
                helper.fail("Expected a fluid handler capability on the channel machine's facing side");
                return;
            }

            int filled = view.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled > 0, "channel machine OUT face must still accept fluid");
            helper.assertTrue(totalFluid(view) > 0,
                    "channel machine OUT face must expose shared tank contents to external readers");
            helper.assertTrue(view.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                    "channel machine OUT face must still deny external drain");
            helper.succeed();
        });
    }

    /** 放置精炼机并完成机器初始化；失败时返回 null（已 fail）。 */
    private static CmMachineBlockEntity placeRefiner(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        CmMachineBlockEntity machine = helper.getBlockEntity(MACHINE_POS);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + MACHINE_POS);
            return null;
        }
        machine.initMachine();
        return machine;
    }

    /** 取被测机器面 capability（物品）；失败时返回 null（已 fail）。 */
    private static IItemHandler machineFace(GameTestHelper helper) {
        IItemHandler view = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(MACHINE_POS), VIEW_SIDE);
        if (view == null) {
            helper.fail("Expected an item handler capability on the machine's face");
        }
        return view;
    }

    private static long totalFluid(IFluidHandler handler) {
        long total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            total += handler.getFluidInTank(tank).getAmount();
        }
        return total;
    }
}
