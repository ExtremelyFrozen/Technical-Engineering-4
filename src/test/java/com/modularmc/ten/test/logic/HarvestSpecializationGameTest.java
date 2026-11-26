package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;
import com.modularmc.ten.common.item.upgrades.LevelupOddGrowth;
import com.modularmc.ten.common.item.upgrades.LevelupReseed;
import com.modularmc.ten.common.item.upgrades.UpgradeConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

import java.util.HashSet;
import java.util.Set;

/**
 * 农场收获独特组件的契约。
 *
 * <p>
 * 两张卡都在「收获完成之后」触发，因此都不改变产量本身，只改变收获带来的外溢：
 * 奇异催生从周围 8 格随机选 1 株作物推进 1 个生长刻度，幼芽复种按概率把种子铺到四邻空耕地。
 * 二者互斥由 {@code SPECIALIZATION} 的整机唯一语义承担，不另建互斥表。
 *
 * <p>
 * 标记查询（{@code hasUpgrade}）只认本 tick 生效的那一张：旧存档里同时插着两张时，
 * {@code applyUpgradeEffects} 取槽位序第一张，另一张必须查不到——否则两台逻辑会同时跑，
 * 8 邻催生与四邻蔓延叠在一起，等于绕过互斥。
 */
@GameTestHolder(TEN.MOD_ID)
public class HarvestSpecializationGameTest {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** 农场只认两张收获卡，且整机唯一；粉碎机的副产物卡必须被能力门禁挡下。 */
    @TestHolder(value = TEN.MOD_ID + ":farm_accepts_one_harvest_specialization", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void farmAcceptsOneHarvestSpecialization(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_FARM.get());
        if (machine == null) return;

        helper.assertTrue(machine.supportsHarvestSpecialization(), "the farm manager must advertise harvest specialization");
        helper.assertTrue(!machine.supportsByproductTuning(), "the farm manager must not advertise byproduct tuning");

        ItemStack oddGrowth = TENItems.ODDGROWTH_LEVELUP.get().getDefaultInstance();
        ItemStack reseed = TENItems.RESPROUT_LEVELUP.get().getDefaultInstance();
        ItemStack press = TENItems.PRESS_LEVELUP.get().getDefaultInstance();

        helper.assertTrue(machine.validUpgrade(0, oddGrowth), "an empty farm must accept the odd growth card");
        helper.assertTrue(machine.validUpgrade(0, reseed), "an empty farm must accept the reseed card");
        helper.assertTrue(!machine.validUpgrade(0, press),
                "a byproduct card must be rejected by the farm's capability gate");

        machine.upgradeHandler.setStackInSlot(0, oddGrowth.copy());
        helper.assertTrue(!machine.validUpgrade(1, reseed),
                "odd growth and reseed are mutually exclusive: the second specialization must be rejected");
        helper.assertTrue(!machine.validUpgrade(1, oddGrowth.copy()),
                "the same specialization must not be installed twice");

        machine.upgradeHandler.setStackInSlot(0, ItemStack.EMPTY);
        machine.upgradeHandler.setStackInSlot(0, reseed.copy());
        helper.assertTrue(!machine.validUpgrade(1, oddGrowth.copy()),
                "the exclusion must hold in the other direction too");

        helper.succeed();
    }

    /** 旧档同时插着两张时，只有槽位序第一张生效，另一张的 marker 必须查不到。 */
    @TestHolder(value = TEN.MOD_ID + ":harvest_marker_picks_a_single_card", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void harvestMarkerPicksASingleCard(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_FARM.get());
        if (machine == null) return;

        machine.upgradeHandler.setStackInSlot(0, TENItems.ODDGROWTH_LEVELUP.get().getDefaultInstance());
        machine.doBaseData();
        helper.assertTrue(machine.hasUpgrade(LevelupOddGrowth.class), "the installed odd growth card must be visible");
        helper.assertTrue(!machine.hasUpgrade(LevelupReseed.class), "the reseed marker must stay off");

        // 构造两张独特组件同时存在的旧存档布局。
        machine.upgradeHandler.setStackInSlot(1, TENItems.RESPROUT_LEVELUP.get().getDefaultInstance());
        machine.doBaseData();
        helper.assertTrue(machine.hasUpgrade(LevelupOddGrowth.class),
                "the first specialization in slot order must stay active");
        helper.assertTrue(!machine.hasUpgrade(LevelupReseed.class),
                "the second specialization must not run at the same time");

        machine.upgradeHandler.setStackInSlot(0, ItemStack.EMPTY);
        machine.upgradeHandler.setStackInSlot(1, TENItems.RESPROUT_LEVELUP.get().getDefaultInstance());
        machine.doBaseData();
        helper.assertTrue(machine.hasUpgrade(LevelupReseed.class), "the reseed card must take over once it is the only one");
        helper.assertTrue(!machine.hasUpgrade(LevelupOddGrowth.class), "the odd growth marker must stay off");

        helper.succeed();
    }

    /** 蔓延只走四邻，不含对角；催生只选 1 株并推进 1 格年龄，概率落在 (0,1) 内。 */
    @TestHolder(value = TEN.MOD_ID + ":harvest_constants_stay_in_shape", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void harvestConstantsStayInShape(GameTestHelper helper) {
        int[][] offsets = UpgradeConstants.FARM_RESEED_OFFSETS;
        helper.assertTrue(offsets.length == 4, "reseeding must cover exactly four neighbours, got " + offsets.length);

        Set<Long> seen = new HashSet<>();
        for (int[] offset : offsets) {
            int dx = offset[0];
            int dz = offset[1];
            helper.assertTrue(Math.abs(dx) + Math.abs(dz) == 1,
                    "reseeding must only use the four orthogonal neighbours, got (" + dx + "," + dz + ")");
            helper.assertTrue(seen.add(((long) dx << 32) ^ (dz & 0xFFFFFFFFL)),
                    "duplicate reseed offset (" + dx + "," + dz + ")");
        }

        helper.assertTrue(UpgradeConstants.FARM_RESEED_CHANCE > 0.0 && UpgradeConstants.FARM_RESEED_CHANCE < 1.0,
                "the reseed chance must be a real probability, got " + UpgradeConstants.FARM_RESEED_CHANCE);
        helper.assertTrue(UpgradeConstants.FARM_GROW_TARGETS == 1,
                "odd growth must pick exactly one crop among the eight neighbours, got " + UpgradeConstants.FARM_GROW_TARGETS);
        helper.assertTrue(UpgradeConstants.FARM_GROW_AGE_STEPS == 1,
                "odd growth must advance exactly one age step, got " + UpgradeConstants.FARM_GROW_AGE_STEPS);

        helper.succeed();
    }

    private static CmMachineBlockEntity place(GameTestHelper helper, Block block) {
        helper.setBlock(POS, block);
        CmMachineBlockEntity machine = helper.getBlockEntity(POS);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + POS);
            return null;
        }
        machine.initMachine();
        return machine;
    }
}
