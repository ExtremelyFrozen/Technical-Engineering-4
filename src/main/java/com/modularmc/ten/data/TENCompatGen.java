package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.RecipeSignature;
import com.modularmc.ten.config.TENConfig;

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
 * 邻居更新后重跑转换器即可对齐，不靠手抄。AE2 家族的水晶行是手写的（{@link #GRIND_MANUAL}），转换器碰不到。
 * 导入规则：
 * <ol>
 * <li>每条都带 {@code neoforge:mod_loaded} 条件，邻居缺席时整条在加载期消失；生成结果与本机
 * 是否安装邻居无关，CI 与本地产出一致。</li>
 * <li>耗时不按对方能量换算，而是统一到本模组的尺规（{@code tools/compat/config.json} 的 timeRule）：
 * 感应炉 50 tick × 输入物料份数、粉碎机一次操作 120（粗矿块这类 9 份输入 200）、
 * 压缩机恒 100 不看份数。原来的「对方总 FE ÷ 15 FE/t」在 IE 的 51200 FE 前会顶到 1200 tick 上限，
 * 与原生配方差一个数量级。</li>
 * <li>金属输入写 {@code c:ingots/X}；只有感应炉走 {@link #matIngot} 才升级成 {@code c:mats/X}（锭或粉皆可入），
 * 粉碎机与压缩机不收粉——{@code c:mats/X} 只服务感应炉。</li>
 * <li>对方的 compound 输入（如 soul_sand｜soul_soil）展开成多条——本模组格式没有 any_of 输入。</li>
 * <li>输入签名与本模组已有配方相同的条目一律不导入：同一台机器不允许两条可匹配配方，
 * 本模组的语义优先（矿石三段式已覆盖 {@code c:ores/*}，不再导入对方的 ore 研磨）。
 * 落盘前再由 {@link RecipeSignature#claim} 断言一次：两张表在同一次 DataGen 里互相登记，
 * 同一签名出现在两个文件直接让生成失败，不靠人记得去重。</li>
 * <li>输出必须是具体物品：标准语义里配方输出是一个确定的 ItemStack，标签输出按标准处理会得到空产物。
 * 对方数据里的标签输出在<b>编写期</b>按优先级列表（本模组 → EVO → EIO）解析成该标签的第一个
 * 可用成员再落地；解析不到（该标签谁都不提供，如 {@code c:dusts/aluminum}）就不导入这条。
 * 邻居增减后重跑转换器即可对齐。</li>
 * <li>输入用标签是允许的，且是跨模组适配的主要手段；每条还会按输入标签补 {@code not(tag_empty)}
 * 门控，材料缺席时整条在加载期消失——与邻居自己的写法一致，JEI 不会留下跑不起来的配方。</li>
 * <li>对方配方自带的第三方 {@code neoforge:mod_loaded} 门控（例如 EVO 只在装了
 * Industrial Foregoing 时才有的合金行）会一并带过来，作为本行的额外门控；漏掉它等于
 * 造出一条只在别的模组包里成立的配方。例外：对方另有<b>不带该第三方门控</b>的等价路线时按等价路线放宽——
 * {@code neoecoae} 的两条水晶行就是如此，对方的 {@code inscriber} 路线不需要通用机械。</li>
 * <li>同一输入有多家邻居提供、产物或副产不同时走 {@link #GRIND_SPLIT}：条件成对互斥，加载期只有一条成立。
 * 素材全是原版的条目不再进兼容层——转正为 {@code recipe/pulverizer/*.json} 的原生配方，
 * 并在 {@code tools/compat/config.json} 的 {@code droppedRows} 登记，重跑转换器不会塞回来。</li>
 * </ol>
 */
public class TENCompatGen implements DataProvider {

    private static final String EIO = "enderio";
    private static final String EVO = "enderio_evolution";
    private static final String IE = "immersiveengineering";
    private static final String GRAINS = EIO + ":grains_of_infinity";
    private static final String DUST_BLOCK = EVO + ":infinity_dust_block";
    private static final String COMPRESSED = EVO + ":compressed_infinity_dust_block";
    private static final String DOUBLE_COMPRESSED = EVO + ":double_compressed_infinity_dust_block";

    /** 感应炉 ← EIO / EVO 合金炉。 */
    private static final Recipe[] ALLOYS = {
            alloy("enderio", "black_dye", "minecraft:black_dye", 1, 100, tag("c:dusts/coal"), item("minecraft:egg")),
            alloy("enderio", "black_dye_double", "minecraft:black_dye", 2, 150, tagN("c:dusts/coal", 2), tag("c:slimeballs")),
            alloy("enderio", "brown_dye_twigs", "minecraft:brown_dye", 1, 350, itemN("enderio:plant_matter_brown", 6), item("minecraft:egg")),
            alloy("enderio", "brown_dye_twigs_double", "minecraft:brown_dye", 2, 650, itemN("enderio:plant_matter_brown", 12), tag("c:slimeballs")),
            alloy("enderio", "clear_glass", "enderio:clear_glass", 1, 50, tag("c:glass_blocks/colorless")),
            alloy("enderio", "clear_glass_d_from_base", "enderio:clear_glass_d", 1, 250, item("enderio:clear_glass"), tagN("c:gems/amethyst", 4)),
            alloy("enderio", "clear_glass_d_from_base_alt", "enderio:clear_glass_d", 1, 100, item("enderio:clear_glass"), tag("c:storage_blocks/amethyst")),
            alloy("enderio", "clear_glass_d_from_main", "enderio:clear_glass_d", 1, 250, tag("c:glass_blocks/colorless"), tagN("c:gems/amethyst", 4)),
            alloy("enderio", "clear_glass_d_from_main_alt", "enderio:clear_glass_d", 1, 100, tag("c:glass_blocks/colorless"), tag("c:storage_blocks/amethyst")),
            alloy("enderio", "clear_glass_e_from_base", "enderio:clear_glass_e", 1, 250, item("enderio:clear_glass"), tagN("c:dusts/glowstone", 4)),
            alloy("enderio", "clear_glass_e_from_base_alt", "enderio:clear_glass_e", 1, 100, item("enderio:clear_glass"), item("minecraft:glowstone")),
            alloy("enderio", "clear_glass_e_from_main", "enderio:clear_glass_e", 1, 250, tag("c:glass_blocks/colorless"), tagN("c:dusts/glowstone", 4)),
            alloy("enderio", "clear_glass_e_from_main_alt", "enderio:clear_glass_e", 1, 100, tag("c:glass_blocks/colorless"), item("minecraft:glowstone")),
            alloy("enderio", "conductive_alloy_ingot", "enderio:conductive_alloy_ingot", 2, 100, matIngot("iron"), matIngot("copper")),
            alloy("enderio", "dark_steel_ingot", "enderio:dark_steel_ingot", 2, 150, matIngot("iron"), tag("c:dusts/coal"), tag("c:obsidians")),
            alloy("enderio", "dead_bush", "minecraft:dead_bush", 1, 50, tag("minecraft:saplings")),
            alloy("enderio", "ender_pearl", "minecraft:ender_pearl", 1, 450, tagN("c:dusts/ender_pearl", 9)),
            alloy("enderio", "end_steel_ingot", "enderio:end_steel_ingot", 2, 150, tag("c:end_stones"), tag("c:ingots/dark_steel"), tag("c:obsidians")),
            alloy("enderio", "energetic_alloy_ingot", "enderio:energetic_alloy_ingot", 2, 150, tag("c:dusts/redstone"), tag("c:ingots/conductive_alloy"), matIngot("gold")),
            alloy("enderio", "fused_quartz", "enderio:fused_quartz", 1, 200, tagN("c:gems/quartz", 4)),
            alloy("enderio", "fused_quartz_alt", "enderio:fused_quartz", 1, 50, tag("c:storage_blocks/quartz")),
            alloy("enderio", "fused_quartz_d_from_base", "enderio:fused_quartz_d", 1, 250, item("enderio:fused_quartz"), tagN("c:gems/amethyst", 4)),
            alloy("enderio", "fused_quartz_d_from_base_alt", "enderio:fused_quartz_d", 1, 100, item("enderio:fused_quartz"), tag("c:storage_blocks/amethyst")),
            alloy("enderio", "fused_quartz_d_from_main", "enderio:fused_quartz_d", 1, 400, tagN("c:gems/quartz", 4), tagN("c:gems/amethyst", 4)),
            alloy("enderio", "fused_quartz_d_from_main_alt", "enderio:fused_quartz_d", 1, 250, tagN("c:gems/quartz", 4), tag("c:storage_blocks/amethyst")),
            alloy("enderio", "fused_quartz_d_from_storage", "enderio:fused_quartz_d", 1, 250, tag("c:storage_blocks/quartz"), tagN("c:gems/amethyst", 4)),
            alloy("enderio", "fused_quartz_d_from_storage_alt", "enderio:fused_quartz_d", 1, 100, tag("c:storage_blocks/quartz"), tag("c:storage_blocks/amethyst")),
            alloy("enderio", "fused_quartz_e_from_base", "enderio:fused_quartz_e", 1, 250, item("enderio:fused_quartz"), tagN("c:dusts/glowstone", 4)),
            alloy("enderio", "fused_quartz_e_from_base_alt", "enderio:fused_quartz_e", 1, 100, item("enderio:fused_quartz"), item("minecraft:glowstone")),
            alloy("enderio", "fused_quartz_e_from_main", "enderio:fused_quartz_e", 1, 400, tagN("c:gems/quartz", 4), tagN("c:dusts/glowstone", 4)),
            alloy("enderio", "fused_quartz_e_from_main_alt", "enderio:fused_quartz_e", 1, 250, tagN("c:gems/quartz", 4), item("minecraft:glowstone")),
            alloy("enderio", "fused_quartz_e_from_storage", "enderio:fused_quartz_e", 1, 250, tag("c:storage_blocks/quartz"), tagN("c:dusts/glowstone", 4)),
            alloy("enderio", "fused_quartz_e_from_storage_alt", "enderio:fused_quartz_e", 1, 100, tag("c:storage_blocks/quartz"), item("minecraft:glowstone")),
            alloy("enderio", "green_dye_clippings", "minecraft:green_dye", 1, 350, itemN("enderio:plant_matter_green", 6), item("minecraft:egg")),
            alloy("enderio", "green_dye_double_clippings", "minecraft:green_dye", 2, 650, itemN("enderio:plant_matter_green", 12), tag("c:slimeballs")),
            alloy("enderio", "industrial_insulation", "enderio:industrial_insulation", 1, 150, tag("c:dusts/lapis"), tag("minecraft:wool"), tag("enderio:insulation_metals")),
            alloy("enderio", "photovoltaic_plate", "enderio:photovoltaic_plate", 1, 100, itemN("enderio:photovoltaic_composite", 2)),
            alloy("enderio", "pulsating_alloy_ingot", "enderio:pulsating_alloy_ingot", 2, 100, matIngot("iron"), tag("c:ender_pearls")),
            alloy("enderio", "redstone_alloy_ingot", "enderio:redstone_alloy_ingot", 1, 100, tag("c:dusts/redstone"), matIngot("copper")),
            alloy("enderio", "red_dye", "minecraft:red_dye", 12, 500, item("minecraft:beetroot"), itemN("minecraft:clay_ball", 3), itemN("minecraft:egg", 6)),
            alloy("enderio", "soularium_ingot_soul_sand", "enderio:soularium_ingot", 1, 100, item("minecraft:soul_sand"), matIngot("gold")),
            alloy("enderio", "soularium_ingot_soul_soil", "enderio:soularium_ingot", 1, 100, item("minecraft:soul_soil"), matIngot("gold")),
            alloy("enderio", "vibrant_alloy_ingot", "enderio:vibrant_alloy_ingot", 2, 150, tag("c:ingots/energetic_alloy"), tag("c:ender_pearls"), tag("c:dusts/glowstone")),
            alloy("enderio_evolution", "construction_alloy_ingot", "enderio_evolution:construction_alloy_ingot", 1, 150, item("minecraft:copper_ingot"), item("enderio_evolution:crude_steel_ingot"), item("minecraft:gravel")),
            alloy("enderio_evolution", "crude_steel_ingot", "enderio_evolution:crude_steel_ingot", 1, 150, item("minecraft:iron_ingot"), item("minecraft:clay_ball"), tag("c:cobblestones")),
            alloy("enderio_evolution", "crystalline_alloy_ingot", "enderio_evolution:crystalline_alloy_ingot", 2, 150, item("enderio:pulsating_powder"), item("enderio:pulsating_alloy_ingot"), item("enderio_evolution:vivid_alloy_ingot")),
            alloy("enderio_evolution", "crystalline_alloy_ingot_from_vibrant", "enderio_evolution:crystalline_alloy_ingot", 2, 150, item("enderio:pulsating_powder"), item("enderio:pulsating_alloy_ingot"), item("enderio:vibrant_alloy_ingot")),
            alloy("enderio_evolution,industrialforegoing", "crystalline_pink_slime_ingot", "enderio_evolution:crystalline_pink_slime_ingot", 1, 100, item("industrialforegoing:pink_slime"), item("enderio_evolution:crystalline_alloy_ingot")),
            alloy("enderio_evolution", "crystalline_pink_slime_ingot_synthetic", "enderio_evolution:crystalline_pink_slime_ingot", 1, 200, itemN("minecraft:slime_ball", 2), item("enderio_evolution:crystalline_alloy_ingot"), tag("c:dyes/pink")),
            alloy("enderio_evolution", "energetic_silver_ingot_from_iron", "enderio_evolution:energetic_silver_ingot", 1, 150, item("minecraft:redstone"), item("minecraft:iron_ingot"), item("minecraft:glowstone_dust")),
            alloy("enderio_evolution", "energetic_silver_ingot_from_silver", "enderio_evolution:energetic_silver_ingot", 1, 150, item("minecraft:redstone"), matIngot("silver"), item("minecraft:glowstone_dust")),
            alloy("enderio_evolution", "melodic_alloy_ingot", "enderio_evolution:melodic_alloy_ingot", 2, 150, item("enderio_evolution:crystalline_pink_slime_ingot"), item("minecraft:popped_chorus_fruit"), item("enderio:end_steel_ingot")),
            alloy("enderio_evolution", "stellar_alloy_ingot", "enderio_evolution:stellar_alloy_ingot", 2, 300, item("minecraft:nether_star"), item("enderio_evolution:melodic_alloy_ingot"), itemN("minecraft:clay_ball", 4)),
            alloy("enderio_evolution", "vivid_alloy_ingot", "enderio_evolution:vivid_alloy_ingot", 1, 100, item("enderio_evolution:energetic_silver_ingot"), item("minecraft:ender_pearl")),
            alloy("immersiveengineering,alltheores", "brass", "alltheores:brass_ingot", 2, 100, matIngot("copper"), matIngot("zinc")),
            alloy("immersiveengineering", "insulating_glass", "immersiveengineering:insulating_glass", 2, 150, tagN("c:glass_blocks", 2), tag("c:dusts/iron"))
    };

    /** 粉碎机 ← EIO / EVO SAG 研磨（已剔除与本模组输入撞车的条目）。 */
    private static final Recipe[] GRIND = {
            grind("enderio", "allium", item("minecraft:allium"), 120, out("minecraft:magenta_dye", 1, 0.8), out("minecraft:magenta_dye", 1, 0.6), out("minecraft:magenta_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "azure_bluet", item("minecraft:azure_bluet"), 120, out("minecraft:light_gray_dye", 1, 0.8), out("minecraft:light_gray_dye", 1, 0.6), out("minecraft:light_gray_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "blue_orchid", item("minecraft:blue_orchid"), 120, out("minecraft:light_blue_dye", 1, 0.8), out("minecraft:light_blue_dye", 1, 0.6), out("minecraft:light_blue_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "cactus", item("minecraft:cactus"), 120, out("enderio:plant_matter_green", 3), out("enderio:plant_matter_green", 3, 0.75), out("enderio:plant_matter_green", 3, 0.5), out("enderio:plant_matter_brown", 3, 0.25)),
            grind("enderio", "clay", item("minecraft:clay"), 120, out("minecraft:clay_ball", 2), out("minecraft:clay_ball", 1, 0.1), out("enderio:silicon", 2, 0.8)),
            grind("enderio", "coal_ore", tag("c:ores/coal"), 120, out("minecraft:coal", 3), out("enderio:powdered_coal", 1, 0.6), out("minecraft:diamond", 1, 0.005), out("minecraft:cobblestone", 1, 0.15)),
            grind("enderio", "cobbled_deepslate", tag("c:cobblestones/deepslate"), 120, out("minecraft:cobblestone", 1), out("enderio:grains_of_infinity", 1, 0.12)),
            grind("enderio", "cobblestone", tag("c:cobblestones/normal"), 120, out("minecraft:gravel", 1, 0.7), out("minecraft:gravel", 1, 0.3), out("minecraft:sand", 1, 0.1), out("minecraft:flint", 1, 0.05)),
            grind("enderio", "dandelion", item("minecraft:dandelion"), 120, out("minecraft:yellow_dye", 1, 0.8), out("minecraft:yellow_dye", 1, 0.6), out("minecraft:yellow_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "diamond_ore", tag("c:ores/diamond"), 120, out("minecraft:diamond", 2), out("minecraft:diamond", 1, 0.25), out("enderio:powdered_coal", 1, 0.1), out("minecraft:cobblestone", 1, 0.15)),
            grind("enderio", "ender_crystal", tag("c:gems/ender_crystal"), 120, out("enderio:ender_crystal_powder", 1)),
            grind("enderio", "ender_pearl", tag("c:ender_pearls"), 120, out("enderio:powdered_ender_pearl", 9)),
            grind("enderio", "fern", item("minecraft:fern"), 120, out("enderio:plant_matter_green", 1, 0.6), out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1), out("enderio:plant_matter_brown", 1, 0.05)),
            grind("enderio", "flower_pot", item("minecraft:flower_pot"), 120, out("minecraft:brick", 1, 0.9), out("minecraft:brick", 1, 0.3), out("minecraft:brick", 1, 0.1), out("enderio:powdered_coal", 1, 0.05)),
            grind("enderio", "grass", item("minecraft:tall_grass"), 120, out("enderio:plant_matter_green", 1, 0.6), out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1), out("enderio:plant_matter_brown", 1, 0.05)),
            grind("enderio", "large_fern", item("minecraft:large_fern"), 120, out("enderio:plant_matter_green", 2, 0.6), out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 2, 0.1), out("enderio:plant_matter_brown", 1, 0.15)),
            grind("enderio", "leaves", tag("minecraft:leaves"), 120, out("enderio:plant_matter_green", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1), out("enderio:plant_matter_green", 1, 0.02)),
            grind("enderio", "lily_pad", item("minecraft:lily_pad"), 120, out("enderio:plant_matter_green", 3, 0.15), out("enderio:plant_matter_green", 3, 0.1)),
            grind("enderio", "mossy_cobblestone", tag("c:cobblestones/mossy"), 120, out("minecraft:cobblestone", 1), out("minecraft:vine", 1, 0.7)),
            grind("enderio", "obsidian", tag("c:obsidians"), 120, out("enderio:powdered_obsidian", 4)),
            grind("enderio", "orange_tulip", item("minecraft:orange_tulip"), 120, out("minecraft:orange_dye", 1, 0.8), out("minecraft:orange_dye", 1, 0.6), out("minecraft:orange_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "oxeye_daisy", item("minecraft:oxeye_daisy"), 120, out("minecraft:white_dye", 1, 0.8), out("minecraft:white_dye", 1, 0.6), out("minecraft:white_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "pink_tulip", item("minecraft:pink_tulip"), 120, out("minecraft:pink_dye", 1, 0.8), out("minecraft:pink_dye", 1, 0.6), out("minecraft:pink_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "poeny", item("minecraft:peony"), 120, out("minecraft:pink_dye", 2, 0.8), out("minecraft:pink_dye", 1, 0.6), out("minecraft:pink_dye", 2, 0.3), out("enderio:plant_matter_green", 1, 0.2)),
            grind("enderio", "poppy", item("minecraft:poppy"), 120, out("minecraft:red_dye", 1, 0.8), out("minecraft:red_dye", 1, 0.6), out("minecraft:red_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "precient_crystal", tag("c:gems/prescient_crystal"), 120, out("enderio:prescient_powder", 1)),
            grind("enderio", "pulsating_crystal", tag("c:gems/pulsating_crystal"), 120, out("enderio:pulsating_powder", 1)),
            grind("enderio", "quartz_ore", tag("c:ores/quartz"), 120, out("minecraft:quartz", 2), out("enderio:powdered_quartz", 1, 0.1), out("minecraft:netherrack", 1, 0.15)),
            grind("enderio", "redstone_ore", tag("c:ores/redstone"), 120, out("minecraft:redstone", 8), out("minecraft:redstone", 1, 0.2), out("enderio:silicon", 1, 0.8), out("minecraft:cobblestone", 1, 0.15)),
            grind("enderio", "red_tulip", item("minecraft:red_tulip"), 120, out("minecraft:red_dye", 1, 0.8), out("minecraft:red_dye", 1, 0.6), out("minecraft:red_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "rose_bush", item("minecraft:rose_bush"), 120, out("minecraft:red_dye", 2, 0.8), out("minecraft:red_dye", 1, 0.6), out("minecraft:red_dye", 2, 0.3), out("enderio:plant_matter_green", 1, 0.2)),
            grind("enderio", "shrub", item("minecraft:dead_bush"), 120, out("enderio:plant_matter_brown", 1, 0.8), out("enderio:plant_matter_brown", 1, 0.6), out("enderio:plant_matter_brown", 1, 0.3)),
            grind("enderio", "soularium", tag("c:ingots/soularium"), 120, out("enderio:soul_powder", 1)),
            grind("enderio", "sugar_canes", item("minecraft:sugar_cane"), 120, out("minecraft:sugar", 1), out("minecraft:sugar", 1, 0.5), out("minecraft:sugar", 2, 0.5), out("enderio:plant_matter_green", 1, 0.2)),
            grind("enderio", "sun_flower", item("minecraft:sunflower"), 120, out("minecraft:yellow_dye", 2, 0.8), out("minecraft:yellow_dye", 1, 0.6), out("minecraft:yellow_dye", 2, 0.3), out("enderio:plant_matter_green", 1, 0.2)),
            grind("enderio", "vibrant_crystal", tag("c:gems/vibrant_crystal"), 120, out("enderio:vibrant_powder", 1)),
            grind("enderio", "vines", item("minecraft:vine"), 120, out("enderio:plant_matter_green", 1, 0.15), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "white_tulip", item("minecraft:white_tulip"), 120, out("minecraft:white_dye", 1, 0.8), out("minecraft:white_dye", 1, 0.6), out("minecraft:white_dye", 1, 0.3), out("enderio:plant_matter_green", 1, 0.1)),
            grind("enderio", "wither_rose", item("minecraft:wither_rose"), 120, out("enderio:withering_powder", 1, 0.4)),
            grind("enderio", "wither_skull", item("minecraft:wither_skeleton_skull"), 120, out("enderio:withering_powder", 2), out("enderio:withering_powder", 1, 0.2)),
            grind("enderio_evolution,enderio", "compressed_infinity_dust_block", item("enderio_evolution:compressed_infinity_dust_block"), 120, out("enderio_evolution:infinity_dust_block", 9), out("enderio_evolution:compressed_infinity_dust_block", 1, 0.05), out("enderio:grains_of_infinity", 1, 0.1)),
            grind("enderio_evolution,enderio", "double_compressed_infinity_dust_block", item("enderio_evolution:double_compressed_infinity_dust_block"), 120, out("enderio_evolution:compressed_infinity_dust_block", 9), out("enderio_evolution:double_compressed_infinity_dust_block", 1, 0.05), out("enderio:grains_of_infinity", 1, 0.1)),
            grind("enderio_evolution,enderio", "infinity_dust_block", item("enderio_evolution:infinity_dust_block"), 120, out("enderio:grains_of_infinity", 9), out("enderio_evolution:infinity_dust_block", 1, 0.05), out("enderio:grains_of_infinity", 1, 0.1)),
            grind("alltheores,enderio", "brass_ingot", tag("c:ingots/brass"), 120, out("alltheores:brass_dust", 1)),
            grind("alltheores,enderio", "bronze_ingot", tag("c:ingots/bronze"), 120, out("alltheores:bronze_dust", 1)),
            grind("alltheores,enderio", "cinnabar_gem", tag("c:gems/cinnabar"), 120, out("alltheores:cinnabar_dust", 1)),
            grind("alltheores,enderio", "cinnabar_ore", tag("c:ores/cinnabar"), 120, out("alltheores:cinnabar", 6), out("minecraft:cobblestone", 1, 0.15)),
            grind("alltheores,enderio", "constantan_ingot", tag("c:ingots/constantan"), 120, out("alltheores:constantan_dust", 1)),
            grind("alltheores,enderio", "electrum_ingot", tag("c:ingots/electrum"), 120, out("alltheores:electrum_dust", 1)),
            grind("alltheores,enderio", "enderium_ingot", tag("c:ingots/enderium"), 120, out("alltheores:enderium_dust", 1)),
            grind("alltheores,enderio", "fluorite_gem", tag("c:gems/fluorite"), 120, out("alltheores:fluorite_dust", 1)),
            grind("alltheores,enderio", "fluorite_ore", tag("c:ores/fluorite"), 120, out("alltheores:fluorite", 6), out("minecraft:cobblestone", 1, 0.15)),
            grind("alltheores,enderio", "invar_ingot", tag("c:ingots/invar"), 120, out("alltheores:invar_dust", 1)),
            grind("alltheores,enderio", "lumium_ingot", tag("c:ingots/lumium"), 120, out("alltheores:lumium_dust", 1)),
            grind("alltheores,enderio", "peridot_gem", tag("c:gems/peridot"), 120, out("alltheores:peridot_dust", 1)),
            grind("alltheores,enderio", "peridot_ore", tag("c:ores/peridot"), 120, out("alltheores:peridot", 6), out("minecraft:cobblestone", 1, 0.15)),
            grind("alltheores,enderio", "ruby_gem", tag("c:gems/ruby"), 120, out("alltheores:ruby_dust", 1)),
            grind("alltheores,enderio", "ruby_ore", tag("c:ores/ruby"), 120, out("alltheores:ruby", 6), out("minecraft:cobblestone", 1, 0.15)),
            grind("alltheores,enderio", "salt_ore", tag("c:ores/salt"), 120, out("alltheores:salt", 6)),
            grind("alltheores,enderio", "sapphire_gem", tag("c:gems/sapphire"), 120, out("alltheores:sapphire_dust", 1)),
            grind("alltheores,enderio", "sapphire_ore", tag("c:ores/sapphire"), 120, out("alltheores:sapphire", 6), out("minecraft:cobblestone", 1, 0.15)),
            grind("alltheores,enderio", "signalum_ingot", tag("c:ingots/signalum"), 120, out("alltheores:signalum_dust", 1)),
            grind("alltheores,enderio", "steel_ingot", tag("c:ingots/steel"), 120, out("alltheores:steel_dust", 1)),
            grind("alltheores,enderio", "sulfur_ore", tag("c:ores/sulfur"), 120, out("alltheores:sulfur", 6)),
            grind("neoecoae,mekanism", "aluminum_alloy_dust", tag("c:ingots/aluminum_alloy"), 120, out("neoecoae:aluminum_alloy_dust", 1)),
            grind("neoecoae,mekanism", "black_tungsten_alloy_dust", tag("c:ingots/black_tungsten_alloy"), 120, out("neoecoae:black_tungsten_alloy_dust", 1)),
            grind("immersiveengineering,enderio", "coal_block", item("minecraft:coal_block"), 120, out("enderio:powdered_coal", 9)),
            grind("immersiveengineering", "coke_block", tag("c:storage_blocks/coal_coke"), 120, out("immersiveengineering:dust_coke", 9)),
            grind("immersiveengineering", "graphite_ingot", tag("c:ingots/hop_graphite"), 120, out("immersiveengineering:dust_hop_graphite", 1)),
            grind("immersiveengineering", "graphite_plate", tag("c:plates/hop_graphite"), 120, out("immersiveengineering:dust_hop_graphite", 1)),
            grind("immersiveengineering", "slag", tag("c:slag"), 120, out("immersiveengineering:slag_gravel", 1)),
            grind("immersiveengineering", "slag_brick", item("immersiveengineering:slag_brick"), 120, out("immersiveengineering:slag_gravel", 1))
    };

    /**
     * 粉碎机 ← AE2 家族水晶。手写表：{@code inject.js} 只替换 ALLOYS/GRIND/PRESS 的数组体，动不到这里。
     * <p>
     * 一条行 = 一个 {@code c:gems/<水晶>} 输入标签，输出恒为同一模组自己的粉：所以 {@code mod_loaded} 按
     * <b>输出所属命名空间</b>门控就能保证产物存在，输入侧由 {@code not(tag_empty)} 在标签无人填充时整条消失。
     * 对方走 {@code ae2:inscriber} 这类外部机器类型，导入等于替邻居重述配方，因此这条线不进转换器。
     * {@code c:gems/charged_certus_quartz}（data_energistics 的别名标签）与 {@code c:gems/resonance_crystal} 不写：
     * 前者成员已被 certus 行覆盖，后者没有对应的粉产物。
     * 新增行要同时把文件名登记进 {@code tools/compat/config.json} 的 {@code manualRows}，
     * 否则下次跑转换器会把同输入的机械导入塞回来，撞 {@link RecipeSignature#claim} 让生成失败。
     */
    private static final Recipe[] GRIND_MANUAL = {
            grind("ae2", "certus_quartz_crystal", tag("c:gems/certus_quartz"), 120, out("ae2:certus_quartz_dust", 1)),
            grind("ae2", "fluix_crystal", tag("c:gems/fluix"), 120, out("ae2:fluix_dust", 1)),
            grind("ae2", "sky_stone_crystal", tag("c:gems/sky_stone_crystal"), 120, out("ae2:sky_dust", 1)),
            grind("ae2", "ender_quartz", tag("c:gems/ender_quartz"), 120, out("ae2:ender_dust", 1)),
            grind("ae2cs", "resonating_crystal", tag("c:gems/resonating"), 120, out("ae2cs:resonating_crystal_dust", 1)),
            grind("data_energistics", "data_crystal", tag("c:gems/data_crystal"), 120, out("data_energistics:data_dust", 1)),
            grind("extendedae", "entro_crystal", tag("c:gems/entro"), 120, out("extendedae:entro_dust", 1)),
            grind("extendedae_plus", "lattra_crystal", tag("c:gems/lattra"), 120, out("extendedae_plus:lattra_dust", 1)),
            grind("neoecoae", "energized_crystal_dust", tag("c:gems/energized_crystal"), 120, out("neoecoae:energized_crystal_dust", 1)),
            grind("neoecoae", "energized_flux_crystal_dust", tag("c:gems/energized_fluix_crystal"), 120, out("neoecoae:energized_fluix_crystal_dust", 1))
    };

    /**
     * 同一输入有多家邻居提供、产物或副产不同时的分流表：条件互斥，加载期只有一条成立。
     * <p>
     * 与 {@link #GRIND} 的区别是条件可以组合：{@code mod_loaded(X)} 与 {@code not(mod_loaded(X))}
     * 成对出现，表达「装了 A 就走 A 的产物，否则退到 B」。{@code GRIND} 的数组体由转换器整体覆盖，
     * 这张表手写、直接 emit，不参与 {@code inject.js}；行名都带邻居后缀，不会与导入行同名。
     * <p>
     * 三个分流点：
     * <ul>
     * <li>煤：EIO 煤粉优先（Mek 在时带 10% 硫粉副产），没有 EIO 才退到 Mek 煤粉；</li>
     * <li>木炭、焦炭：IE 在时按 IE 语义（黑染料 / 焦炭粉），否则按 EIO/Mek 磨成煤粉。
     * 三种碳燃料因此各自只命中一条，原来是 {@code minecraft:coals} 一条全吃、木炭会被磨成煤粉；</li>
     * <li>砂岩：IE 在时用 IE 表（带硝石），只有 EIO 时用 EIO 表（双倍概率），红砂岩两版都出红沙——
     * EIO 原数据把红砂岩也算进 {@code c:sandstone/blocks}、产物是普通沙，这里纠正。</li>
     * </ul>
     */
    private static final Recipe[] GRIND_SPLIT = {
            grindSplit("enderio", "coal_enderio_mek", List.of(modQ(EIO), modQ("mekanism")),
                    item("minecraft:coal"), 120,
                    out("enderio:powdered_coal", 1), out("enderio:powdered_coal", 1, 0.1), out("mekanism:dust_sulfur", 1, 0.1)),
            grindSplit("enderio", "coal_enderio", List.of(modQ(EIO), notQ(active("mekanism"))),
                    item("minecraft:coal"), 120,
                    out("enderio:powdered_coal", 1), out("enderio:powdered_coal", 1, 0.1)),
            grindSplit("mekanism", "coal_mekanism", List.of(notQ(active(EIO)), modQ("mekanism")),
                    item("minecraft:coal"), 120, out("mekanism:dust_coal", 1)),

            grindSplit("immersiveengineering", "charcoal_black_dye", List.of(modQ("immersiveengineering")),
                    item("minecraft:charcoal"), 120, out("minecraft:black_dye", 1), out("minecraft:gray_dye", 1, 0.2)),
            grindSplit("enderio", "charcoal_enderio", List.of(notQ(active(IE)), modQ(EIO)),
                    item("minecraft:charcoal"), 120, out("enderio:powdered_coal", 1)),
            grindSplit("mekanism", "charcoal_mekanism",
                    List.of(notQ(active(IE)), notQ(active(EIO)), modQ("mekanism")),
                    item("minecraft:charcoal"), 120, out("mekanism:dust_coal", 1)),

            grindSplit("immersiveengineering", "coal_coke", List.of(modQ(IE)),
                    tag("c:coal_coke"), 120, out("immersiveengineering:dust_coke", 1)),
            grindSplit("enderio", "coal_coke_enderio", List.of(notQ(active(IE)), modQ(EIO)),
                    tag("c:coal_coke"), 120, out("enderio:powdered_coal", 1)),

            grindSplit("enderio", "sandstone_enderio", List.of(modQ(EIO), notQ(active(IE))),
                    tag("c:sandstone/uncolored_blocks"), 120, out("minecraft:sand", 2), out("minecraft:sand", 2, 0.4)),
            grindSplit("enderio", "red_sandstone_enderio", List.of(modQ(EIO), notQ(active(IE))),
                    tag("c:sandstone/red_blocks"), 120, out("minecraft:red_sand", 2), out("minecraft:red_sand", 2, 0.4)),
            grindSplit("immersiveengineering", "sandstone_immersiveengineering", List.of(modQ(IE)),
                    tag("c:sandstone/uncolored_blocks"), 120,
                    out("minecraft:sand", 2), out("immersiveengineering:dust_saltpeter", 1, 0.5)),
            grindSplit("immersiveengineering", "red_sandstone_immersiveengineering", List.of(modQ(IE)),
                    tag("c:sandstone/red_blocks"), 120,
                    out("minecraft:red_sand", 2), out("immersiveengineering:dust_saltpeter", 1, 0.5))
    };

    /**
     * 压缩机承接 EVO 的 infinity 粉尘压缩（对方只有工作台形状合成，这是增量）。
     * 还原方向不写：EVO 自己的 sag_milling 已导入到 GRIND 表，重复会撞同一输入。
     */
    private static final Recipe[] INFINITY = {
            press(EVO, "grains_to_dust_block", GRAINS, 9, DUST_BLOCK),
            press(EVO, "dust_block_to_compressed", DUST_BLOCK, 9, COMPRESSED),
            press(EVO, "compressed_to_double", COMPRESSED, 9, DOUBLE_COMPRESSED)
    };

    /**
     * 压缩机承接邻居冲压机的板/齿轮/杆/线（模具换成本模组的对应模具，chance 0 = 催化位）。
     * 行内容由 tools/compat/convert.js 从对方数据机械导入，inject.js 只替换本数组体。
     */
    private static final Recipe[] PRESS = {
            pressed("alltheores,immersiveengineering", "aluminum_gear", tagN("c:ingots/aluminum", 4), "kenergyengineering:mould_gear", "alltheores:aluminum_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "aluminum_plate", tag("c:ingots/aluminum"), "kenergyengineering:mould_plate", "alltheores:aluminum_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "aluminum_rod", tag("c:ingots/aluminum"), "kenergyengineering:mould_rod", "alltheores:aluminum_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "brass_gear", tagN("c:ingots/brass", 4), "kenergyengineering:mould_gear", "alltheores:brass_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "brass_plate", tag("c:ingots/brass"), "kenergyengineering:mould_plate", "alltheores:brass_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "brass_rod", tag("c:ingots/brass"), "kenergyengineering:mould_rod", "alltheores:brass_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "bronze_gear", tagN("c:ingots/bronze", 4), "kenergyengineering:mould_gear", "alltheores:bronze_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "bronze_plate", tag("c:ingots/bronze"), "kenergyengineering:mould_plate", "alltheores:bronze_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "bronze_rod", tag("c:ingots/bronze"), "kenergyengineering:mould_rod", "alltheores:bronze_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "constantan_gear", tagN("c:ingots/constantan", 4), "kenergyengineering:mould_gear", "alltheores:constantan_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "constantan_plate", tag("c:ingots/constantan"), "kenergyengineering:mould_plate", "alltheores:constantan_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "constantan_rod", tag("c:ingots/constantan"), "kenergyengineering:mould_rod", "alltheores:constantan_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "diamond_rod", tag("c:gems/diamond"), "kenergyengineering:mould_rod", "alltheores:diamond_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "electrum_gear", tagN("c:ingots/electrum", 4), "kenergyengineering:mould_gear", "alltheores:electrum_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "electrum_plate", tag("c:ingots/electrum"), "kenergyengineering:mould_plate", "alltheores:electrum_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "electrum_rod", tag("c:ingots/electrum"), "kenergyengineering:mould_rod", "alltheores:electrum_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "enderium_gear", tagN("c:ingots/enderium", 4), "kenergyengineering:mould_gear", "alltheores:enderium_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "enderium_plate", tag("c:ingots/enderium"), "kenergyengineering:mould_plate", "alltheores:enderium_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "enderium_rod", tag("c:ingots/enderium"), "kenergyengineering:mould_rod", "alltheores:enderium_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "invar_gear", tagN("c:ingots/invar", 4), "kenergyengineering:mould_gear", "alltheores:invar_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "invar_plate", tag("c:ingots/invar"), "kenergyengineering:mould_plate", "alltheores:invar_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "invar_rod", tag("c:ingots/invar"), "kenergyengineering:mould_rod", "alltheores:invar_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "iridium_gear", tagN("c:ingots/iridium", 4), "kenergyengineering:mould_gear", "alltheores:iridium_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "iridium_plate", tag("c:ingots/iridium"), "kenergyengineering:mould_plate", "alltheores:iridium_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "iridium_rod", tag("c:ingots/iridium"), "kenergyengineering:mould_rod", "alltheores:iridium_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "lead_gear", tagN("c:ingots/lead", 4), "kenergyengineering:mould_gear", "alltheores:lead_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "lead_plate", tag("c:ingots/lead"), "kenergyengineering:mould_plate", "alltheores:lead_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "lead_rod", tag("c:ingots/lead"), "kenergyengineering:mould_rod", "alltheores:lead_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "lumium_gear", tagN("c:ingots/lumium", 4), "kenergyengineering:mould_gear", "alltheores:lumium_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "lumium_plate", tag("c:ingots/lumium"), "kenergyengineering:mould_plate", "alltheores:lumium_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "lumium_rod", tag("c:ingots/lumium"), "kenergyengineering:mould_rod", "alltheores:lumium_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "osmium_gear", tagN("c:ingots/osmium", 4), "kenergyengineering:mould_gear", "alltheores:osmium_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "osmium_plate", tag("c:ingots/osmium"), "kenergyengineering:mould_plate", "alltheores:osmium_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "osmium_rod", tag("c:ingots/osmium"), "kenergyengineering:mould_rod", "alltheores:osmium_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "platinum_gear", tagN("c:ingots/platinum", 4), "kenergyengineering:mould_gear", "alltheores:platinum_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "platinum_plate", tag("c:ingots/platinum"), "kenergyengineering:mould_plate", "alltheores:platinum_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "platinum_rod", tag("c:ingots/platinum"), "kenergyengineering:mould_rod", "alltheores:platinum_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "signalum_gear", tagN("c:ingots/signalum", 4), "kenergyengineering:mould_gear", "alltheores:signalum_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "signalum_plate", tag("c:ingots/signalum"), "kenergyengineering:mould_plate", "alltheores:signalum_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "signalum_rod", tag("c:ingots/signalum"), "kenergyengineering:mould_rod", "alltheores:signalum_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "silver_gear", tagN("c:ingots/silver", 4), "kenergyengineering:mould_gear", "alltheores:silver_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "silver_plate", tag("c:ingots/silver"), "kenergyengineering:mould_plate", "alltheores:silver_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "silver_rod", tag("c:ingots/silver"), "kenergyengineering:mould_rod", "alltheores:silver_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "steel_gear", tagN("c:ingots/steel", 4), "kenergyengineering:mould_gear", "alltheores:steel_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "steel_plate", tag("c:ingots/steel"), "kenergyengineering:mould_plate", "alltheores:steel_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "steel_rod", tag("c:ingots/steel"), "kenergyengineering:mould_rod", "alltheores:steel_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "uranium_gear", tagN("c:ingots/uranium", 4), "kenergyengineering:mould_gear", "alltheores:uranium_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "uranium_plate", tag("c:ingots/uranium"), "kenergyengineering:mould_plate", "alltheores:uranium_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "uranium_rod", tag("c:ingots/uranium"), "kenergyengineering:mould_rod", "alltheores:uranium_rod", 2, 100),
            pressed("alltheores,immersiveengineering", "zinc_gear", tagN("c:ingots/zinc", 4), "kenergyengineering:mould_gear", "alltheores:zinc_gear", 1, 100),
            pressed("alltheores,immersiveengineering", "zinc_plate", tag("c:ingots/zinc"), "kenergyengineering:mould_plate", "alltheores:zinc_plate", 1, 100),
            pressed("alltheores,immersiveengineering", "zinc_rod", tag("c:ingots/zinc"), "kenergyengineering:mould_rod", "alltheores:zinc_rod", 2, 100),
            pressed("immersiveengineering", "blaze_rod", itemN("minecraft:blaze_powder", 5), "kenergyengineering:mould_rod", "minecraft:blaze_rod", 1, 100),
            pressed("immersiveengineering", "wire_aluminum", tag("c:ingots/aluminum"), "kenergyengineering:mould_string", "immersiveengineering:wire_aluminum", 2, 100),
            pressed("immersiveengineering", "wire_electrum", tag("c:ingots/electrum"), "kenergyengineering:mould_string", "immersiveengineering:wire_electrum", 2, 100),
            pressed("immersiveengineering", "wire_lead", tag("c:ingots/lead"), "kenergyengineering:mould_string", "immersiveengineering:wire_lead", 2, 100),
            pressed("immersiveengineering", "wire_steel", tag("c:ingots/steel"), "kenergyengineering:mould_string", "immersiveengineering:wire_steel", 2, 100)
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
        for (var r : GRIND_SPLIT) emit(futures, cache, base, r);
        for (var r : GRIND_MANUAL) emit(futures, cache, base, r);
        for (var r : INFINITY) emit(futures, cache, base, r);
        for (var r : PRESS) emit(futures, cache, base, r);
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

    /** 金属输入：只认锭。粉碎机与压缩机的工序是把锭磨/压成形，粉进来会得到不对等的产出。 */
    private static Input ingot(String material) {
        return new Input("c:ingots/" + material, null, true);
    }

    /** 感应炉专用：该材料有 {@code c:mats/X} 聚合标签时锭或粉皆可入（合金只看元素种类）。 */
    private static Input matIngot(String material) {
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

    /** 压缩机压缩：N 个输入 + 大压缩模具（chance 0 = 催化位，不消耗、不随并行运行数倍增）→ 1 个块。 */
    private static Recipe press(String mod, String name, String inputId, int inputCount, String outputId) {
        var ins = new JsonArray();
        ins.add(ingr(inputId, inputCount, false, null));
        ins.add(ingr(TEN.MOD_ID + ":mould_compressed_large", null, false, 0.0));
        var outs = new JsonArray();
        outs.add(ingr(outputId, null, false, null));
        return machine(mod, "compressor", name, ins, outs, 100);
    }

    /**
     * 冲压机承接：材料（锭）+ 本模组的对应模具（催化位）→ 板/齿轮/杆/线。
     * 模具与 {@code press} 同理用 {@code chance 0}，不消耗、不随并行运行数倍增。
     */
    private static Recipe pressed(String mod, String name, Input input, String mould, String outputId, int outputCount, int time) {
        var ins = new JsonArray();
        ins.add(ingr(input.key(), input.count(), input.isTag(), null));
        ins.add(ingr(mould, null, false, 0.0));
        var outs = new JsonArray();
        outs.add(ingr(outputId, outputCount > 1 ? outputCount : null, false, null));
        return machine(mod, "compressor", name, ins, outs, time);
    }

    /** 分流行：条件由表行自己给（互斥的 {@code mod_loaded} / {@code not(mod_loaded)} 组合），source 只决定目录与来源开关。 */
    private static Recipe grindSplit(String source, String name, List<JsonObject> conds, Input input, int time, Out... outputs) {
        var ins = new JsonArray();
        ins.add(ingr(input.key(), input.count(), input.isTag(), null));
        var outs = new JsonArray();
        for (var o : outputs) outs.add(ingr(o.key(), o.count() > 1 ? o.count() : null, false, o.chance()));
        return machineWith(source, "pulverizer", name, conds, ins, outs, time);
    }

    /** {@code neoforge:mod_loaded}，供分流表组合使用（可被 {@link #notQ} 取反）。 */
    private static JsonObject modQ(String modId) {
        return modLoaded(modId);
    }

    /** {@code neoforge:not} 包装。 */
    private static JsonObject notQ(JsonObject cond) {
        var o = new JsonObject();
        o.addProperty("type", "neoforge:not");
        o.add("value", cond);
        return o;
    }

    /**
     * 「该来源真的生效」：模组在场且 {@code compat.disabledSources} 没关它。
     * 分流表的兜底分支要否定这个整体，否则玩家关掉某来源的兼容配方后，兜底分支仍被
     * {@code mod_loaded} 挡住，输入会一条配方都不剩。
     */
    private static JsonObject active(String source) {
        var and = new JsonObject();
        and.addProperty("type", "neoforge:and");
        var values = new JsonArray();
        values.add(modLoaded(source));
        var toggle = new JsonObject();
        toggle.addProperty("type", TEN.MOD_ID + ":compat_enabled");
        toggle.addProperty("source", source);
        values.add(toggle);
        and.add("values", values);
        return and;
    }

    /**
     * 表行写的来源必须登记在 {@link TENConfig.CompatConfig#KNOWN_SOURCES}。
     * 拼错不会报错，只会生成一条 {@code compat_enabled} 永远匹配不上的配方——玩家装再多邻居也看不到它。
     * <p>
     * source 支持 {@code "a,b"} 形式（一条配方同时依赖多个邻居），逐段检查。
     */
    private static void checkSource(String source) {
        for (String s : source.split(",")) {
            if (!TENConfig.CompatConfig.KNOWN_SOURCES.contains(s)) {
                throw new IllegalStateException("兼容来源未登记到 TENConfig.CompatConfig.KNOWN_SOURCES：" + s);
            }
        }
    }

    /** 与 {@link #machine} 同构，但条件由调用方给全（不再从 mod 串推导），用于分流表。 */
    private static Recipe machineWith(String source, String type, String name, List<JsonObject> conds,
                                      JsonArray inputs, JsonArray outputs, int time) {
        checkSource(source);
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":" + type);
        var all = new JsonArray();
        for (var c : conds) all.add(c);
        for (var e : inputs) {
            var in = e.getAsJsonObject();
            if (!"tag".equals(in.get("type").getAsString())) continue;
            var absent = new JsonObject();
            absent.addProperty("type", "neoforge:tag_empty");
            absent.addProperty("tag", in.get("key").getAsString());
            var guard = new JsonObject();
            guard.addProperty("type", "neoforge:not");
            guard.add("value", absent);
            all.add(guard);
        }
        // 每源开关：主来源总是加；其余来源只在配方真的用到它的物品时才加。
        // 复合 source 也用于目录归属（pressed 的 "alltheores,immersiveengineering" 输入是通用标签、
        // 输出归 ATO），把 IE 一并算进来会在关掉 IE 兼容时误伤 ATO 的配方。
        addSourceToggles(all, source, inputs, outputs);
        j.add("neoforge:conditions", all);
        j.add("inputs", inputs);
        j.add("outputs", outputs);
        j.addProperty("time", time);
        return new Recipe(source + "/" + type, name, j);
    }

    private static Recipe machine(String mod, String type, String name, JsonArray inputs, JsonArray outputs, int time) {
        checkSource(mod);
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":" + type);
        var conds = modLoadedAll(mod);
        // 输入标签缺席 → 整条配方在加载期消失。邻居自己就是这么写的（EIO 的 sag_milling 有 15 条
        // not(tag_empty) 门控），少了它，JEI 会显示一条原料根本不存在、永远跑不起来的配方。
        for (var e : inputs) {
            var in = e.getAsJsonObject();
            if (!"tag".equals(in.get("type").getAsString())) continue;
            var absent = new JsonObject();
            absent.addProperty("type", "neoforge:tag_empty");
            absent.addProperty("tag", in.get("key").getAsString());
            var guard = new JsonObject();
            guard.addProperty("type", "neoforge:not");
            guard.add("value", absent);
            conds.add(guard);
        }
        // 每源开关：主来源总是加；其余来源只在配方真的用到它的物品时才加。
        // 复合 source 也用于目录归属（pressed 的 "alltheores,immersiveengineering" 输入是通用标签、
        // 输出归 ATO），把 IE 一并算进来会在关掉 IE 兼容时误伤 ATO 的配方。
        addSourceToggles(conds, mod, inputs, outputs);
        j.add("neoforge:conditions", conds);
        j.add("inputs", inputs);
        j.add("outputs", outputs);
        j.addProperty("time", time);
        // 目录带机器类型：EIO 的 alloy_smelting/ender_pearl 与 sag_milling/ender_pearl 同名，
        // 平铺会互相覆盖
        return new Recipe(mod.split(",")[0] + "/" + type, name, j);
    }

    /**
     * 给配方补 {@code compat_enabled} 条件：主来源（第一段）总是加，其余来源只在输入或输出里
     * 出现该来源命名空间的物品时才加。
     * <p>
     * 复合 source 有第二种用途——目录归属：{@code pressed} 的
     * {@code "alltheores,immersiveengineering"} 输入是通用标签、输出归 ATO，IE 只是同一材料的
     * 另一个提供者。把 IE 也算成开关，关掉 IE 兼容会让 ATO 的配方一起消失。
     */
    private static void addSourceToggles(JsonArray conds, String sources, JsonArray inputs, JsonArray outputs) {
        String primary = sources.split(",")[0].trim();
        for (String raw : sources.split(",")) {
            String source = raw.trim();
            if (!source.equals(primary) && !mentionsNamespace(inputs, source) && !mentionsNamespace(outputs, source)) {
                continue;
            }
            var toggle = new JsonObject();
            toggle.addProperty("type", TEN.MOD_ID + ":compat_enabled");
            toggle.addProperty("source", source);
            conds.add(toggle);
        }
    }

    /** 输入/输出里是否有属于该命名空间的物品或标签 id。 */
    private static boolean mentionsNamespace(JsonArray items, String namespace) {
        for (var element : items) {
            var obj = element.getAsJsonObject();
            if (!obj.has("key")) continue;
            if (obj.get("key").getAsString().startsWith(namespace + ":")) return true;
        }
        return false;
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
