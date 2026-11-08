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
 * 面配置可选范围的两层判定：
 * <ul>
 * <li><b>设备功能性</b>（{@code supportsActiveDirection}）：能量输出只给产能类，能量输入排除
 * 引擎，物品/流体按槽位与罐体的可进可出方向；</li>
 * <li><b>方块标签</b>（{@code no_active_face_energy/_item/_fluid}）：共享存储型方块（频道）
 * 整套资源都不许自搬。</li>
 * </ul>
 *
 * <p>
 * 这两层都是「配了也不生效」的静默错误来源：普通机器配了能量主动输出不会推任何电，
 * 频道方块的共享存储不接受机器自搬，无罐机器的流体页无论怎么点都不会有流体进出。
 */
@GameTestHolder(TEN.MOD_ID)
public class FaceModeRangeGameTest {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** 精炼机：能量只能主动输入，物品与流体双向都在（tank 0 输入、tank 1 输出）。 */
    @TestHolder(value = TEN.MOD_ID + ":machine_face_range_follows_device_function", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void machineFaceRangeFollowsDeviceFunction(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_REFINER.get());
        if (machine == null) return;

        helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.IN),
                "a processing machine must keep active energy pull");
        helper.assertFalse(machine.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.OUT),
                "a processing machine has no generation path, active energy push must be gone");
        helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.BE_OUT),
                "passive energy extraction stays: cables and pipes pull from passive faces");
        for (int mode : new int[] { FaceOption.OFF, FaceOption.BE_IN, FaceOption.BE_OUT, FaceOption.BOTH }) {
            helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ENERGY, mode),
                    "energy mode must stay selectable: " + FaceOption.toStr(mode));
        }

        for (int mode = FaceOption.OFF; mode < FaceOption.size(); mode++) {
            helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ITEM, mode),
                    "refiner has in and out slots, item mode must be selectable: " + FaceOption.toStr(mode));
            helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_FLUID, mode),
                    "refiner has an input and an output tank, fluid mode must be selectable: " + FaceOption.toStr(mode));
        }
        helper.succeed();
    }

    /** 引擎：纯电源，主动输出在、主动输入不在；燃料槽只进不出，物品主动输出也不在。 */
    @TestHolder(value = TEN.MOD_ID + ":engine_only_pushes_energy", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void engineOnlyPushesEnergy(GameTestHelper helper) {
        CmMachineBlockEntity engine = place(helper, TENBlocks.ENGINE_EXTRACTION.get());
        if (engine == null) return;

        helper.assertTrue(engine.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.OUT),
                "engines must keep active energy push");
        helper.assertFalse(engine.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.IN),
                "charging an engine only bounces energy back out, active pull must be gone");
        helper.assertTrue(engine.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.IN),
                "fuel slot accepts items, item input must stay");
        helper.assertFalse(engine.allowsFaceMode(FaceOption.TYPE_ITEM, FaceOption.OUT),
                "engines have no output slot, item output must be gone");
        helper.succeed();
    }

    /** 能量单元：储能设备，两个方向都有意义（充电靠主动输入，放电靠主动输出）。 */
    @TestHolder(value = TEN.MOD_ID + ":cell_keeps_both_energy_directions", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void cellKeepsBothEnergyDirections(GameTestHelper helper) {
        CmMachineBlockEntity cell = place(helper, TENBlocks.CELL.get());
        if (cell == null) return;

        helper.assertTrue(cell.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.IN),
                "cells must be chargeable through an active input face");
        helper.assertTrue(cell.allowsFaceMode(FaceOption.TYPE_ENERGY, FaceOption.OUT),
                "cells must be able to push energy out");
        helper.succeed();
    }

    /** 只有 XP 输出罐的机器：流体主动输出在，主动输入不在（罐体本身不收）。 */
    @TestHolder(value = TEN.MOD_ID + ":output_only_tank_has_no_fluid_pull", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void outputOnlyTankHasNoFluidPull(GameTestHelper helper) {
        CmMachineBlockEntity smelter = place(helper, TENBlocks.MACHINE_SMELTER.get());
        if (smelter == null) return;

        helper.assertFalse(smelter.tanks.isEmpty(), "smelter is expected to hold the XP tank");
        helper.assertTrue(smelter.allowsFaceMode(FaceOption.TYPE_FLUID, FaceOption.OUT),
                "XP tank is an output tank, fluid push must stay");
        helper.assertFalse(smelter.allowsFaceMode(FaceOption.TYPE_FLUID, FaceOption.IN),
                "XP tank rejects fluid, active fluid pull must be gone");
        helper.succeed();
    }

    /** 频道方块带 no_active_face_item：六面只能配被动三态，主动输入/输出必须被挡掉。 */
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

    /**
     * 轮换是「按设备现算的活列表」：被去掉的方向不占轮换位。
     * 机器能量页 IN 之后应直接跳到 BE_IN（跳过 OUT）；频道物品页 OFF 之后应直接到 BE_IN。
     */
    @TestHolder(value = TEN.MOD_ID + ":face_cycle_skips_denied_directions", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void faceCycleSkipsDeniedDirections(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_PULVERIZER.get());
        CmMachineBlockEntity channel = place(helper, TENBlocks.CHANNEL_ITEM.get(), POS.east(2));
        if (machine == null || channel == null) return;

        int afterIn = FaceOption.nextAllowed(FaceOption.IN,
                m -> machine.allowsFaceMode(FaceOption.TYPE_ENERGY, m));
        helper.assertTrue(afterIn == FaceOption.BE_IN,
                "machine energy page must skip OUT and go IN -> BE_IN, got " + FaceOption.toStr(afterIn));

        int machineItem = FaceOption.nextAllowed(FaceOption.OFF,
                m -> machine.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(machineItem == FaceOption.IN,
                "machine item page must cycle OFF -> IN, got " + FaceOption.toStr(machineItem));

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
