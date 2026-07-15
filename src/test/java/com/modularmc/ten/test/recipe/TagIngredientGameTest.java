package com.modularmc.ten.test.recipe;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import net.neoforged.testframework.gametest.GameTest;

/**
 * RED test: tag ingredients must dynamically resolve items from the current registry,
 * not cache empty matchItems at creation time.
 * <p>
 * This test targets the exact bug: FormsCombinedIngredient.create() with type=tag
 * eagerly calls TagHelper.getItems() which returns empty before PendingTags.apply().
 * After the fix, itemStacks()/symbolItem()/toOriginStackIngredients() must reflect
 * all current tag members at invocation time.
 * <p>
 * Only verifiable at runtime (GameTest server). Compile-only when server not started.
 */
@TestHolder(value = TEN.MOD_ID + ":tag_ingredient_dynamic")
public class TagIngredientGameTest {

    /**
     * Verify that c:ingots/iron tag-based ingredient dynamically resolves to
     * include minecraft:iron_ingot after tags are fully loaded.
     * <p>
     * Before fix: itemStacks() returns empty → GUI/JEI shows blank main slot.
     * After fix: itemStacks() reflects all holders in the tag.
     */
    @GameTest(required = true)
    public void tagIngredientItemStacksIncludesAllTagMembers(ExtendedGameTestHelper helper) {
        // Create a tag ingredient for c:ingots/iron (NeoForge Convention Tag, always present)
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);

        // itemStacks() must dynamically resolve tag members, not return empty
        var stacks = ing.itemStacks();
        helper.assertTrue(!stacks.isEmpty(),
                "itemStacks() for c:ingots/iron must not be empty after tags are loaded");

        boolean hasIron = stacks.stream().anyMatch(s -> s.is(Items.IRON_INGOT));
        helper.assertTrue(hasIron,
                "itemStacks() for c:ingots/iron must include minecraft:iron_ingot");

        // symbolItem() must return a non-empty stack based on current tag
        var symbol = ing.symbolItem();
        helper.assertTrue(!symbol.isEmpty(),
                "symbolItem() for c:ingots/iron must not be empty");
        helper.assertTrue(symbol.is(Items.IRON_INGOT),
                "symbolItem() for c:ingots/iron should be iron_ingot (first tag member)");

        // contains() must match items in the tag dynamically
        helper.assertTrue(ing.contains(Items.IRON_INGOT),
                "contains(iron_ingot) must be true for c:ingots/iron tag ingredient");
        helper.assertFalse(ing.contains(Items.DIAMOND),
                "contains(diamond) must be false for c:ingots/iron tag ingredient");

