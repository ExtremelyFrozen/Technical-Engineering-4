package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 面配置可选范围：由方块标签组合（{@code ten:face_active_energy/_item/_fluid}）决定主动模式，
 * 由方块自身能力决定该资源页是否存在。
 *
 * <p>
 * 这两条边界都是「配了也不生效」的静默错误来源：频道方块的共享存储不接受机器自搬，
 * 无罐机器的流体页无论怎么点都不会有流体进出。
 */
@GameTestHolder(TEN.MOD_ID)
public class FaceModeRangeGameTest {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** 普通机器：三种资源页齐全（精炼机有槽位、有罐体、有储能），六态全可选。 */
    @TestHolder(value = TEN.MOD_ID + ":machine_face_range_is_full", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void machineFaceRangeIsFull(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_REFINER.get());
        if (machine == null) return;

        for (int type : new int[] { FaceOption.TYPE_ENERGY, FaceOption.TYPE_ITEM, FaceOption.TYPE_FLUID }) {
            for (int mode = FaceOption.OFF; mode < FaceOption.size(); mode++) {
                helper.assertTrue(machine.allowsFaceMode(type, mode),
                        "tagged machine must allow every face mode, type=" + type + " mode=" + FaceOption.toStr(mode));
            }
        }
        helper.succeed();
    }

    /** 频道方块不带主动标签：六面只能配被动三态，主动输入/输出必须被挡掉。 */
    @TestHolder(value = TEN.MOD_ID + ":channel_faces_are_passive_only", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelFacesArePassiveOnly(GameTestHelper helper) {
        CmMachineBlockEntity channel = place(helper, TENBlocks.CHANNEL_ITEM.get());
        if (channel == null) return;

        helper.assertTrue(channel.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.OFF), "OFF must always be allowed");
        helper.assertTrue(channel.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.BE_IN), "passive insert must be allowed");
        helper.assertTrue(channel.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.BE_OUT), "passive extract must be allowed");
        helper.assertTrue(channel.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.BOTH), "passive both must be allowed");
        helper.assertFalse(channel.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.IN),
                "item channel must not accept active pull: shared storage owns the movement");
        helper.assertFalse(channel.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.OUT),
                "item channel must not accept active push: shared storage owns the movement");
        // 频道只有自己那一种资源页：物品频道不该提供能量/流体面（本地缓冲不是它的用途）
        helper.assertFalse(channel.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.BE_IN),
                "item channel has no energy face page");
        helper.assertFalse(channel.allowsFaceMode(FaceOption.TYPE_FLUID, FaceOption.BOTH),
                "item channel has no fluid face page");
        helper.succeed();
    }

    /** 无罐机器的流体页只剩「关闭」：点一下不会变成被动输出。 */
    @TestHolder(value = TEN.MOD_ID + ":tankless_machine_has_no_fluid_modes", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void tanklessMachineHasNoFluidModes(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_PULVERIZER.get());
        if (machine == null) return;

        helper.assertTrue(machine.tanks.isEmpty(), "pulverizer is expected to have no tank");
        helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_FLUID, FaceOption.OFF), "OFF must always be allowed");
        helper.assertFalse(machine.allowsFaceMode(FaceOption.TYPE_FLUID, FaceOption.BE_IN),
                "a tankless machine must not offer fluid face modes");
        helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.IN),
                "item page stays fully available on a tankless machine");
        helper.succeed();
    }

    /** 循环跳过不可选模式：机器 OFF → IN，频道 OFF → BE_IN（不能先跳到 IN）。 */
    @TestHolder(value = TEN.MOD_ID + ":face_cycle_skips_disallowed_modes", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void faceCycleSkipsDisallowedModes(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_PULVERIZER.get());
        CmMachineBlockEntity channel = place(helper, TENBlocks.CHANNEL_ITEM.get(), POS.east(2));
        if (machine == null || channel == null) return;

        int machineNext = FaceOption.nextAllowed(FaceOption.OFF,
                m -> machine.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(machineNext == FaceOption.IN,
                "machine item page must cycle OFF -> IN, got " + FaceOption.toStr(machineNext));

        int channelNext = FaceOption.nextAllowed(FaceOption.OFF,
                m -> channel.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(channelNext == FaceOption.BE_IN,
                "channel item page must cycle OFF -> BE_IN, got " + FaceOption.toStr(channelNext));

        // 绕回：频道从 BOTH 继续点应回到 OFF
        int wrapped = FaceOption.nextAllowed(FaceOption.BOTH,
                m -> channel.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(wrapped == FaceOption.OFF,
                "channel item page must wrap BOTH -> OFF, got " + FaceOption.toStr(wrapped));
        helper.succeed();
    }

    private static CmMachineBlockEntity place(GameTestHelper helper, Block block) {
        return place(helper, block, POS);
    }

    private static CmMachineBlockEntity place(GameTestHelper helper, Block block, BlockPos pos) {
        helper.setBlock(pos, block);
        CmMachineBlockEntity machine = helper.getBlockEntity(pos);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + pos);
            return null;
        }
        machine.initMachine();
        return machine;
    }
}
