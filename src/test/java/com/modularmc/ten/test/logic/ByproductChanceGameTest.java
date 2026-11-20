package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;
import com.modularmc.ten.common.item.upgrades.UpgradeConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 副产物倍率与粉碎机独特组件的契约。
 *
 * <p>
 * 两张卡都只改「概率输出」的结算，不碰确定产出与催化剂：{@code chance >= 1} 是主产物，
 * {@code chance <= 0} 是催化剂修饰符（不消耗），只有 {@code 0 < chance < 1} 才是副产物。
 * 压榨把概率乘 0.5 同时把份数乘 3（期望 ×1.5，以更少的高份产出换取更高收益），
 * 筛选把概率乘 2 并在 100% 处封顶——封顶必须发生在乘完之后，否则 0.6 会算成 1.2 再溢出成必不出。
 *
 * <p>
 * 倍率由机器侧按已装卡决定，预检（{@code canFitOutputs*}）与提交（{@code collectBatchOutputs}）
 * 必须取同一个值；本测试锁的是倍率本身的语义边界，避免预检按 2 倍、提交按 1 倍这类错位。
 */
@GameTestHolder(TEN.MOD_ID)
public class ByproductChanceGameTest {

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final double EPS = 1e-9;

    /** 倍率钳位到 [0, 1]；非法倍率退回原概率，保证既有配方不受影响。 */
    @TestHolder(value = TEN.MOD_ID + ":byproduct_chance_multiplier_clamps", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void byproductChanceMultiplierClamps(GameTestHelper helper) {
        helper.assertTrue(UpgradeConstants.BYPRODUCT_PRESS_COUNT == 3,
                "press must triple byproduct output after halving its chance, got " + UpgradeConstants.BYPRODUCT_PRESS_COUNT);

        FormsCombinedIngredient byproduct = FormsCombinedIngredient.create(1, "item", "static", "minecraft:iron_ingot", 0.6);

        assertChance(helper, byproduct, 1.0, 0.6, "1x must keep the recipe chance");
        assertChance(helper, byproduct, 0.5, 0.3, "press must halve the byproduct chance");
        assertChance(helper, byproduct, 2.0, 1.0, "filter must clamp 0.6 x 2 to 1.0, not overflow");
        assertChance(helper, byproduct, 3.0, 1.0, "any multiplier above the cap still clamps to 1.0");
        assertChance(helper, byproduct, 0.0, 0.0, "a zero multiplier must remove the byproduct entirely");

        assertChance(helper, byproduct, Double.NaN, 0.6, "NaN must fall back to the recipe chance");
        assertChance(helper, byproduct, Double.POSITIVE_INFINITY, 0.6, "infinity must fall back to the recipe chance");
        assertChance(helper, byproduct, -0.5, 0.6, "a negative multiplier must fall back to the recipe chance");

        // 分流责任在调用方：collectBatchOutputs 先按 chance >= 1 走确定产出分支，只给概率输出传倍率。
        // effectiveChance 保持纯乘法钳位——若把「是否为副产物」塞进来，催化剂与确定产出的语义
        // 就会被一个数值函数同时决定，两个调用点迟早会分叉。
        FormsCombinedIngredient certain = FormsCombinedIngredient.create(1, "item", "static", "minecraft:iron_ingot", 1.0);
        assertChance(helper, certain, 0.5, 0.5,
                "effectiveChance must stay a pure clamp; callers own the certain-vs-byproduct split");
        FormsCombinedIngredient catalyst = FormsCombinedIngredient.create(1, "item", "static", "minecraft:iron_ingot", 0.0);
        assertChance(helper, catalyst, 2.0, 0.0, "a catalyst must stay a catalyst under filter");

        helper.succeed();
    }

    /** 产出次数按有效概率结算：倍率把 0.5 推到 1.0 后必出，压到 0.25 后只有 1/4 的掷骰命中。 */
    @TestHolder(value = TEN.MOD_ID + ":byproduct_gen_item_honours_multiplier", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void byproductGenItemHonoursMultiplier(GameTestHelper helper) {
        FormsCombinedIngredient byproduct = FormsCombinedIngredient.create(1, "item", "static", "minecraft:iron_ingot", 0.5);

        helper.assertTrue(!byproduct.genItem(2.0, () -> 0.99).isEmpty(),
                "0.5 x 2 = 1.0 must always produce, even against a 0.99 roll");
        helper.assertTrue(byproduct.genItem(0.5, () -> 0.3).isEmpty(),
                "0.5 x 0.5 = 0.25 must miss a 0.3 roll");
        helper.assertTrue(!byproduct.genItem(0.5, () -> 0.2).isEmpty(),
                "0.5 x 0.5 = 0.25 must hit a 0.2 roll");
        helper.assertTrue(!byproduct.genItem(1.0, () -> 0.49).isEmpty(),
                "an unmodified 0.5 must hit a 0.49 roll");
        helper.assertTrue(byproduct.genItem(1.0, () -> 0.51).isEmpty(),
                "an unmodified 0.5 must miss a 0.51 roll");

        helper.succeed();
    }

    /** rolls > 1 时每次独立掷骰，份数 = 成功次数 × 单次数量；概率为 0 时整体为空。 */
    @TestHolder(value = TEN.MOD_ID + ":byproduct_rolls_accumulate", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void byproductRollsAccumulate(GameTestHelper helper) {
        FormsCombinedIngredient rolled = FormsCombinedIngredient.create(2, "item", "static", "minecraft:iron_ingot", 0.5, 4);

        ItemStack all = rolled.genItem(2.0, () -> 0.0);
        helper.assertTrue(all.getCount() == 8,
                "4 rolls x 2 count must yield 8 when the multiplier makes every roll succeed, got " + all.getCount());

        helper.assertTrue(rolled.genItem(0.0, () -> 0.0).isEmpty(),
                "a zero multiplier must make every roll miss");

        FormsCombinedIngredient half = FormsCombinedIngredient.create(1, "item", "static", "minecraft:iron_ingot", 0.5, 4);
        int successes = 0;
        for (int i = 0; i < 4; i++) {
            if (!half.genItem(1.0, () -> 0.4).isEmpty()) successes++;
        }
        helper.assertTrue(successes == 4, "a fixed 0.4 roll must hit all four 0.5 rolls, got " + successes);

        helper.succeed();
    }

    /** 超出单槽绝对上限时 fail-fast，而不是静默截断丢物品。 */
    @TestHolder(value = TEN.MOD_ID + ":byproduct_overstack_fails_fast", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void byproductOverstackFailsFast(GameTestHelper helper) {
        FormsCombinedIngredient fat = FormsCombinedIngredient.create(50, "item", "static", "minecraft:iron_ingot", 1.0, 4);
        boolean threw = false;
        try {
            fat.genItem(1.0, () -> 0.0);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        helper.assertTrue(threw, "4 rolls x 50 count exceeds the absolute slot cap and must throw");
        helper.succeed();
    }

    /** 粉碎机只认两张副产物卡，且整机唯一：第二张独特组件在放入时就被拒。 */
    @TestHolder(value = TEN.MOD_ID + ":pulverizer_accepts_one_byproduct_specialization", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void pulverizerAcceptsOneByproductSpecialization(GameTestHelper helper) {
        CmMachineBlockEntity machine = place(helper, TENBlocks.MACHINE_PULVERIZER.get());
        if (machine == null) return;

        helper.assertTrue(machine.supportsByproductTuning(), "the pulverizer must advertise byproduct tuning");
        helper.assertTrue(!machine.supportsHarvestSpecialization(), "the pulverizer must not advertise harvest specialization");

        ItemStack press = TENItems.PRESS_LEVELUP.get().getDefaultInstance();
        ItemStack filter = TENItems.FILTER_LEVELUP.get().getDefaultInstance();
        ItemStack oddGrowth = TENItems.ODDGROWTH_LEVELUP.get().getDefaultInstance();

        helper.assertTrue(machine.validUpgrade(0, press), "an empty pulverizer must accept the press card");
        helper.assertTrue(machine.validUpgrade(0, filter), "an empty pulverizer must accept the filter card");
        helper.assertTrue(!machine.validUpgrade(0, oddGrowth),
                "a harvest card must be rejected by the pulverizer's capability gate");

        machine.upgradeHandler.setStackInSlot(0, press.copy());
        helper.assertTrue(!machine.validUpgrade(1, filter),
                "press and filter are mutually exclusive: the second specialization must be rejected");
        helper.assertTrue(!machine.validUpgrade(1, press.copy()),
                "the same specialization must not be installed twice");

        machine.upgradeHandler.setStackInSlot(0, ItemStack.EMPTY);
        machine.upgradeHandler.setStackInSlot(0, filter.copy());
        helper.assertTrue(!machine.validUpgrade(1, press.copy()),
                "the exclusion must hold in the other direction too");

        helper.succeed();
    }

    private static void assertChance(GameTestHelper helper, FormsCombinedIngredient ing, double multiplier, double expected, String message) {
        double actual = ing.effectiveChance(multiplier);
        helper.assertTrue(Math.abs(actual - expected) < EPS,
                message + " (multiplier=" + multiplier + ", expected=" + expected + ", got=" + actual + ")");
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
