package com.modularmc.ten.data;

import com.modularmc.ten.data.lang.TENLangHandler;

import com.tterrag.registrate.providers.ProviderType;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public final class TENDataGen {

    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        REGISTRATE.addDataGenerator(ProviderType.LANG, TENLangHandler::init);
    }

    private TENDataGen() {}
}
