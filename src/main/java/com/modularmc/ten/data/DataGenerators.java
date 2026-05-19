package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENCreativeModeTabs;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.data.TENItems;
import com.modularmc.ten.data.lang.TENLangHandler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();

        // Registrate handles block/item models, blockstates, and English lang entries automatically.

        // Chinese localization — reads from all ZH_NAMES/ZH_ENTRIES maps
        generator.addProvider(event.includeClient(), new LanguageProvider(output, TEN.MOD_ID, "zh_cn") {

            @Override
            protected void addTranslations() {
                // 1. From 4 static maps (blocks, items, fluids, lang entries)
                TENBlocks.ZH_NAMES.forEach((id, cn) -> add("block." + TEN.MOD_ID + "." + id, cn));
                TENItems.ZH_NAMES.forEach((id, cn) -> add("item." + TEN.MOD_ID + "." + id, cn));
                TENFluids.ZH_NAMES.forEach((id, cn) -> add("block." + TEN.MOD_ID + "." + id, cn));
                TENLangHandler.ZH_ENTRIES.forEach(this::add);

                // Creative tabs
                TENCreativeModeTabs.ZH_NAMES.forEach(this::add);

                // 2a. Auto-generate standalone keys from item and block registrations
                // (kenergyengineering.xxx = same value as item.kenergyengineering.xxx / block.kenergyengineering.xxx)
                TENItems.ZH_NAMES.forEach((id, cn) -> add(TEN.MOD_ID + "." + id, cn));
                TENBlocks.ZH_NAMES.forEach((id, cn) -> add(TEN.MOD_ID + "." + id, cn));
                TENFluids.ZH_FLUID_KEYS.forEach((key, cn) -> add(key, cn));

                // 2b. Tag translations (no registration function to hook into)
                // Common tags (tag.c.*)
                add("tag.c.dusts.chlorium", "叶绿粉");
                add("tag.c.dusts.nickel", "镍粉");
                add("tag.c.dusts.powered_tin", "充能锡粉");
                add("tag.c.dusts.tin", "锡粉");
                add("tag.c.gears", "齿轮");
                add("tag.c.gears.chlorium", "叶绿齿轮");
                add("tag.c.gears.copper", "铜齿轮");
                add("tag.c.gears.gold", "金齿轮");
                add("tag.c.gears.iron", "铁齿轮");
                add("tag.c.gears.nickel", "镍齿轮");
                add("tag.c.gears.powered_tin", "充能锡齿轮");
                add("tag.c.gears.tin", "锡齿轮");
                add("tag.c.gears.netherite", "下界合金齿轮");
                add("tag.c.ingots.chlorium", "叶绿锭");
                add("tag.c.ingots.nickel", "镍锭");
                add("tag.c.ingots.powered_tin", "充能锡锭");
                add("tag.c.ingots.iron", "铁锭");
                add("tag.c.ingots.gold", "金锭");
                add("tag.c.ingots.copper", "铜锭");
                add("tag.c.ingots.netherite", "下界合金锭");
                add("tag.c.ingots.tin", "锡锭");
                add("tag.c.ingots.mushrium", "蘑菇锭");
                add("tag.c.plates.mushrium", "蘑菇板");
                add("tag.c.gears.mushrium", "蘑菇齿轮");
                add("tag.c.nuggets.mushrium", "蘑菇粒");
                add("tag.c.dusts.mushrium", "蘑菇粉");
                add("tag.c.dusts.netherite", "下界合金粉");
                add("tag.c.nuggets.chlorium", "叶绿粒");
                add("tag.c.nuggets.nickel", "镍粒");
                add("tag.c.nuggets.powered_tin", "充能锡粒");
                add("tag.c.nuggets.copper", "铜粒");
                add("tag.c.nuggets.iron", "铁粒");
                add("tag.c.nuggets.gold", "金粒");
                add("tag.c.nuggets.netherite", "下界合金粒");
                add("tag.c.nuggets.tin", "锡粒");
                add("tag.c.ores.nickel", "镍矿石");
                add("tag.c.ores.tin", "锡矿石");
                add("tag.c.plates.chlorium", "叶绿板");
                add("tag.c.plates.nickel", "镍板");
                add("tag.c.plates.powered_tin", "充能锡板");
                add("tag.c.plates.netherite", "下界合金板");
                add("tag.c.plates.tin", "锡板");
                add("tag.c.raw_materials.nickel", "粗镍");
                add("tag.c.raw_materials.tin", "粗锡");
                add("tag.c.storage_blocks.chlorium", "叶绿块");
                add("tag.c.storage_blocks.nickel", "镍块");
                add("tag.c.storage_blocks.powered_tin", "充能锡块");
                add("tag.c.storage_blocks.raw_nickel", "粗镍块");
                add("tag.c.storage_blocks.raw_tin", "粗锡块");
                add("tag.c.storage_blocks.tin", "锡块");
                // Mod tags (tag.kenergyengineering.*)
                add("tag.kenergyengineering.catalyst", "催化剂");
                add("tag.kenergyengineering.common_ingots", "常见金属锭");
                add("tag.kenergyengineering.mats.chlorium", "叶绿材料");
                add("tag.kenergyengineering.mats.copper", "铜材料");
                add("tag.kenergyengineering.mats.gold", "金材料");
                add("tag.kenergyengineering.mats.iron", "铁材料");
                add("tag.kenergyengineering.mats.nickel", "镍材料");
                add("tag.kenergyengineering.mats.powered_tin", "充能锡材料");
                add("tag.kenergyengineering.mats.tin", "锡材料");
                add("tag.kenergyengineering.moulds", "模具");
                add("tag.kenergyengineering.uncommon_ingots", "稀有金属锭");
                add("tag.kenergyengineering.valuable_ingots", "贵重金属锭");

                // 2b5. Miscellaneous items not covered by standard registration paths
                add("item.kenergyengineering.spanner", "扳手");
                add("kenergyengineering.energy_core", "能量核心");
                add("item.kenergyengineering.liquid_bizarrerie_bucket", "奇异物质桶");
                add("kenergyengineering.liquid_honey", "蜂蜜");
                add("kenergyengineering.liquid_honey_bucket", "蜂蜜桶");
                add("kenergyengineering.liquid_royal_jelly", "蜂王浆");
                add("kenergyengineering.liquid_royal_jelly_bucket", "蜂王浆桶");
                add("kenergyengineering.liquid_spicy_jelly", "香料酱");
                add("kenergyengineering.liquid_spicy_jelly_bucket", "香料酱桶");
                add("item.kenergyengineering.liquid_xp_bucket", "液态经验桶");
                add("kenergyengineering.machine_frame", "机械框架");
                add("kenergyengineering.royal_jelly", "蜂王浆");
                add("kenergyengineering.spicy_jelly", "香料酱");
                add("kenergyengineering.world_bag", "世界袋");
                add("kenergyengineering.liquid_bizarrerie", "奇异物质");
                add("kenergyengineering.liquid_xp", "液态经验");

                // 2c. Supplement from hand-written zh_cn.json for entries not covered by maps above
                // Map entries take priority since they are added first (LanguageProvider skips duplicates).
                try {
                    var file = new java.io.File("src/main/resources/assets/" + TEN.MOD_ID + "/lang/zh_cn.json");
                    if (file.exists()) {
                        var json = new com.google.gson.JsonParser()
                                .parse(new java.io.FileReader(file))
                                .getAsJsonObject();
                        json.entrySet().forEach(entry -> add(entry.getKey(), entry.getValue().getAsString()));
                    }
                } catch (Exception e) {
                    // Silently skip if file doesn't exist (e.g. after deleting the hand-written file)
                }
            }
        });

        // Material variant recipes — auto-generated from Mat enum
        generator.addProvider(event.includeServer(), new TENRecipeGen(output, event.getLookupProvider()));
        // Vanilla pack/split recipes — separate from mod materials
        generator.addProvider(event.includeServer(), new TENVanillaPackGen(output));
    }
}
