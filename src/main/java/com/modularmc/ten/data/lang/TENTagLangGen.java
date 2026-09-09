package com.modularmc.ten.data.lang;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.Mat;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import java.util.LinkedHashMap;

/**
 * Tag 名称本地化（独立文件，2026-09 用户需求）。
 * <p>
 * JEI 的标签名显示链路：JEI RenderHelper.getName(TagKey) → NeoForge
 * {@code Tags.getTagTranslationKey(tag)} → 键格式
 * {@code tag.<registry>.<namespace>.<path，'/' 换 '.'>}，无翻译时回退
 * {@code #<namespace>:<path>} 显示裸 id。
 * 因此本文件为 KER 注册/引用的全部标签生成标准键
 * （如 {@code tag.item.c.ingots.tin}），并保留旧短键
 * {@code tag.c.ingots.tin}（历史第三方/EMI 查询兼容）。
 * <p>
 * 覆盖范围 = src/main/resources/data 下全部 tags json：
 * <ul>
 * <li>c: 命名空间（类别 + 材质家族，item + block）</li>
 * <li>kenergyengineering: 自有标签（catalyst/spanner/mats/采矿场等）</li>
 * <li>minecraft: 本模组追加值的原版标签（mineable/pickaxe、needs_iron_tool）</li>
 * </ul>
 */
public final class TENTagLangGen {

    public static final LinkedHashMap<String, String> ZH_ENTRIES = new LinkedHashMap<>();

    /** 已写入键去重：item/block 同名类别在旧短键体系下同 key，值相同则合并。 */
    private static final java.util.Set<String> WRITTEN = new java.util.HashSet<>();

