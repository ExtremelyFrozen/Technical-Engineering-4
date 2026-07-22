package com.modularmc.ten.api.recipe;

import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.DoubleSupplier;

public class FormsCombinedIngredient {

    boolean ALLOW_ALL;

    public boolean isAllowAll() {
        return ALLOW_ALL;
    }

    String type;
    String form;
    Collection<Item> matchItems = new ArrayList<>();
    TagKey<Item> ifTagItem;
    Collection<Fluid> matchFluids = new ArrayList<>();
    TagKey<Fluid> ifTagFluid;
    Identifier key;
    int amountOrCount;
    double chance;
    int rolls = 1;

    /**
     * Threshold below which chance is displayed in overlay/tooltip.
     * Values at or above this are treated as "certain" (100%).
     * Accounts for floating-point imprecision near 1.0.
     */
    private static final double CHANCE_CERTAIN_THRESHOLD = 1.0d - 1e-12;

    public int amountOrCount() {
        return amountOrCount;
    }

    public int rolls() {
        return rolls;
    }

    public String type() {
        return type;
    }

    public String form() {
        return form;
    }

    public Identifier key() {
        return key;
    }

    public double chance() {
        return chance;
    }

    /**
     * @return overlay chance text like {@code "40%"}, or {@code null} if chance is at or
     *         above the certainty threshold (no overlay needed). Rounded to integer percentage.
     */
    public String chanceOverlayText() {
        if (chance >= CHANCE_CERTAIN_THRESHOLD) return null;
        return Math.round(chance * 100.0d) + "%";
    }

    /**
     * @return overlay rolls text like {@code "R9"}, or {@code null} if rolls <= 1
     *         (no overlay needed).
     */
    public String rollsOverlayText() {
        if (rolls <= 1) return null;
        return "R" + rolls;
    }

    /**
     * Describes what kind of rich tooltip callback should be registered for a
     * slot in JEI/EMI integration, based purely on this ingredient's chance and
     * rolls values. This is a pure function — no JEI runtime required.
     */
    public enum TooltipKind {
        /** No tooltip callback needed. */
        NONE,
        /** OUTPUT + chance &lt; 1.0: translates to {@code kenergyengineering.jei_addition_chance}. */
        CHANCE_ONLY,
        /**
         * OUTPUT + chance &lt; 1.0 + rolls &gt; 1: translates to
         * {@code kenergyengineering.jei_addition_chance_rolls}.
         */
        CHANCE_WITH_ROLLS,
        /** INPUT + chance &le; 0: translates to {@code kenergyengineering.not_consumed}. */
        NOT_CONSUMED
    }

    /**
     * Determines the {@link TooltipKind} for JEI/EMI tooltip registration.
     * Pure function, no JEI runtime dependencies.
     *
     * @param isOutput {@code true} if this ingredient is in an OUTPUT slot,
     *                 {@code false} for INPUT
     */
    public TooltipKind tooltipKind(boolean isOutput) {
        if (isOutput && chance < CHANCE_CERTAIN_THRESHOLD) {
            return rolls > 1 ? TooltipKind.CHANCE_WITH_ROLLS : TooltipKind.CHANCE_ONLY;
        }
        if (!isOutput && chance <= 0) {
            return TooltipKind.NOT_CONSUMED;
        }
        return TooltipKind.NONE;
    }

    public List<ItemStack> itemStacks() {
        if (ALLOW_ALL) return List.of(ItemStack.EMPTY);
        if ("tag".equals(type)) {
            // Dynamic resolution from current registry tag — do NOT cache at creation time.
            // Tags may not be populated when recipe is deserialized (before PendingTags.apply()).
            var tagContents = BuiltInRegistries.ITEM.getTagOrEmpty(ifTagItem);
            List<ItemStack> result = new ArrayList<>();
            for (var holder : tagContents) {
                result.add(new ItemStack(holder.value(), amountOrCount));
            }
            return result;
        }
        return matchItems.stream().map(i -> new ItemStack(i, amountOrCount)).toList();
    }

    public List<FluidStack> fluidStacks() {
        if (ALLOW_ALL) return List.of(FluidStack.EMPTY);
        if ("tag".equals(type)) {
            // Dynamic resolution from current registry tag — do NOT cache at creation time.
            var tagContents = BuiltInRegistries.FLUID.getTagOrEmpty(ifTagFluid);
            List<FluidStack> result = new ArrayList<>();
            for (var holder : tagContents) {
                result.add(new FluidStack(holder.value(), amountOrCount));
            }
            return result;
        }
        return matchFluids.stream().map(f -> new FluidStack(f, amountOrCount)).toList();
    }

