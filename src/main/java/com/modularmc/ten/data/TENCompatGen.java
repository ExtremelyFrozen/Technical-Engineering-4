package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.RecipeSignature;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 邻居模组（Ender IO 系）的兼容配方：本模组的机器能吃对方的内容，但不由本模组注册任何物品。
 * <p>
 * 两张表都是机械导入的（EIO/EVO 的 {@code alloy_smelting} 与 {@code sag_milling} 数据 → 表行），
 * 邻居更新后重跑转换器即可对齐，不靠手抄。导入规则：
 * <ol>
 * <li>每条都带 {@code neoforge:mod_loaded} 条件，邻居缺席时整条在加载期消失；生成结果与本机
 *     是否安装邻居无关，CI 与本地产出一致。</li>
 * <li>时间按对方每次总能耗折算到本模组的 15 FE/t，钳位 20..1200 tick。</li>
 * <li>金属输入写 {@code c:ingots/X}，该材料存在 {@code c:mats/X} 时自动升级为「锭或粉皆可」。</li>
 * <li>对方的 compound 输入（如 soul_sand｜soul_soil）展开成多条——本模组格式没有 any_of 输入。</li>
 * <li>输入签名与本模组已有配方相同的条目一律不导入：同一台机器不允许两条可匹配配方，
 *     本模组的语义优先（矿石三段式已覆盖 {@code c:ores/*}，不再导入对方的 ore 研磨）。
 *     落盘前再由 {@link RecipeSignature#claim} 断言一次：两张表在同一次 DataGen 里互相登记，
 *     同一签名出现在两个文件直接让生成失败，不靠人记得去重。</li>
 * <li>输出必须是具体物品：标准语义里配方输出是一个确定的 ItemStack，标签输出按标准处理会得到空产物。
 *     对方数据里的标签输出在<b>编写期</b>按优先级列表（本模组 → EVO → EIO）解析成该标签的第一个
 *     可用成员再落地；解析不到（该标签谁都不提供，如 {@code c:dusts/aluminum}）就不导入这条。
 *     邻居增减后重跑转换器即可对齐。</li>
 * <li>输入用标签是允许的，且是跨模组适配的主要手段；标签为空时配方永不匹配，不报错也不丢件。</li>
 * </ol>
 */
public class TENCompatGen implements DataProvider {

    private static final String EIO = "enderio";
    private static final String EVO = "enderio_evolution";
    private static final String GRAINS = EIO + ":grains_of_infinity";
    private static final String DUST_BLOCK = EVO + ":infinity_dust_block";
    private static final String COMPRESSED = EVO + ":compressed_infinity_dust_block";
    private static final String DOUBLE_COMPRESSED = EVO + ":double_compressed_infinity_dust_block";

    /** 感应炉 ← EIO / EVO 合金炉。 */
    private static final Recipe[] ALLOYS = {
        alloy("enderio", "black_dye", "minecraft:black_dye", 1, 67, tag("c:dusts/coal"), item("minecraft:egg")),
        alloy("enderio", "black_dye_double", "minecraft:black_dye", 2, 107, tagN("c:dusts/coal", 2), tag("c:slimeballs")),
        alloy("enderio", "brown_dye_twigs", "minecraft:brown_dye", 1, 67, itemN("enderio:plant_matter_brown", 6), item("minecraft:egg")),
        alloy("enderio", "brown_dye_twigs_double", "minecraft:brown_dye", 2, 107, itemN("enderio:plant_matter_brown", 12), tag("c:slimeballs")),
        alloy("enderio", "clear_glass", "enderio:clear_glass", 1, 213, tag("c:glass_blocks/colorless")),
        alloy("enderio", "clear_glass_d_from_base", "enderio:clear_glass_d", 1, 107, item("enderio:clear_glass"), tagN("c:gems/amethyst", 4)),
        alloy("enderio", "clear_glass_d_from_base_alt", "enderio:clear_glass_d", 1, 107, item("enderio:clear_glass"), tag("c:storage_blocks/amethyst")),
        alloy("enderio", "clear_glass_d_from_main", "enderio:clear_glass_d", 1, 213, tag("c:glass_blocks/colorless"), tagN("c:gems/amethyst", 4)),
        alloy("enderio", "clear_glass_d_from_main_alt", "enderio:clear_glass_d", 1, 213, tag("c:glass_blocks/colorless"), tag("c:storage_blocks/amethyst")),
        alloy("enderio", "clear_glass_e_from_base", "enderio:clear_glass_e", 1, 107, item("enderio:clear_glass"), tagN("c:dusts/glowstone", 4)),
        alloy("enderio", "clear_glass_e_from_base_alt", "enderio:clear_glass_e", 1, 107, item("enderio:clear_glass"), item("minecraft:glowstone")),
        alloy("enderio", "clear_glass_e_from_main", "enderio:clear_glass_e", 1, 213, tag("c:glass_blocks/colorless"), tagN("c:dusts/glowstone", 4)),
        alloy("enderio", "clear_glass_e_from_main_alt", "enderio:clear_glass_e", 1, 213, tag("c:glass_blocks/colorless"), item("minecraft:glowstone")),
        alloy("enderio", "conductive_alloy_ingot", "enderio:conductive_alloy_ingot", 2, 213, ingot("iron"), ingot("copper")),
        alloy("enderio", "dark_steel_ingot", "enderio:dark_steel_ingot", 2, 213, ingot("iron"), tag("c:dusts/coal"), tag("c:obsidians")),
        alloy("enderio", "dead_bush", "minecraft:dead_bush", 1, 33, tag("minecraft:saplings")),
        alloy("enderio", "ender_pearl", "minecraft:ender_pearl", 1, 133, tagN("c:dusts/ender_pearl", 9)),
        alloy("enderio", "end_steel_ingot", "enderio:end_steel_ingot", 2, 427, tag("c:end_stones"), tag("c:ingots/dark_steel"), tag("c:obsidians")),
        alloy("enderio", "energetic_alloy_ingot", "enderio:energetic_alloy_ingot", 2, 320, tag("c:dusts/redstone"), tag("c:ingots/conductive_alloy"), ingot("gold")),
        alloy("enderio", "fused_quartz", "enderio:fused_quartz", 1, 427, tagN("c:gems/quartz", 4)),
        alloy("enderio", "fused_quartz_alt", "enderio:fused_quartz", 1, 427, tag("c:storage_blocks/quartz")),
        alloy("enderio", "fused_quartz_d_from_base", "enderio:fused_quartz_d", 1, 213, item("enderio:fused_quartz"), tagN("c:gems/amethyst", 4)),
        alloy("enderio", "fused_quartz_d_from_base_alt", "enderio:fused_quartz_d", 1, 213, item("enderio:fused_quartz"), tag("c:storage_blocks/amethyst")),
        alloy("enderio", "fused_quartz_d_from_main", "enderio:fused_quartz_d", 1, 427, tagN("c:gems/quartz", 4), tagN("c:gems/amethyst", 4)),
        alloy("enderio", "fused_quartz_d_from_main_alt", "enderio:fused_quartz_d", 1, 427, tagN("c:gems/quartz", 4), tag("c:storage_blocks/amethyst")),
        alloy("enderio", "fused_quartz_d_from_storage", "enderio:fused_quartz_d", 1, 427, tag("c:storage_blocks/quartz"), tagN("c:gems/amethyst", 4)),
        alloy("enderio", "fused_quartz_d_from_storage_alt", "enderio:fused_quartz_d", 1, 427, tag("c:storage_blocks/quartz"), tag("c:storage_blocks/amethyst")),
        alloy("enderio", "fused_quartz_e_from_base", "enderio:fused_quartz_e", 1, 213, item("enderio:fused_quartz"), tagN("c:dusts/glowstone", 4)),
        alloy("enderio", "fused_quartz_e_from_base_alt", "enderio:fused_quartz_e", 1, 213, item("enderio:fused_quartz"), item("minecraft:glowstone")),
        alloy("enderio", "fused_quartz_e_from_main", "enderio:fused_quartz_e", 1, 427, tagN("c:gems/quartz", 4), tagN("c:dusts/glowstone", 4)),
        alloy("enderio", "fused_quartz_e_from_main_alt", "enderio:fused_quartz_e", 1, 427, tagN("c:gems/quartz", 4), item("minecraft:glowstone")),
        alloy("enderio", "fused_quartz_e_from_storage", "enderio:fused_quartz_e", 1, 427, tag("c:storage_blocks/quartz"), tagN("c:dusts/glowstone", 4)),
        alloy("enderio", "fused_quartz_e_from_storage_alt", "enderio:fused_quartz_e", 1, 427, tag("c:storage_blocks/quartz"), item("minecraft:glowstone")),
        alloy("enderio", "green_dye_clippings", "minecraft:green_dye", 1, 67, itemN("enderio:plant_matter_green", 6), item("minecraft:egg")),
        alloy("enderio", "green_dye_double_clippings", "minecraft:green_dye", 2, 107, itemN("enderio:plant_matter_green", 12), tag("c:slimeballs")),
        alloy("enderio", "industrial_insulation", "enderio:industrial_insulation", 1, 213, tag("c:dusts/lapis"), tag("minecraft:wool"), tag("enderio:insulation_metals")),
        alloy("enderio", "photovoltaic_plate", "enderio:photovoltaic_plate", 1, 373, itemN("enderio:photovoltaic_composite", 2)),
        alloy("enderio", "pulsating_alloy_ingot", "enderio:pulsating_alloy_ingot", 2, 320, ingot("iron"), tag("c:ender_pearls")),
        alloy("enderio", "redstone_alloy_ingot", "enderio:redstone_alloy_ingot", 1, 213, tag("c:dusts/redstone"), ingot("copper")),
        alloy("enderio", "red_dye", "minecraft:red_dye", 12, 107, item("minecraft:beetroot"), itemN("minecraft:clay_ball", 3), itemN("minecraft:egg", 6)),
        alloy("enderio", "soularium_ingot_soul_sand", "enderio:soularium_ingot", 1, 373, item("minecraft:soul_sand"), ingot("gold")),
        alloy("enderio", "soularium_ingot_soul_soil", "enderio:soularium_ingot", 1, 373, item("minecraft:soul_soil"), ingot("gold")),
        alloy("enderio", "vibrant_alloy_ingot", "enderio:vibrant_alloy_ingot", 2, 373, tag("c:ingots/energetic_alloy"), tag("c:ender_pearls"), tag("c:dusts/glowstone")),
        alloy("enderio_evolution", "construction_alloy_ingot", "enderio_evolution:construction_alloy_ingot", 1, 667, item("minecraft:copper_ingot"), item("enderio_evolution:crude_steel_ingot"), item("minecraft:gravel")),
        alloy("enderio_evolution", "crude_steel_ingot", "enderio_evolution:crude_steel_ingot", 1, 667, item("minecraft:iron_ingot"), item("minecraft:clay_ball"), tag("c:cobblestones")),
        alloy("enderio_evolution", "crystalline_alloy_ingot", "enderio_evolution:crystalline_alloy_ingot", 2, 667, item("enderio:pulsating_powder"), item("enderio:pulsating_alloy_ingot"), item("enderio_evolution:vivid_alloy_ingot")),
        alloy("enderio_evolution", "crystalline_alloy_ingot_from_vibrant", "enderio_evolution:crystalline_alloy_ingot", 2, 667, item("enderio:pulsating_powder"), item("enderio:pulsating_alloy_ingot"), item("enderio:vibrant_alloy_ingot")),
        alloy("enderio_evolution", "crystalline_pink_slime_ingot", "enderio_evolution:crystalline_pink_slime_ingot", 1, 667, item("industrialforegoing:pink_slime"), item("enderio_evolution:crystalline_alloy_ingot")),
        alloy("enderio_evolution", "crystalline_pink_slime_ingot_synthetic", "enderio_evolution:crystalline_pink_slime_ingot", 1, 1000, itemN("minecraft:slime_ball", 2), item("enderio_evolution:crystalline_alloy_ingot"), tag("c:dyes/pink")),
        alloy("enderio_evolution", "energetic_silver_ingot_from_iron", "enderio_evolution:energetic_silver_ingot", 1, 667, item("minecraft:redstone"), item("minecraft:iron_ingot"), item("minecraft:glowstone_dust")),
        alloy("enderio_evolution", "energetic_silver_ingot_from_silver", "enderio_evolution:energetic_silver_ingot", 1, 667, item("minecraft:redstone"), tag("c:ingots/silver"), item("minecraft:glowstone_dust")),
        alloy("enderio_evolution", "melodic_alloy_ingot", "enderio_evolution:melodic_alloy_ingot", 2, 667, item("enderio_evolution:crystalline_pink_slime_ingot"), item("minecraft:popped_chorus_fruit"), item("enderio:end_steel_ingot")),
        alloy("enderio_evolution", "stellar_alloy_ingot", "enderio_evolution:stellar_alloy_ingot", 2, 1200, item("minecraft:nether_star"), item("enderio_evolution:melodic_alloy_ingot"), itemN("minecraft:clay_ball", 4)),
        alloy("enderio_evolution", "vivid_alloy_ingot", "enderio_evolution:vivid_alloy_ingot", 1, 667, item("enderio_evolution:energetic_silver_ingot"), item("minecraft:ender_pearl"))
    };

    /** 粉碎机 ← EIO / EVO SAG 研磨（已剔除与本模组输入撞车的条目）。 */
    private static final Recipe[] GRIND = {
        grind("enderio", "allium", item("minecraft:allium"), 160, out("minecraft:magenta_dye", 1, 0.8), out("minecraft:magenta_dye", 1, 0.6), out("minecraft:magenta_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio,immersiveengineering", "aluminum", tag("c:ingots/aluminum"), 160, out("immersiveengineering:dust_aluminum", 1)),
        grind("enderio", "azure_bluet", item("minecraft:azure_bluet"), 160, out("minecraft:light_gray_dye", 1, 0.8), out("minecraft:light_gray_dye", 1, 0.6), out("minecraft:light_gray_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "blaze_powder", tag("c:rods/blaze"), 160, out("minecraft:blaze_powder", 4)),
        grind("enderio", "blue_orchid", item("minecraft:blue_orchid"), 160, out("minecraft:light_blue_dye", 1, 0.8), out("minecraft:light_blue_dye", 1, 0.6), out("minecraft:light_blue_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "bone", tag("c:bones"), 160, out("minecraft:bone_meal", 6), out("minecraft:bone_meal", 2, 0.1)),
        grind("enderio", "bone_block", item("minecraft:bone_block"), 160, out("minecraft:bone_meal", 9)),
        grind("enderio", "cactus", item("minecraft:cactus"), 160, out("enderio:plant_matter_green", 3), out("enderio:plant_matter_green", 3, 0.75), out("enderio:plant_matter_green", 3, 0.5), out("enderio:plant_matter_brown", 3, 0.25)),
        grind("enderio", "clay", item("minecraft:clay"), 160, out("minecraft:clay_ball", 2), out("minecraft:clay_ball", 1, 0.1), out("enderio:silicon", 2, 0.8)),
        grind("enderio,mekanism", "coal", tag("minecraft:coals"), 160, out("enderio:powdered_coal", 1), out("enderio:powdered_coal", 1, 0.1), out("mekanism:dust_sulfur", 1, 0.1)),
        grind("enderio", "coal_ore", tag("c:ores/coal"), 160, out("minecraft:coal", 3), out("enderio:powdered_coal", 1, 0.6), out("minecraft:diamond", 1, 0.005), out("minecraft:cobblestone", 1, 0.15)),
        grind("enderio", "cobbled_deepslate", tag("c:cobblestones/deepslate"), 160, out("minecraft:cobblestone", 1), out("enderio:grains_of_infinity", 1, 0.12)),
        grind("enderio", "cobblestone", tag("c:cobblestones/normal"), 160, out("minecraft:gravel", 1, 0.7), out("minecraft:gravel", 1, 0.3), out("minecraft:sand", 1, 0.1), out("minecraft:flint", 1, 0.05)),
        grind("enderio", "cobweb", item("minecraft:cobweb"), 160, out("minecraft:string", 2), out("minecraft:string", 1, 0.6), out("minecraft:string", 1, 0.3)),
        grind("enderio", "dandelion", item("minecraft:dandelion"), 160, out("minecraft:yellow_dye", 1, 0.8), out("minecraft:yellow_dye", 1, 0.6), out("minecraft:yellow_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "deepslate", item("minecraft:deepslate"), 160, out("minecraft:cobbled_deepslate", 1)),
        grind("enderio", "diamond_ore", tag("c:ores/diamond"), 160, out("minecraft:diamond", 2), out("minecraft:diamond", 1, 0.25), out("enderio:powdered_coal", 1, 0.1), out("minecraft:cobblestone", 1, 0.15)),
        grind("enderio", "emerald_ore", tag("c:ores/emerald"), 160, out("minecraft:emerald", 2), out("minecraft:emerald", 1, 0.25), out("minecraft:cobblestone", 1, 0.15)),
        grind("enderio", "ender_crystal", tag("c:gems/ender_crystal"), 160, out("enderio:ender_crystal_powder", 1)),
        grind("enderio", "ender_pearl", tag("c:ender_pearls"), 160, out("enderio:powdered_ender_pearl", 9)),
        grind("enderio", "fern", item("minecraft:fern"), 160, out("enderio:plant_matter_green", 1, 0.6), out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1), out("enderio:plant_matter_brown", 1, 0.05)),
        grind("enderio", "flower_pot", item("minecraft:flower_pot"), 160, out("minecraft:brick", 1, 0.9), out("minecraft:brick", 1, 0.3), out("minecraft:brick", 1, 0.1), out("enderio:powdered_coal", 1, 0.05)),
        grind("enderio", "glass", tag("c:glass_blocks"), 160, out("minecraft:sand", 1)),
        grind("enderio", "lapis_ore", tag("c:ores/lapis"), 160, out("minecraft:lapis_lazuli", 8), out("minecraft:lapis_lazuli", 1, 0.2), out("minecraft:cobblestone", 1, 0.15)),
        grind("enderio", "large_fern", item("minecraft:large_fern"), 160, out("enderio:plant_matter_green", 2, 0.6), out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 2, 0.1), out("enderio:plant_matter_brown", 1, 0.15)),
        grind("enderio,mekanism", "lead", tag("c:ingots/lead"), 160, out("mekanism:dust_lead", 1)),
        grind("enderio", "leaves", tag("minecraft:leaves"), 160, out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1), out("enderio:plant_matter_green", 1, 0.02)),
        grind("enderio", "lily_pad", item("minecraft:lily_pad"), 160, out("enderio:plant_matter_green", 3, 0.15), out("enderio:plant_matter_green", 3, 0.1)),
        grind("enderio", "mossy_cobblestone", tag("c:cobblestones/mossy"), 160, out("minecraft:cobblestone", 1), out("minecraft:vine", 1, 0.7)),
        grind("enderio", "obsidian", tag("c:obsidians"), 160, out("enderio:powdered_obsidian", 4)),
        grind("enderio", "orange_tulip", item("minecraft:orange_tulip"), 160, out("minecraft:orange_dye", 1, 0.8), out("minecraft:orange_dye", 1, 0.6), out("minecraft:orange_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio,mekanism", "osmium", tag("c:ingots/osmium"), 160, out("mekanism:dust_osmium", 1)),
        grind("enderio", "oxeye_daisy", item("minecraft:oxeye_daisy"), 160, out("minecraft:white_dye", 1, 0.8), out("minecraft:white_dye", 1, 0.6), out("minecraft:white_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "pink_tulip", item("minecraft:pink_tulip"), 160, out("minecraft:pink_dye", 1, 0.8), out("minecraft:pink_dye", 1, 0.6), out("minecraft:pink_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "poeny", item("minecraft:peony"), 160, out("minecraft:pink_dye", 2, 0.8), out("minecraft:pink_dye", 1, 0.6), out("minecraft:pink_dye", 2, 0.3), out("enderio:plant_matter_green", 1, 0.2)),
        grind("enderio", "poppy", item("minecraft:poppy"), 160, out("minecraft:red_dye", 1, 0.8), out("minecraft:red_dye", 1, 0.6), out("minecraft:red_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "precient_crystal", tag("c:gems/prescient_crystal"), 160, out("enderio:prescient_powder", 1)),
        grind("enderio", "prismarine_shard", item("minecraft:prismarine_shard"), 160, out("minecraft:prismarine_crystals", 1), out("minecraft:prismarine_crystals", 1, 0.1)),
        grind("enderio", "pulsating_crystal", tag("c:gems/pulsating_crystal"), 160, out("enderio:pulsating_powder", 1)),
        grind("enderio", "quartz_block", tag("c:storage_blocks/quartz"), 160, out("minecraft:quartz", 2), out("minecraft:quartz", 2, 0.25)),
        grind("enderio", "quartz_ore", tag("c:ores/quartz"), 160, out("minecraft:quartz", 2), out("enderio:powdered_quartz", 1, 0.1), out("minecraft:netherrack", 1, 0.15)),
        grind("enderio", "quartz_slabs", item("minecraft:quartz_slab"), 160, out("minecraft:quartz", 1), out("minecraft:quartz", 1, 0.25)),
        grind("enderio", "quartz_stairs", item("minecraft:quartz_stairs"), 160, out("minecraft:quartz", 2), out("minecraft:quartz", 2, 0.25)),
        grind("enderio", "redstone_ore", tag("c:ores/redstone"), 160, out("minecraft:redstone", 8), out("minecraft:redstone", 1, 0.2), out("enderio:silicon", 1, 0.8), out("minecraft:cobblestone", 1, 0.15)),
        grind("enderio", "red_tulip", item("minecraft:red_tulip"), 160, out("minecraft:red_dye", 1, 0.8), out("minecraft:red_dye", 1, 0.6), out("minecraft:red_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "rose_bush", item("minecraft:rose_bush"), 160, out("minecraft:red_dye", 2, 0.8), out("minecraft:red_dye", 1, 0.6), out("minecraft:red_dye", 2, 0.3), out("enderio:plant_matter_green", 1, 0.2)),
        grind("enderio", "sandstone", tag("c:sandstone/blocks"), 160, out("minecraft:sand", 2), out("minecraft:sand", 2, 0.4)),
        grind("enderio", "shrub", item("minecraft:dead_bush"), 160, out("enderio:plant_matter_brown", 1, 0.8), out("enderio:plant_matter_brown", 1, 0.6), out("enderio:plant_matter_brown", 1, 0.3)),
        grind("enderio", "soularium", tag("c:ingots/soularium"), 160, out("enderio:soul_powder", 1)),
        grind("enderio", "sugar_canes", item("minecraft:sugar_cane"), 160, out("minecraft:sugar", 1), out("minecraft:sugar", 1, 0.5), out("minecraft:sugar", 2, 0.5), out("enderio:plant_matter_green", 1, 0.2)),
        grind("enderio", "sun_flower", item("minecraft:sunflower"), 160, out("minecraft:yellow_dye", 2, 0.8), out("minecraft:yellow_dye", 1, 0.6), out("minecraft:yellow_dye", 2, 0.3), out("enderio:plant_matter_green", 1, 0.2)),
        grind("enderio", "tall_grass", item("minecraft:tall_grass"), 160, out("enderio:plant_matter_green", 2, 0.6), out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 2, 0.1), out("enderio:plant_matter_brown", 1, 0.15)),
        grind("enderio,mekanism", "uranium", tag("c:ingots/uranium"), 160, out("mekanism:dust_uranium", 1)),
        grind("enderio", "vibrant_crystal", tag("c:gems/vibrant_crystal"), 160, out("enderio:vibrant_powder", 1)),
        grind("enderio", "vines", item("minecraft:vine"), 160, out("enderio:plant_matter_green", 1, 0.15), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "white_tulip", item("minecraft:white_tulip"), 160, out("minecraft:white_dye", 1, 0.8), out("minecraft:white_dye", 1, 0.6), out("minecraft:white_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
        grind("enderio", "wither_rose", item("minecraft:wither_rose"), 160, out("enderio:withering_powder", 1, 0.4)),
        grind("enderio", "wither_skull", item("minecraft:wither_skeleton_skull"), 160, out("enderio:withering_powder", 2), out("enderio:withering_powder", 1, 0.2)),
        grind("enderio", "wool", tag("minecraft:wool"), 160, out("minecraft:string", 2), out("minecraft:string", 1, 0.1)),
        grind("enderio_evolution,enderio", "compressed_infinity_dust_block", item("enderio_evolution:compressed_infinity_dust_block"), 160, out("enderio_evolution:infinity_dust_block", 9), out("enderio_evolution:compressed_infinity_dust_block", 1, 0.05), out("enderio:grains_of_infinity", 1, 0.1)),
        grind("enderio_evolution,enderio", "double_compressed_infinity_dust_block", item("enderio_evolution:double_compressed_infinity_dust_block"), 160, out("enderio_evolution:compressed_infinity_dust_block", 9), out("enderio_evolution:double_compressed_infinity_dust_block", 1, 0.05), out("enderio:grains_of_infinity", 1, 0.1)),
        grind("enderio_evolution,enderio", "infinity_dust_block", item("enderio_evolution:infinity_dust_block"), 160, out("enderio:grains_of_infinity", 9), out("enderio_evolution:infinity_dust_block", 1, 0.05), out("enderio:grains_of_infinity", 1, 0.1))
    };

    /** 压缩机承接 EVO 的 infinity 粉尘压缩（对方只有工作台形状合成，这是增量）。
     *  还原方向不写：EVO 自己的 sag_milling 已导入到 GRIND 表，重复会撞同一输入。 */
    private static final Recipe[] INFINITY = {
        press(EVO, "grains_to_dust_block", GRAINS, 9, DUST_BLOCK),
        press(EVO, "dust_block_to_compressed", DUST_BLOCK, 9, COMPRESSED),
        press(EVO, "compressed_to_double", COMPRESSED, 9, DOUBLE_COMPRESSED)
    };

    private final PackOutput output;

    public TENCompatGen(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path base = output.getOutputFolder().resolve("data/" + TEN.MOD_ID + "/recipe/compat");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var r : ALLOYS) emit(futures, cache, base, r);
        for (var r : GRIND) emit(futures, cache, base, r);
        for (var r : INFINITY) emit(futures, cache, base, r);
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ══════════════════════════════════════════════════════════════════
    // 输入 / 输出描述
    // ══════════════════════════════════════════════════════════════════

    private record Input(String key, Integer count, boolean isTag) {}

    private record Out(String key, int count, Double chance) {}

    /** 标签输入（4 个工厂按「是否标签 × 是否带数量」区分，不靠命名空间前缀猜）。 */
    private static Input tag(String key) {
        return new Input(key, null, true);
    }

    private static Input tagN(String key, int n) {
        return new Input(key, n, true);
    }

    private static Input item(String key) {
        return new Input(key, null, false);
    }

    private static Input itemN(String key, int n) {
        return new Input(key, n, false);
    }

    /** 金属输入：该材料有 {@code c:mats/X} 聚合标签时自动升级成锭或粉皆可。 */
    private static Input ingot(String material) {
        return new Input(TENMaterialTagGen.hasMatTag(material) ? "c:mats/" + material : "c:ingots/" + material, null, true);
    }

    private static Out out(String key, int count) {
        return new Out(key, count, null);
    }

    private static Out out(String key, int count, double chance) {
        return new Out(key, count, chance);
    }

    // ══════════════════════════════════════════════════════════════════
    // 配方构造
    // ══════════════════════════════════════════════════════════════════

    protected record Recipe(String dir, String name, JsonObject json) {}

    /** 感应炉合金：1–3 输入 → 1 输出。 */
    private static Recipe alloy(String mod, String name, String outputId, int outputCount, int time, Input... inputs) {
        var ins = new JsonArray();
        for (var in : inputs) ins.add(ingr(in.key(), in.count(), in.isTag(), null));
        var outs = new JsonArray();
        outs.add(ingr(outputId, outputCount > 1 ? outputCount : null, false, null));
        return machine(mod, "induction_furnace", name, ins, outs, time);
    }

    /** 粉碎机研磨：1 输入 → ≤4 输出（带 chance）。 */
    private static Recipe grind(String mod, String name, Input input, int time, Out... outputs) {
        var ins = new JsonArray();
        ins.add(ingr(input.key(), input.count(), input.isTag(), null));
        var outs = new JsonArray();
        for (var o : outputs) outs.add(ingr(o.key(), o.count() > 1 ? o.count() : null, false, o.chance()));
        return machine(mod, "pulverizer", name, ins, outs, time);
    }

    /** 压缩机压缩：N 个输入 + 大压缩模具（chance 0 = 催化位，不消耗、不随批量倍增）→ 1 个块。 */
    private static Recipe press(String mod, String name, String inputId, int inputCount, String outputId) {
        var ins = new JsonArray();
        ins.add(ingr(inputId, inputCount, false, null));
        ins.add(ingr(TEN.MOD_ID + ":mould_compressed_large", null, false, 0.0));
        var outs = new JsonArray();
        outs.add(ingr(outputId, null, false, null));
        return machine(mod, "compressor", name, ins, outs, 100);
    }

    private static Recipe machine(String mod, String type, String name, JsonArray inputs, JsonArray outputs, int time) {
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":" + type);
        var conds = modLoadedAll(mod);
        // 每源开关：关掉该来源即整条配方在加载期消失（JEI 同步不显示）
        var toggle = new JsonObject();
        toggle.addProperty("type", TEN.MOD_ID + ":compat_enabled");
        toggle.addProperty("source", mod.split(",")[0]);
        conds.add(toggle);
        j.add("neoforge:conditions", conds);
        j.add("inputs", inputs);
        j.add("outputs", outputs);
        j.addProperty("time", time);
        // 目录带机器类型：EIO 的 alloy_smelting/ender_pearl 与 sag_milling/ender_pearl 同名，
        // 平铺会互相覆盖
        return new Recipe(mod.split(",")[0] + "/" + type, name, j);
    }

    /** 逗号分隔的多重门控：任一邻居缺席，整条配方在加载期消失。 */
    private static JsonArray modLoadedAll(String mods) {
        var a = new JsonArray();
        for (String m : mods.split(",")) {
            var o = new JsonObject();
            o.addProperty("type", "neoforge:mod_loaded");
            o.addProperty("modid", m.trim());
            a.add(o);
        }
        return a;
    }

    private static JsonObject modLoaded(String modId) {
        var o = new JsonObject();
        o.addProperty("type", "neoforge:mod_loaded");
        o.addProperty("modid", modId);
        return o;
    }

    private static JsonObject ingr(String key, Integer count, boolean isTag, Double chance) {
        var o = new JsonObject();
        o.addProperty("form", "item");
        o.addProperty("type", isTag ? "tag" : "static");
        o.addProperty("key", key);
        if (count != null) o.addProperty("count", count);
        if (chance != null) o.addProperty("chance", chance);
        return o;
    }

    private void emit(List<CompletableFuture<?>> futures, CachedOutput cache, Path base, Recipe r) {
        Path dir = base.resolve(r.dir());
        dir.toFile().mkdirs();
        Path file = dir.resolve(r.name() + ".json");
        RecipeSignature.claim(r.json(), file.toString());
        futures.add(DataProvider.saveStable(cache, r.json(), file));
    }

    @Override
    public String getName() {
        return "Neighbour Mod Compat Recipes";
    }
}
