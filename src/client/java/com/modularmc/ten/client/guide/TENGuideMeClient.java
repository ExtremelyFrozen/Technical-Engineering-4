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
        // folder 是 GuideME 的跨命名空间内容根：与 AE2 共用 ae2guide 会被并进它的目录树，
        // 自己的指南反而取不到起始页，所以这里用独立 folder
        Guide.builder(TEN.id("guide"))
                .folder("kenergyguide")
                .startPage(TEN.id("index"))
                .register(true)
                .itemSettings(new GuideItemSettings(
                        Optional.of(Component.translatable("item.kenergyengineering.guide")),
                        List.of(Component.translatable("item.kenergyengineering.guide.tooltip")),
                        Optional.empty()))
                .build();
    }
}