    public boolean contains(Item i) {
        return switch (type) {
            case "tag" -> TagHelper.containsItem(i, ifTagItem);
            case "static" -> matchItems.contains(i);
            case "item" -> matchItems.contains(i);
            default -> false;
        };
    }

    public boolean contains(Fluid f) {
        return switch (type) {
            case "tag" -> TagHelper.containsFluid(f, ifTagFluid);
            case "static" -> matchFluids.contains(f);
            case "item" -> matchFluids.contains(f);
            default -> false;
        };
    }

    public boolean check(IngredientTypeGetter slotType, IngredientTypeGetter tankType, IItemHandler inv, List<? extends IFluidHandler> tanks) {
        if (ALLOW_ALL) return true;
        return switch (form) {
            case "item" -> {
                for (int i = 0; i < inv.getSlots(); i++) {
                    if (!slotType.get(i).canIn()) continue;
                    ItemStack stack = inv.getStackInSlot(i);
                    if (stack.getCount() < amountOrCount) continue;
                    if (contains(stack.getItem())) yield true;
                }
                yield false;
            }
            case "fluid" -> {
                for (int i = 0; i < tanks.size(); i++) {
                    if (!tankType.get(i).canIn()) continue;
                    FluidStack stack = tanks.get(i).getFluidInTank(0);
                    if (stack.getAmount() < amountOrCount) continue;
                    if (contains(stack.getFluid())) yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    public Ingredient toOriginStackIngredients() {
        if ("tag".equals(type)) {
            // Create a proper tag-based Ingredient from the entire tag.
            // In NeoForge 26.1.2, Ingredient.of(HolderSet<Item>) is available.
            // getTagOrEmpty returns HolderSet.Named<Item> which extends HolderSet<Item>,
            // but the compiler sees the erased return type as Iterable — cast explicitly.
            if (ifTagItem == null) return Ingredient.of();
            var tagContents = BuiltInRegistries.ITEM.getTagOrEmpty(ifTagItem);
            if (!tagContents.iterator().hasNext()) return Ingredient.of();
            return Ingredient.of((HolderSet<Item>) tagContents);
        }
        return Ingredient.of(itemStacks().stream().map(ItemStack::getItem).toArray(n -> new Item[n]));
    }

    // Parsing
    public static FormsCombinedIngredient parseFrom(JsonObject json) {
        String form = JsonParser.getString(json, "form");
        String type = JsonParser.getString(json, "type");
        String key = JsonParser.getString(json, "key");
        int limit = switch (form) {
            case "fluid" -> JsonParser.getIntOr(json, "amount", 0);
            default -> JsonParser.getIntOr(json, "count", 1);
        };
        double chance = JsonParser.getFloatOr(json, "chance", 1);
        int rolls = JsonParser.getIntOr(json, "rolls", 1);
        return create(limit, form, type, key, chance, rolls);
    }

    public static FormsCombinedIngredient create(int limit, String form, String type, String key, double chance) {
        return create(limit, form, type, key, chance, 1);
    }

    public static FormsCombinedIngredient create(int limit, String form, String type, String key, double chance, int rolls) {
        // ── Rolls validation ──
        if (rolls < 1) throw new IllegalArgumentException("rolls must be >= 1, got: " + rolls);
        if ("fluid".equals(form) && rolls > 1) {
            throw new IllegalArgumentException("Fluid output ingredient must have rolls=1, but got rolls=" + rolls + " for key=" + key);
        }

        // ── Chance validation: must be finite and in [0, 1] ──
        if (Double.isNaN(chance) || Double.isInfinite(chance)) {
            throw new IllegalArgumentException("chance must be a finite value, got: " + chance);
        }
        if (chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("chance must be in [0, 1], got: " + chance);
        }

        var ing = new FormsCombinedIngredient();
        ing.form = form;
        ing.type = type;
        // Fail-fast: leading '#' is not valid in Minecraft Identifier (valid chars: [a-z0-9/._-]).
        // Identifier.parse will throw IllegalArgumentException; do NOT silently strip.
        ing.key = Identifier.parse(key);
        ing.amountOrCount = limit;
        ing.chance = chance;
        ing.rolls = rolls;
        switch (form) {
            case "item" -> {
                switch (type) {
                    case "tag" -> {
                        ing.ifTagItem = TagHelper.keyItem(key);
                        // Do NOT cache matchItems from TagHelper.getItems() here.
                        // Tags may not be populated at recipe creation/deserialization time
                        // (before PendingTags.apply()). Dynamic resolution happens in itemStacks().
                    }
                    case "static" -> ing.matchItems = List.of(parseItem(key));
                    case "item" -> ing.matchItems = List.of(parseItem(key));
                }
            }
            case "fluid" -> {
                switch (type) {
                    case "tag" -> {
                        ing.ifTagFluid = TagHelper.keyFluid(key);
                        // Do NOT cache matchFluids — dynamic resolution in fluidStacks().
                    }
                    case "static" -> ing.matchFluids = List.of(parseFluid(key));
                    case "item" -> ing.matchFluids = List.of(parseFluid(key));
                }
            }
        }
        return ing;
    }

    public void writeTo(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(form);
        buf.writeUtf(type);
        buf.writeIdentifier(key);
        buf.writeInt(amountOrCount);
        buf.writeDouble(chance);
        buf.writeInt(rolls);
    }

    public static FormsCombinedIngredient parseFrom(RegistryFriendlyByteBuf buf) {
        String form = buf.readUtf();
        String type = buf.readUtf();
        Identifier rl = buf.readIdentifier();
        int limit = buf.readInt();
        double chance = buf.readDouble();
        int rolls = buf.readInt();
        return create(limit, form, type, rl.toString(), chance, rolls);
    }

    // Output helpers
    public ItemStack genItem() {
        return genItem(Math::random);
    }

    /**
     * Generate an output item with a controllable random source.
     * Performs {@code rolls} independent Bernoulli trials, each with
     * probability {@code chance}. The total output count is the number
     * of successes multiplied by {@code amountOrCount}.
     * <p>
     * The result is capped at {@link Item#ABSOLUTE_MAX_STACK_SIZE} (99).
     * If the computed total exceeds this limit, an exception is thrown
     * (fast-fail) rather than silently capping — the recipe definition
     * must be fixed to avoid data loss.
     *
     * @param random a source of doubles in [0, 1) for each trial
     * @return the generated ItemStack (may be empty)
     */
    public ItemStack genItem(DoubleSupplier random) {
        if (rolls <= 1) {
            if (amountOrCount > Item.ABSOLUTE_MAX_STACK_SIZE) {
                throw new IllegalStateException(
                        "genItem: amountOrCount=" + amountOrCount + " exceeds ABSOLUTE_MAX_STACK_SIZE=" + Item.ABSOLUTE_MAX_STACK_SIZE + " for key=" + key);
            }
            return random.getAsDouble() < chance ? symbolItem() : ItemStack.EMPTY;
        }
        int successCount = 0;
        for (int i = 0; i < rolls; i++) {
            if (random.getAsDouble() < chance) successCount++;
        }
        if (successCount == 0) return ItemStack.EMPTY;
        int totalCount = Math.multiplyExact(successCount, amountOrCount);
        if (totalCount > Item.ABSOLUTE_MAX_STACK_SIZE) {
            throw new IllegalStateException(
                    "genItem: totalCount=" + totalCount + " (successCount=" + successCount + " × amountOrCount=" + amountOrCount + ") exceeds ABSOLUTE_MAX_STACK_SIZE=" + Item.ABSOLUTE_MAX_STACK_SIZE + " for key=" + key);
        }
        ItemStack result = symbolItem();
        result.setCount(totalCount);
        return result;
    }

    public ItemStack symbolItem() {
        var stacks = itemStacks();
        return stacks.isEmpty() || stacks.get(0).isEmpty() ? ItemStack.EMPTY : new ItemStack(stacks.get(0).getItem(), amountOrCount);
    }

    public FluidStack genFluid() {
        if (rolls > 1) {
            throw new IllegalStateException(
                    "genFluid: rolls must be 1 for fluid output, but got rolls=" + rolls + " for key=" + key);
        }
        return Math.random() < chance ? symbolFluid() : FluidStack.EMPTY;
    }

    public FluidStack symbolFluid() {
        var stacks = fluidStacks();
        return stacks.isEmpty() || stacks.get(0).isEmpty() ? FluidStack.EMPTY : new FluidStack(stacks.get(0).getFluid(), amountOrCount);
    }

    // ── Equality ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormsCombinedIngredient that)) return false;
        return amountOrCount == that.amountOrCount && Double.compare(chance, that.chance) == 0 && rolls == that.rolls && ALLOW_ALL == that.ALLOW_ALL && java.util.Objects.equals(form, that.form) && java.util.Objects.equals(type, that.type) && java.util.Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(form, type, key, amountOrCount, chance, rolls, ALLOW_ALL);
    }

    private static Item parseItem(String i) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(i)).orElse(Items.AIR);
    }

    private static Fluid parseFluid(String i) {
        return BuiltInRegistries.FLUID.getOptional(Identifier.parse(i)).orElse(Fluids.EMPTY);
    }

    @FunctionalInterface
    public interface IngredientTypeGetter {

        IngredientType get(int index);
    }
}
