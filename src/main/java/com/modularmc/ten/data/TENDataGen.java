package com.modularmc.ten.data;

import com.modularmc.ten.data.lang.TENLangHandler;

import com.tterrag.registrate.providers.ProviderType;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public final class TENDataGen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, TENLangHandler::init);
    }
}
