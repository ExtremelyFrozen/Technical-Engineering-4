package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.block.machine.DirectionalMachineBlock;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

import java.util.EnumSet;

/**
 * 面配置可选范围的三层判定：
 * <ul>
 * <li><b>面入口</b>（{@code supportsFaceOnSide}）：该世界方向是不是本设备能接容器的面。
 * 默认六面都是；频道只有朝向外侧的那一面。</li>
 * <li><b>设备功能性</b>（{@code supportsFaceDirection}）：六个档位归成「向内 / 向外」两种能力，
 * 能量向外只给产能类、能量向内排除引擎，物品/流体按槽位与罐体的可进可出方向。被动档位同样
 * 要过这层——只进不出的引擎不能出现「被动输出」，只有 XP 输出罐的熔炉不能出现「被动输入」。</li>
 * <li><b>方块标签</b>（{@code no_active_face_energy/_item/_fluid}）：基础模组当前不打条目，
 * 留给整合包整体收回某类资源的主动面。</li>
 * </ul>
 *
 * <p>
 * 这些边界都是「配了也不生效」的静默错误来源：普通机器配了能量主动输出不会推任何电，
 * 无罐机器的流体页无论怎么点都不会有流体进出，频道在非朝向面配置只会让面板看起来串面。
 */
@GameTestHolder(TEN.MOD_ID)
public class FaceModeRangeGameTest {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** 精炼机：能量只能向内，物品与流体双向都在（tank 0 输入、tank 1 输出）。 */
    @TestHolder(value = TEN.MOD_ID + ":machine_face_range_follows_device_function", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void machineFaceRangeFollowsDeviceFunction(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_REFINER.get());
        if (machine == null) return;

        for (int mode : new int[] { FaceOption.OFF, FaceOption.IN, FaceOption.BE_IN }) {
            helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ENERGY, mode),
                    "a processing machine must keep energy input modes: " + FaceOption.toStr(mode));
        }
        for (int mode : new int[] { FaceOption.OUT, FaceOption.BE_OUT, FaceOption.BOTH }) {
            helper.assertFalse(machine.allowsFaceMode(FaceOption.TYPE_ENERGY, mode),
                    "a processing machine has no generation path, energy output modes must be gone: " + FaceOption.toStr(mode));
        }

        for (int mode = FaceOption.OFF; mode < FaceOption.size(); mode++) {
            helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_ITEM, mode),
                    "refiner has in and out slots, item mode must be selectable: " + FaceOption.toStr(mode));
            helper.assertTrue(machine.allowsFaceMode(FaceOption.TYPE_FLUID, mode),
                    "refiner has an input and an output tank, fluid mode must be selectable: " + FaceOption.toStr(mode));
        }
        helper.succeed();
    }

    /** 引擎：纯电源，能量只能向外；燃料槽只进不出，物品只能向内。 */
    @TestHolder(value = TEN.MOD_ID + ":engine_only_pushes_energy", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void engineOnlyPushesEnergy(GameTestHelper helper) {
        CmMachineBlockEntity engine = place(helper, TENBlocks.ENGINE_EXTRACTION.get());
        if (engine == null) return;

        for (int mode : new int[] { FaceOption.OUT, FaceOption.BE_OUT }) {
            helper.assertTrue(engine.allowsFaceMode(FaceOption.TYPE_ENERGY, mode),
                    "engines must keep energy output modes: " + FaceOption.toStr(mode));
        }
        for (int mode : new int[] { FaceOption.IN, FaceOption.BE_IN, FaceOption.BOTH }) {
            helper.assertFalse(engine.allowsFaceMode(FaceOption.TYPE_ENERGY, mode),
                    "charging an engine only bounces energy back out, energy input modes must be gone: " + FaceOption.toStr(mode));
        }

        for (int mode : new int[] { FaceOption.IN, FaceOption.BE_IN }) {
            helper.assertTrue(engine.allowsFaceMode(FaceOption.TYPE_ITEM, mode),
                    "fuel slot accepts items, item input modes must stay: " + FaceOption.toStr(mode));
        }
        for (int mode : new int[] { FaceOption.OUT, FaceOption.BE_OUT, FaceOption.BOTH }) {
            helper.assertFalse(engine.allowsFaceMode(FaceOption.TYPE_ITEM, mode),
                    "engines have no output slot, item output modes must be gone: " + FaceOption.toStr(mode));
        }
        helper.succeed();
    }

    /** 能量单元：储能设备，六个能量档位都有意义（充电、放电、被动进出）。 */
    @TestHolder(value = TEN.MOD_ID + ":cell_keeps_both_energy_directions", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void cellKeepsBothEnergyDirections(GameTestHelper helper) {
        CmMachineBlockEntity cell = place(helper, TENBlocks.CELL.get());
        if (cell == null) return;

        for (int mode = FaceOption.OFF; mode < FaceOption.size(); mode++) {
            helper.assertTrue(cell.allowsFaceMode(FaceOption.TYPE_ENERGY, mode),
                    "cells must keep every energy mode: " + FaceOption.toStr(mode));
        }
        helper.succeed();
    }

    /** 只有 XP 输出罐的机器：流体只能向外，被动输入/双向都不该出现。 */
    @TestHolder(value = TEN.MOD_ID + ":output_only_tank_has_no_fluid_pull", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void outputOnlyTankHasNoFluidPull(GameTestHelper helper) {
        CmMachineBlockEntity smelter = place(helper, TENBlocks.MACHINE_SMELTER.get());
        if (smelter == null) return;

        helper.assertFalse(smelter.tanks.isEmpty(), "smelter is expected to hold the XP tank");
        for (int mode : new int[] { FaceOption.OUT, FaceOption.BE_OUT }) {
            helper.assertTrue(smelter.allowsFaceMode(FaceOption.TYPE_FLUID, mode),
                    "XP tank is an output tank, fluid output modes must stay: " + FaceOption.toStr(mode));
        }
        for (int mode : new int[] { FaceOption.IN, FaceOption.BE_IN, FaceOption.BOTH }) {
            helper.assertFalse(smelter.allowsFaceMode(FaceOption.TYPE_FLUID, mode),
                    "XP tank rejects fluid, fluid input modes must be gone: " + FaceOption.toStr(mode));
        }
        helper.succeed();
    }

    /** 频道方块：向内向外都真实存在，六档全可；且只有自己那一种资源页。 */
    @TestHolder(value = TEN.MOD_ID + ":channel_faces_allow_every_mode", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelFacesAllowEveryMode(GameTestHelper helper) {
        CmMachineBlockEntity channel = place(helper, TENBlocks.CHANNEL_ITEM.get());
        if (channel == null) return;

        for (int mode : new int[] { FaceOption.OFF, FaceOption.IN, FaceOption.OUT, FaceOption.BE_IN,
                FaceOption.BE_OUT, FaceOption.BOTH }) {
            helper.assertTrue(channel.allowsFaceMode(FaceOption.TYPE_ITEM, mode),
                    "item channel is a shared storage, every mode must stay: " + FaceOption.toStr(mode));
        }
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
     * 机器能量页 IN 之后应直接跳到 BE_IN（跳过 OUT / BE_OUT / BOTH）；
     * 频道物品页六档全可，轮换就是老老实实逐个走，BOTH 之后绕回 OFF。
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
                "machine energy page must skip OUT/BE_OUT/BOTH and go IN -> BE_IN, got " + FaceOption.toStr(afterIn));

        int machineItem = FaceOption.nextAllowed(FaceOption.OFF,
                m -> machine.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(machineItem == FaceOption.IN,
                "machine item page must cycle OFF -> IN, got " + FaceOption.toStr(machineItem));

        int channelNext = FaceOption.nextAllowed(FaceOption.OFF,
                m -> channel.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(channelNext == FaceOption.IN,
                "channel item page must cycle OFF -> IN, got " + FaceOption.toStr(channelNext));

        int channelAfterIn = FaceOption.nextAllowed(FaceOption.IN,
                m -> channel.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(channelAfterIn == FaceOption.OUT,
                "channel takes every mode, IN must fall through to OUT, got " + FaceOption.toStr(channelAfterIn));

        // 绕回：频道从 BOTH 继续点应回到 OFF
        int wrapped = FaceOption.nextAllowed(FaceOption.BOTH,
                m -> channel.allowsFaceMode(FaceOption.TYPE_ITEM, m));
        helper.assertTrue(wrapped == FaceOption.OFF,
                "channel item page must wrap BOTH -> OFF, got " + FaceOption.toStr(wrapped));
        helper.succeed();
    }

    /**
     * 逻辑面 → 世界方向必须是双射：六个按钮各自落在一个不同方向，任何朝向都不例外。
     *
     * <p>
     * 回归的是「频道朝上/朝下放置时，面板上改一个面会连带改另一个面」：旧实现把上/下固定成
     * UP/DOWN，朝下放置时「前」与「下」都映射到 DOWN、「后」与「上」都映射到 UP。
     */
    @TestHolder(value = TEN.MOD_ID + ":face_logical_mapping_is_bijective", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void faceLogicalMappingIsBijective(GameTestHelper helper) {
        for (Direction facing : Direction.values()) {
            EnumSet<Direction> seen = EnumSet.noneOf(Direction.class);
            for (int side = 0; side < 6; side++) {
                Direction mapped = CmMachineBlockEntity.faceForLogicalSide(facing, side);
                helper.assertTrue(mapped != null,
                        "logical side " + side + " must map to a direction for facing " + facing);
                if (mapped == null) {
                    return;
                }
                helper.assertTrue(seen.add(mapped),
                        "facing " + facing + ": logical side " + side + " aliases " + mapped);
            }
            helper.assertTrue(seen.size() == 6,
                    "facing " + facing + " must cover all six directions, got " + seen);
        }

        // 水平朝向沿用既有语义：前=正面、后=背面、左=顺时针、右=逆时针、上/下=世界上下
        helper.assertTrue(CmMachineBlockEntity.faceForLogicalSide(Direction.NORTH, 0) == Direction.NORTH,
                "north-facing front must be NORTH");
        helper.assertTrue(CmMachineBlockEntity.faceForLogicalSide(Direction.NORTH, 2) == Direction.EAST,
                "north-facing left must stay EAST (legacy getClockWise)");
        helper.assertTrue(CmMachineBlockEntity.faceForLogicalSide(Direction.NORTH, 4) == Direction.UP,
                "north-facing up must stay UP");
        helper.succeed();
    }

    /**
     * 频道只有朝向外侧的那一面是面入口：其余五面一律只允许关闭，旧档残留的档位在读档时归零。
     *
     * <p>
     * 这一条与上一条共同保证「改一个面不会连带改另一个面」：映射不再重号，死面也不再收配置。
     */
    @TestHolder(value = TEN.MOD_ID + ":channel_only_configures_facing_side", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void channelOnlyConfiguresFacingSide(GameTestHelper helper) {
        // 放在方块顶面时频道朝向 DOWN（模型突出的一侧贴住被点击的面）
        helper.setBlock(POS, TENBlocks.CHANNEL_ITEM.get().defaultBlockState()
                .setValue(DirectionalMachineBlock.FACING, Direction.DOWN));
        CmMachineBlockEntity channel = helper.getBlockEntity(POS);
        if (channel == null) {
            helper.fail("Expected a channel block entity at " + POS);
            return;
        }
        // 读档前就存在的死面配置（背面 UP）必须在 initMachine 的归一化里归零
        channel.itemFaceMode.put(Direction.UP, FaceOption.IN);
        channel.initMachine();

        Direction facing = channel.getFacing();
        helper.assertTrue(facing == Direction.DOWN, "channel must face DOWN, got " + facing);
        helper.assertTrue(channel.allowsFaceMode(facing, FaceOption.TYPE_ITEM, FaceOption.IN),
                "the facing side must keep every item mode");
        helper.assertTrue(channel.itemFaceMode.get(Direction.UP) == FaceOption.OFF,
                "stored mode on a non-facing side must be normalized to OFF, got " + FaceOption.toStr(channel.itemFaceMode.get(Direction.UP)));

        for (Direction side : Direction.values()) {
            if (side == facing) {
                continue;
            }
            for (int mode : new int[] { FaceOption.IN, FaceOption.OUT, FaceOption.BE_IN,
                    FaceOption.BE_OUT, FaceOption.BOTH }) {
                helper.assertFalse(channel.allowsFaceMode(side, FaceOption.TYPE_ITEM, mode),
                        "non-facing side " + side + " must reject " + FaceOption.toStr(mode));
            }
        }
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
