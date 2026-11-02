package com.modularmc.ten.data;

import com.modularmc.ten.data.lang.TENLangHandler;
import com.modularmc.ten.data.lang.TENTagLangGen;

import com.tterrag.registrate.providers.ProviderType;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public final class TENDataGen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, TENLangHandler::init);
        // 标签本地化（独立文件）：JEI/NeoForge 标准键 tag.<registry>.<ns>.<path> + 旧短键
        REGISTRATE.addDataGenerator(ProviderType.LANG, TENTagLangGen::init);
    }
}
