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

/**
 * Data generator entry point for all auto-generated assets and data.
 * <p>
 * <b>Language ownership</b>:
 * <ul>
 * <li><b>src/generated/resources</b> — sole owner of both {@code en_us.json}
 * and {@code zh_cn.json}. Generated via the nested {@link LangProvider} from
 * paired data sources ({@code EN_NAMES}/{@code ZH_NAMES} and
 * {@link TENLangHandler#EN_ENTRIES}/{@code ZH_ENTRIES}).</li>
 * <li><b>src/main/resources/assets/kenergyengineering/lang/</b> — empty directory.
 * No hand-written locale files exist.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class DataGenerators {

    /**
     * Private nested language provider that generates en_us or zh_cn from paired
     * data sources. Single {@link #addTranslations()} structure for both locales —
     * locale parameter selects EN vs ZH data source. Never writes duplicate keys.
     */
    private static final class LangProvider extends LanguageProvider {

        private final boolean isEnglish;

        LangProvider(net.minecraft.data.PackOutput output, String locale) {
            super(output, TEN.MOD_ID, locale);
            this.isEnglish = "en_us".equals(locale);
        }

        @Override
        protected void addTranslations() {
            if (isEnglish) {
                TENBlocks.EN_NAMES.forEach((id, name) -> addBlockName(id, name));
                TENItems.EN_NAMES.forEach((id, name) -> addItemName(id, name));
                TENFluids.EN_NAMES.forEach((id, name) -> addBlockName(id, name));
                TENLangHandler.EN_ENTRIES.forEach(this::add);
                TENCreativeModeTabs.EN_NAMES.forEach(this::add);
                TENItems.EN_NAMES.forEach((id, name) -> add(TEN.MOD_ID + "." + id, name));
                TENBlocks.EN_NAMES.forEach((id, name) -> add(TEN.MOD_ID + "." + id, name));
                TENFluids.EN_FLUID_KEYS.forEach(this::add);
                TENFluids.EN_NAMES.forEach((id, en) -> add("item." + TEN.MOD_ID + "." + id + "_bucket", en + " Bucket"));
            } else {
                TENBlocks.ZH_NAMES.forEach((id, name) -> addBlockName(id, name));
                TENItems.ZH_NAMES.forEach((id, name) -> addItemName(id, name));
                TENFluids.ZH_NAMES.forEach((id, name) -> addBlockName(id, name));
                TENLangHandler.ZH_ENTRIES.forEach(this::add);
                TENCreativeModeTabs.ZH_NAMES.forEach(this::add);
                TENItems.ZH_NAMES.forEach((id, name) -> add(TEN.MOD_ID + "." + id, name));
                TENBlocks.ZH_NAMES.forEach((id, name) -> add(TEN.MOD_ID + "." + id, name));
                TENFluids.ZH_FLUID_KEYS.forEach(this::add);
                TENFluids.ZH_NAMES.forEach((id, cn) -> add("item." + TEN.MOD_ID + "." + id + "_bucket", cn + "桶"));
            }
        }

        private void addBlockName(String id, String name) {
            add("block." + TEN.MOD_ID + "." + id, name);
        }

        private void addItemName(String id, String name) {
            add("item." + TEN.MOD_ID + "." + id, name);
        }
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();

        // TWO language providers — same structure, different locale
        generator.addProvider(true, new LangProvider(output, "en_us"));
        generator.addProvider(true, new LangProvider(output, "zh_cn"));

        // Matrix validator — runs before recipe providers to catch
        // registration-recipe mismatches early (fail-fast only, no file output)
        generator.addProvider(true, new TENRecipeMatrixValidator());

        // Material variant recipes — auto-generated from Mat enum
        generator.addProvider(true, new TENRecipeGen(output, event.getLookupProvider()));
        // Vanilla pack/split recipes — separate from mod materials
        generator.addProvider(true, new TENVanillaPackGen(output));

        // Model/blockstate provider — generates all blockstates, block models, and item models
        generator.addProvider(true, new TENModelProvider(output, TEN.MOD_ID));

        // Block loot tables — generates self-drop loot for all 38 blocks
        generator.addProvider(true, new TENLootTableProvider(output));

        // Block, item, and fluid tags (mod + minecraft namespace)
        generator.addProvider(true, new TENTagProvider(output));
    }
}
