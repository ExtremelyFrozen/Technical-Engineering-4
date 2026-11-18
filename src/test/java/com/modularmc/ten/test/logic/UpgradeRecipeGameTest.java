package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

import java.util.Optional;

/**
 * 十六张新增升级组件的合成配方必须真的进到配方管理器里。
 *
 * <p>
 * 配方是手写 JSON（{@code data/kenergyengineering/recipe/vanilla/upgrades/}），不经过数据生成，
 * 拼错一个物品 id 或漏掉 pattern 字符时游戏只会安静地丢掉这条配方——玩家看到的是「合成表里没有」，
 * 而不是加载报错。这里直接向 {@code RecipeManager} 要 id，等价于在游戏里打开配方书查一遍。
 */
@GameTestHolder(TEN.MOD_ID)
public class UpgradeRecipeGameTest {

    /** 路径与 JSON 文件位置一一对应：{@code data/<ns>/recipe/vanilla/upgrades/<name>.json}。 */
    private static final String[][] RECIPES = {
            { "lev_speed", "speed_levelup" },
            { "lev_speed_2", "speed_levelup_2" },
            { "lev_speed_3", "speed_levelup_3" },
            { "lev_duration", "duration_levelup" },
            { "lev_duration_2", "duration_levelup_2" },
            { "lev_duration_3", "duration_levelup_3" },
            { "lev_efficiency", "efficiency_levelup" },
            { "lev_efficiency_2", "efficiency_levelup_2" },
            { "lev_efficiency_3", "efficiency_levelup_3" },
            { "lev_parallel", "parallel_levelup" },
            { "lev_parallel_2", "parallel_levelup_2" },
            { "lev_parallel_3", "parallel_levelup_3" },
            { "lev_press", "press_levelup" },
            { "lev_filter", "filter_levelup" },
            { "lev_oddgrowth", "oddgrowth_levelup" },
            { "lev_resprout", "resprout_levelup" }
    };

    @TestHolder(value = TEN.MOD_ID + ":upgrade_recipes_are_loaded", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void upgradeRecipesAreLoaded(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        var itemRegistry = registryAccess.registryOrThrow(Registries.ITEM);

        for (String[] entry : RECIPES) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, "vanilla/upgrades/" + entry[0]);
            Optional<RecipeHolder<?>> holder = helper.getLevel().getRecipeManager().getRecipes().stream()
                    .filter(h -> h.id().equals(id))
                    .findFirst();
            helper.assertTrue(holder.isPresent(), "recipe " + id + " must be loaded from the data pack");
            if (holder.isEmpty()) continue;

            ResourceLocation expected = ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, entry[1]);
            ItemStack result = holder.get().value().getResultItem(registryAccess);
            helper.assertTrue(result.is(itemRegistry.get(expected)),
                    "recipe " + id + " must craft " + expected + ", got " + result);
            helper.assertTrue(result.getCount() == 1,
                    "upgrade cards are crafted one at a time, got " + result.getCount());
        }

        helper.succeed();
    }
}
