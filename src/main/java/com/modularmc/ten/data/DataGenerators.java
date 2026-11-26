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
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();

        // Registrate 自动生成方块/物品模型、blockstate 与英文语言条目。

        // 中文本地化：汇总各 ZH_NAMES/ZH_ENTRIES 映射。
        generator.addProvider(event.includeClient(), new LanguageProvider(output, TEN.MOD_ID, "zh_cn") {

            @Override
            protected void addTranslations() {
                // 1. 四类静态映射：方块、物品、流体与语言条目。
                TENBlocks.ZH_NAMES.forEach((id, cn) -> add("block." + TEN.MOD_ID + "." + id, cn));
                TENItems.ZH_NAMES.forEach((id, cn) -> add("item." + TEN.MOD_ID + "." + id, cn));
                TENFluids.ZH_NAMES.forEach((id, cn) -> add("block." + TEN.MOD_ID + "." + id, cn));
                TENLangHandler.ZH_ENTRIES.forEach(this::add);

                // 创造模式物品栏。
                TENCreativeModeTabs.ZH_NAMES.forEach(this::add);

                // 2a. 从物品与方块注册信息生成裸键。
                TENItems.ZH_NAMES.forEach((id, cn) -> add(TEN.MOD_ID + "." + id, cn));
                TENBlocks.ZH_NAMES.forEach((id, cn) -> add(TEN.MOD_ID + "." + id, cn));
                TENFluids.ZH_FLUID_KEYS.forEach((key, cn) -> add(key, cn));

                // 2b. 标签翻译由 TENTagLangGen.ZH_ENTRIES 提供，包含 JEI/NeoForge 标准键与兼容短键。
                TENTagLangGen.ZH_ENTRIES.forEach(this::add);

                // 2b5. 补充流体桶物品名，供背包与容器界面显示。
                add("item.kenergyengineering.liquid_bizarrerie_bucket", "奇异物质桶");
                add("item.kenergyengineering.liquid_honey_bucket", "蜂蜜桶");
                add("item.kenergyengineering.liquid_royal_jelly_bucket", "蜂王浆桶");
                add("item.kenergyengineering.liquid_spicy_jelly_bucket", "香辣蜂王浆桶");
                add("item.kenergyengineering.liquid_xp_bucket", "液态经验桶");

                // 2c. 从手写 zh_cn.json 补充条目。
                try {
                    var file = new java.io.File("src/main/resources/assets/" + TEN.MOD_ID + "/lang/zh_cn.json");
                    if (file.exists()) {
                        var json = new com.google.gson.JsonParser()
                                .parse(new java.io.FileReader(file))
                                .getAsJsonObject();
                        json.entrySet().forEach(entry -> add(entry.getKey(), entry.getValue().getAsString()));
                    }
                } catch (Exception e) {
                    // 文件不存在时跳过。
                }
            }
        });
        // 材料变体配方。
        generator.addProvider(event.includeServer(), new TENRecipeGen(output, event.getLookupProvider()));
        // c:mats/* 聚合标签（锭或粉）。
        generator.addProvider(event.includeServer(), new TENMaterialTagGen(output));
        // 邻居模组兼容配方（EIO 系，条件门控）。
        generator.addProvider(event.includeServer(), new TENCompatGen(output));
        // 原版物品的压缩与拆分配方。
        generator.addProvider(event.includeServer(), new TENVanillaPackGen(output));
        // 引擎燃料配方（数据包 engine_fuel，运行期事实源）。
        generator.addProvider(event.includeServer(), new TENFuelGen(output));
        // 主世界锡与镍的矿石生成（configured/placed feature 与 biome modifier）。
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(output, event.getLookupProvider(), TENWorldGenGen.BUILDER, Set.of(TEN.MOD_ID)));
    }
}
