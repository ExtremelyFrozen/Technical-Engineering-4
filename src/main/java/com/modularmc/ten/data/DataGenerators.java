package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENCreativeModeTabs;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.data.TENItems;
import com.modularmc.ten.data.lang.TENLangHandler;
import com.modularmc.ten.data.lang.TENTagLangGen;

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
                TENItems.ZH_NAMES.forEach((id, cn) -> add(TEN.MOD_ID + "." + id, cn));
                TENBlocks.ZH_NAMES.forEach((id, cn) -> add(TEN.MOD_ID + "." + id, cn));
                TENFluids.ZH_FLUID_KEYS.forEach((key, cn) -> add(key, cn));

                // 2b. Tag translations → 已迁至独立文件 TENTagLangGen（含 JEI/NeoForge 标准键 + 双语）
                TENTagLangGen.ZH_ENTRIES.forEach(this::add);

                // 2b5. Miscellaneous items
                add("kenergyengineering.energy_core", "能量核心");
                add("item.kenergyengineering.liquid_bizarrerie_bucket", "奇异物质桶");
                add("kenergyengineering.liquid_honey", "蜂蜜");
                add("item.kenergyengineering.liquid_honey_bucket", "蜂蜜桶");
                add("kenergyengineering.liquid_royal_jelly", "蜂王浆");
                add("item.kenergyengineering.liquid_royal_jelly_bucket", "蜂王浆桶");
                add("kenergyengineering.liquid_spicy_jelly", "香辣蜂王浆");
                add("item.kenergyengineering.liquid_spicy_jelly_bucket", "香辣蜂王浆桶");
                add("item.kenergyengineering.liquid_xp_bucket", "液态经验桶");
                add("kenergyengineering.machine_frame", "机械框架");
                add("kenergyengineering.world_bag", "世界袋");
                add("kenergyengineering.liquid_bizarrerie", "奇异物质");
                add("kenergyengineering.liquid_xp", "液态经验");

                // 2c. Supplement from hand-written zh_cn.json
                try {
                    var file = new java.io.File("src/main/resources/assets/" + TEN.MOD_ID + "/lang/zh_cn.json");
                    if (file.exists()) {
                        var json = new com.google.gson.JsonParser()
                                .parse(new java.io.FileReader(file))
                                .getAsJsonObject();
                        json.entrySet().forEach(entry -> add(entry.getKey(), entry.getValue().getAsString()));
                    }
                } catch (Exception e) {
                    // Silently skip if file doesn't exist
                }
            }
        });
        // Material variant recipes
        generator.addProvider(event.includeServer(), new TENRecipeGen(output, event.getLookupProvider()));
        // Vanilla pack/split recipes
        generator.addProvider(event.includeServer(), new TENVanillaPackGen(output));
    }
}
