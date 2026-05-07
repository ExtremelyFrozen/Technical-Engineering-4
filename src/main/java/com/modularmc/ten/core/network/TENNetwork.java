package com.modularmc.ten.core.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class TENNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // TODO: register packets during content migration
    }
}
