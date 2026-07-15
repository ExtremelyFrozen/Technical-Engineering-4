package com.modularmc.ten.api.recipe;

import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.Holder;
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

public class FormsCombinedIngredient {

    boolean ALLOW_ALL;

    String type;
    String form;
    Collection<Item> matchItems = new ArrayList<>();
    TagKey<Item> ifTagItem;
    Collection<Fluid> matchFluids = new ArrayList<>();
    TagKey<Fluid> ifTagFluid;
    Identifier key;
    int amountOrCount;
    double chance;

    public int amountOrCount() {
        return amountOrCount;
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
        return create(limit, form, type, key, chance);
    }

    public static FormsCombinedIngredient create(int limit, String form, String type, String key, double chance) {
        var ing = new FormsCombinedIngredient();
        ing.form = form;
        ing.type = type;
        // Fail-fast: leading '#' is not valid in Minecraft Identifier (valid chars: [a-z0-9/._-]).
        // Identifier.parse will throw IllegalArgumentException; do NOT silently strip.
        ing.key = Identifier.parse(key);
        ing.amountOrCount = limit;
        ing.chance = chance;
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
    }

    public static FormsCombinedIngredient parseFrom(RegistryFriendlyByteBuf buf) {
        String form = buf.readUtf();
        String type = buf.readUtf();
        Identifier rl = buf.readIdentifier();
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
