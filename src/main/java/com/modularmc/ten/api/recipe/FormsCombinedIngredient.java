package com.modularmc.ten.api.recipe;

import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
    ResourceLocation key;
    int amountOrCount;
    double chance;
    int rolls = 1;

    public int amountOrCount() {
        return amountOrCount;
    }

    /**
     * 输出掷骰次数（1.21.1 配方无此字段时恒 1；与 26.1.2 无 rolls 配方一致）。
     */
    public int rolls() {
        return rolls;
    }

    public String type() {
        return type;
    }

    public String form() {
        return form;
    }

    public ResourceLocation key() {
        return key;
    }

    public double chance() {
        return chance;
    }

    private static final double CHANCE_CERTAIN_THRESHOLD = 1.0d - 1e-12;

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
        /** OUTPUT + chance &lt; 1.0 + rolls &gt; 1: translates to {@code kenergyengineering.jei_addition_chance_rolls}. */
        CHANCE_WITH_ROLLS,
        /** INPUT + chance &le; 0: translates to {@code kenergyengineering.not_consumed}. */
        NOT_CONSUMED
    }

    /**
     * Determines the {@link TooltipKind} for JEI/EMI tooltip registration.
     * Pure function, no JEI runtime dependencies.
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
        return matchItems.stream().map(i -> new ItemStack(i, amountOrCount)).toList();
    }

    public List<FluidStack> fluidStacks() {
        if (ALLOW_ALL) return List.of(FluidStack.EMPTY);
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
                    FluidStack stack = tanks.get(i).getFluidInTank(i);
                    if (stack.getAmount() < amountOrCount) continue;
                    if (contains(stack.getFluid())) yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    public Ingredient toOriginStackIngredients() {
        return "tag".equals(type) ? Ingredient.of(ifTagItem) : Ingredient.of(itemStacks().stream());
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
        return create(limit, form, type, key, chance);
    }

    public static FormsCombinedIngredient create(int limit, String form, String type, String key, double chance) {
        var ing = new FormsCombinedIngredient();
        ing.form = form;
        ing.type = type;
        ing.amountOrCount = limit;
        ing.key = ResourceLocation.parse(key);
        ing.chance = chance;
        switch (form) {
            case "item" -> {
                switch (type) {
                    case "tag" -> {
                        ing.ifTagItem = TagHelper.keyItem(key);
                        ing.matchItems = TagHelper.getItems(ing.ifTagItem);
                    }
                    case "static" -> ing.matchItems = List.of(parseItem(key));
                    case "item" -> ing.matchItems = List.of(parseItem(key));
                }
            }
            case "fluid" -> {
                switch (type) {
                    case "tag" -> {
                        ing.ifTagFluid = TagHelper.keyFluid(key);
                        ing.matchFluids = TagHelper.getFluids(ing.ifTagFluid);
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
        buf.writeResourceLocation(key);
        buf.writeInt(amountOrCount);
        buf.writeDouble(chance);
    }

    public static FormsCombinedIngredient parseFrom(RegistryFriendlyByteBuf buf) {
        String form = buf.readUtf();
        String type = buf.readUtf();
        ResourceLocation rl = buf.readResourceLocation();
        int limit = buf.readInt();
        double chance = buf.readDouble();
        return create(limit, form, type, rl.toString(), chance);
    }

    // Output helpers
    public ItemStack genItem() {
        return Math.random() < chance ? symbolItem() : ItemStack.EMPTY;
    }

    public ItemStack symbolItem() {
        var stacks = itemStacks();
        return stacks.isEmpty() || stacks.get(0).isEmpty() ? ItemStack.EMPTY : new ItemStack(stacks.get(0).getItem(), amountOrCount);
    }

    public FluidStack genFluid() {
        return Math.random() < chance ? symbolFluid() : FluidStack.EMPTY;
    }

    public FluidStack symbolFluid() {
        var stacks = fluidStacks();
        return stacks.isEmpty() || stacks.get(0).isEmpty() ? FluidStack.EMPTY : new FluidStack(stacks.get(0).getFluid(), amountOrCount);
    }

    private static Item parseItem(String i) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(i)).orElse(Items.AIR);
    }

    private static Fluid parseFluid(String i) {
        return BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(i)).orElse(Fluids.EMPTY);
    }

    @FunctionalInterface
    public interface IngredientTypeGetter {

        IngredientType get(int index);
    }
}
