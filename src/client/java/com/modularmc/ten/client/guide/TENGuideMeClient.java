package com.modularmc.ten.client.guide;

import com.modularmc.ten.TEN;

import net.minecraft.network.chat.Component;

import guideme.Guide;
import guideme.GuideItemSettings;

import java.util.List;
import java.util.Optional;

public final class TENGuideMeClient {

    private TENGuideMeClient() {}

    public static void init() {
        Guide.builder(TEN.id("guide"))
                .folder("ae2guide")
                .startPage(TEN.id("index"))
                .register(true)
                .itemSettings(new GuideItemSettings(
                        Optional.of(Component.translatable("item.kenergyengineering.guide")),
                        List.of(Component.translatable("item.kenergyengineering.guide.tooltip")),
                        Optional.empty()))
                .build();
    }
}