    /** Registrate en_us provider 入口（与 TENLangHandler.init 同挂载点）。 */
    public static void init(RegistrateLangProvider provider) {
        // ── c: 物品类别（单段）──
        addTag(provider, "item", "c", "dusts", "Dusts", "粉尘");
        addTag(provider, "item", "c", "gears", "Gears", "齿轮");
        addTag(provider, "item", "c", "gems", "Gems", "宝石");
        addTag(provider, "item", "c", "ingots", "Ingots", "锭");
        addTag(provider, "item", "c", "moulds", "Moulds", "模具");
        addTag(provider, "item", "c", "mushrooms", "Mushrooms", "蘑菇");
        addTag(provider, "item", "c", "nuggets", "Nuggets", "粒");
        addTag(provider, "item", "c", "ores", "Ores", "矿石");
        addTag(provider, "item", "c", "plates", "Plates", "板");
        addTag(provider, "item", "c", "raw_materials", "Raw Materials", "粗原料");
        addTag(provider, "item", "c", "storage_blocks", "Storage Blocks", "存储块");

        // ── c: 物品材质家族（Mat 枚举驱动）──
        // 有锭材质（vanilla 锭重定向的 4 个 + 自研 5 个）
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                Mat.POWERED_TIN, Mat.CHLORIUM, Mat.MUSHRIUM, Mat.NETHERITE }) {
            addMatForm(provider, "ingots", "Ingot", "锭", mat);
        }
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                Mat.POWERED_TIN, Mat.CHLORIUM, Mat.MUSHRIUM, Mat.NETHERITE }) {
            addMatForm(provider, "dusts", "Dust", "粉", mat);
        }
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                Mat.POWERED_TIN, Mat.CHLORIUM, Mat.MUSHRIUM, Mat.NETHERITE }) {
            addMatForm(provider, "nuggets", "Nugget", "粒", mat);
        }
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                Mat.POWERED_TIN, Mat.CHLORIUM, Mat.MUSHRIUM, Mat.NETHERITE }) {
            addMatForm(provider, "plates", "Plate", "板", mat);
        }
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                Mat.POWERED_TIN, Mat.CHLORIUM, Mat.MUSHRIUM, Mat.NETHERITE }) {
            addMatForm(provider, "gears", "Gear", "齿轮", mat);
        }
        // 宝石（gem 类别：材质名即 gem 名）
        for (Mat mat : new Mat[] { Mat.DIAMOND, Mat.EMERALD, Mat.LAPIS, Mat.QUARTZ, Mat.AMETHYST }) {
            addTag(provider, "item", "c", "gems/" + mat.id, mat.englishName(), mat.cn);
        }
        // 粗原料
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL }) {
            addTag(provider, "item", "c", "raw_materials/" + mat.id, "Raw " + mat.englishName(), "粗" + mat.cn);
        }
        // 存储块（物品）
        for (Mat mat : new Mat[] { Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM }) {
            addTag(provider, "item", "c", "storage_blocks/" + mat.id, mat.englishName() + " Block", mat.cn + "块");
        }
        addTag(provider, "item", "c", "storage_blocks/raw_tin", "Raw Tin Block", "粗锡块");
        addTag(provider, "item", "c", "storage_blocks/raw_nickel", "Raw Nickel Block", "粗镍块");

        // ── c: 模具子类 ──
        addTag(provider, "item", "c", "moulds/shape", "Shaping Moulds", "成型模具");
        addTag(provider, "item", "c", "moulds/utility", "Utility Moulds", "功能模具");

        // ── c: 方块类别 ──
        addTag(provider, "block", "c", "cobblestones", "Cobblestones", "圆石");
        addTag(provider, "block", "c", "cobblestones/normal", "Normal Cobblestones", "普通圆石");
        addTag(provider, "block", "c", "gravels", "Gravels", "沙砾");
        addTag(provider, "block", "c", "netherracks", "Netherracks", "下界岩");
        addTag(provider, "block", "c", "obsidians", "Obsidians", "黑曜石");
        addTag(provider, "block", "c", "obsidians/normal", "Normal Obsidians", "普通黑曜石");
        addTag(provider, "block", "c", "ores", "Ores", "矿石");
        addTag(provider, "block", "c", "ores/tin", "Tin Ore", "锡矿石");
        addTag(provider, "block", "c", "ores/nickel", "Nickel Ore", "镍矿石");
        addTag(provider, "block", "c", "sands", "Sands", "沙子");
        addTag(provider, "block", "c", "stones", "Stones", "石头");
        addTag(provider, "block", "c", "storage_blocks", "Storage Blocks", "存储块");
        for (Mat mat : new Mat[] { Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM }) {
            addTag(provider, "block", "c", "storage_blocks/" + mat.id, mat.englishName() + " Block", mat.cn + "块");
        }
        addTag(provider, "block", "c", "storage_blocks/raw_tin", "Raw Tin Block", "粗锡块");
        addTag(provider, "block", "c", "storage_blocks/raw_nickel", "Raw Nickel Block", "粗镍块");

        // ── kenergyengineering 自有物品标签 ──
        addTag(provider, "item", TEN.MOD_ID, "catalyst", "Catalysts", "催化剂");
        addTag(provider, "item", TEN.MOD_ID, "common_ingots", "Common Ingots", "常见金属锭");
        addTag(provider, "item", TEN.MOD_ID, "uncommon_ingots", "Uncommon Ingots", "稀有金属锭");
        addTag(provider, "item", TEN.MOD_ID, "valuable_ingots", "Valuable Ingots", "贵重金属锭");
        addTag(provider, "item", TEN.MOD_ID, "spanner", "Spanners", "扳手");
        for (Mat mat : new Mat[] { Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                Mat.POWERED_TIN, Mat.CHLORIUM }) {
            addTag(provider, "item", TEN.MOD_ID, "mats/" + mat.id,
                    mat.englishName() + " Materials", mat.cn + "材料");
        }

        // ── kenergyengineering 方块标签 ──
        addTag(provider, "block", TEN.MOD_ID, "quarry_valids", "Quarry-Valid Blocks", "采矿场可挖掘方块");
        addTag(provider, "block", TEN.MOD_ID, "wrench_dismantleable", "Wrench Dismantleable", "扳手可拆解");

        // ── minecraft: 本模组追加值的原版方块标签 ──
        addTag(provider, "block", "minecraft", "mineable/pickaxe", "Mineable by Pickaxe", "镐可挖掘");
        addTag(provider, "block", "minecraft", "needs_iron_tool", "Requires Iron Tool", "需要铁质工具");
    }

    /** 材质家族条目：en "{Material} {FormEn}" / zh "{cn}{FormCn}"。 */
    private static void addMatForm(RegistrateLangProvider provider, String category,
                                   String formEn, String formCn, Mat mat) {
        addTag(provider, "item", "c", category + "/" + mat.id,
                mat.englishName() + " " + formEn, mat.cn + formCn);
    }

    /**
     * 写入标准 JEI/EMI/NeoForge 键 + 旧短键（EMI 第三层兕底）。
     * 标准：tag.item.c.ingots.tin（JEI 与 EMI 在 NeoForge 下同构）；旧：tag.c.ingots.tin。
     * 旧短键 item/block 同名类别（ores/storage_blocks）值相同，合并写入（WRITTEN 去重）。
     */
    private static void addTag(RegistrateLangProvider provider, String registry,
                               String namespace, String path, String en, String cn) {
        String tagPath = path.replace('/', '.');
        addRaw(provider, "tag." + registry + "." + namespace + "." + tagPath, en, cn);
        addRaw(provider, "tag." + namespace + "." + tagPath, en, cn);
    }

    private static void addRaw(RegistrateLangProvider provider, String key, String en, String cn) {
        if (!WRITTEN.add(key)) {
            return; // 重复键（item/block 同名短键同值）：首写为准
        }
        ZH_ENTRIES.put(key, cn);
        provider.add(key, en);
    }

    private TENTagLangGen() {}
}