        // toOriginStackIngredients() must create a full Ingredient from all tag members
        Ingredient origin = ing.toOriginStackIngredients();
        // The origin ingredient should match iron_ingot
        helper.assertTrue(origin.test(new ItemStack(Items.IRON_INGOT)),
                "toOriginStackIngredients() Ingredient must test iron_ingot");
    }

    /**
     * Verify that c:gems/diamond tag ingredient resolves dynamically.
     */
    @GameTest(required = true)
    public void tagIngredientGemDiamond(ExtendedGameTestHelper helper) {
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:gems/diamond", 1.0);

        var stacks = ing.itemStacks();
        helper.assertTrue(!stacks.isEmpty(),
                "itemStacks() for c:gems/diamond must not be empty");

        boolean hasDiamond = stacks.stream().anyMatch(s -> s.is(Items.DIAMOND));
        helper.assertTrue(hasDiamond,
                "itemStacks() for c:gems/diamond must include minecraft:diamond");

        helper.assertTrue(ing.contains(Items.DIAMOND),
                "contains(diamond) must be true for c:gems/diamond");
    }

    /**
     * Verify that c:stones tag (NeoForge 26.1.2 Convention Item Tag) resolves
     * to non-empty members including minecraft:stone.
     */
    @GameTest(required = true)
    public void tagIngredientStonesIncludesStone(ExtendedGameTestHelper helper) {
        // c:stones is a non-empty NeoForge 26.1.2 convention item tag
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:stones", 1.0);

        // Must resolve to non-empty items
        var stacks = ing.itemStacks();
        helper.assertTrue(!stacks.isEmpty(),
                "itemStacks() for c:stones must not be empty (NeoForge convention tag)");

        boolean hasStone = stacks.stream().anyMatch(s -> s.is(Items.STONE));
        helper.assertTrue(hasStone,
                "itemStacks() for c:stones must include minecraft:stone");

        // symbolItem must return a non-empty item
        var symbol = ing.symbolItem();
        helper.assertTrue(!symbol.isEmpty(),
                "symbolItem() for c:stones must not be empty");

        // contains must match items in the tag dynamically
        helper.assertTrue(ing.contains(Items.STONE),
                "contains(stone) must be true for c:stones tag ingredient");
        helper.assertTrue(ing.contains(Items.ANDESITE) || ing.contains(Items.GRANITE) || ing.contains(Items.DIORITE),
                "contains must match common stone variants for c:stones");
    }

    /**
     * Regression: direct item (type=item) still works correctly.
     */
    @GameTest(required = true)
    public void directItemIngredientRegression(ExtendedGameTestHelper helper) {
        var ing = FormsCombinedIngredient.create(4, "item", "item", "minecraft:redstone", 1.0);

        var stacks = ing.itemStacks();
        helper.assertTrue(stacks.size() == 1,
                "direct item ingredient must have 1 itemStack");
        helper.assertTrue(stacks.get(0).is(Items.REDSTONE),
                "direct item ingredient must be redstone");
        helper.assertTrue(stacks.get(0).getCount() == 4,
                "direct item ingredient count must be 4");

        var symbol = ing.symbolItem();
        helper.assertTrue(symbol.is(Items.REDSTONE),
                "symbolItem() for direct item must be redstone");

        helper.assertTrue(ing.contains(Items.REDSTONE),
                "contains(redstone) must be true for direct item ingredient");
        helper.assertFalse(ing.contains(Items.DIAMOND),
                "contains(diamond) must be false for direct item ingredient");

        // toOriginStackIngredients must work
        Ingredient origin = ing.toOriginStackIngredients();
        helper.assertTrue(origin.test(new ItemStack(Items.REDSTONE, 4)),
                "toOriginStackIngredients Ingredient must test redstone");
    }

    /**
     * Regression: static item (type=static) still works correctly.
     */
    @GameTest(required = true)
    public void staticItemIngredientRegression(ExtendedGameTestHelper helper) {
        var ing = FormsCombinedIngredient.create(1, "item", "static", "minecraft:iron_ingot", 0.5);

        var stacks = ing.itemStacks();
        helper.assertTrue(stacks.size() == 1,
                "static item ingredient must have 1 itemStack");
        helper.assertTrue(stacks.get(0).is(Items.IRON_INGOT),
                "static item ingredient must be iron_ingot");

        helper.assertTrue(ing.contains(Items.IRON_INGOT),
                "contains(iron_ingot) must be true for static item ingredient");
    }

    /**
     * Verify that fluid tag ingredient (minecraft:water) dynamically resolves
     * to include minecraft:water and minecraft:flowing_water.
     */
    @GameTest(required = true)
    public void fluidTagIngredientResolvesWater(ExtendedGameTestHelper helper) {
        // minecraft:water is the vanilla fluid tag including water and flowing_water
        var ing = FormsCombinedIngredient.create(1000, "fluid", "tag", "minecraft:water", 1.0);

        // fluidStacks() must dynamically resolve tag members
        var stacks = ing.fluidStacks();
        helper.assertTrue(!stacks.isEmpty(),
                "fluidStacks() for minecraft:water must not be empty");

        boolean hasWater = stacks.stream().anyMatch(s -> s.getFluid() == Fluids.WATER);
        helper.assertTrue(hasWater,
                "fluidStacks() for minecraft:water must include Fluids.WATER");

        // symbolFluid must return a non-empty stack
        var symbol = ing.symbolFluid();
        helper.assertTrue(!symbol.isEmpty(),
                "symbolFluid() for minecraft:water must not be empty");
        helper.assertTrue(symbol.getFluid() == Fluids.WATER,
                "symbolFluid() for minecraft:water should be Fluids.WATER");

        // contains must match fluids in the tag dynamically
        helper.assertTrue(ing.contains(Fluids.WATER),
                "contains(water) must be true for minecraft:water tag");
        helper.assertFalse(ing.contains(Fluids.LAVA),
                "contains(lava) must be false for minecraft:water tag");
    }
}
