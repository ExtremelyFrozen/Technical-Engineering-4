package com.modularmc.ten.client.guide;

import com.modularmc.ten.TEN;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import guideme.Guide;
import guideme.GuideItemSettings;

import java.util.List;
import java.util.Optional;

public final class TENGuideMeClient {

    /**
     * 本书的书物品模型。GuideME 的 dispatch 只把自家底图加进烘焙表，
     * 各册自带的 itemModel 要模组自己注册，见 {@code TENClientSetup}。
     */
    public static final ResourceLocation GUIDE_ITEM_MODEL = TEN.id("item/pedia");

    private TENGuideMeClient() {}

    public static void init() {
        // folder 是跨命名空间内容根（listResources 在每个命名空间下找 assets/<ns>/<folder>/**.md，
        // 页面 id =「<ns>:folder 内相对路径」含 .md）：与 AE2 共用 ae2guide 会被并进它的目录树
        // startPage 不写——GuideBuilder 缺省即 <ns>:index.md（AE2/Powah 都不写），
        // 上一版手写 "index" 少扩展名，取不到页，书打开是空的
        Guide.builder(TEN.id("guide"))
                .folder("kenergyguide")
                .register(true)
                .itemSettings(new GuideItemSettings(
                        Optional.of(Component.translatable("item.kenergyengineering.guide")),
                        List.of(Component.translatable("item.kenergyengineering.guide.tooltip")),
                        // GuideME 的书物品是单体 dispatch 模型，第三参数给本册的 item model id。
                        // 必须自带 item/ 前缀：dispatch 直接 modelBaker.getModel(id)，不补前缀
                        // （GuideME 底图也是显式的 guideme:item/guide_base）
                        Optional.of(GUIDE_ITEM_MODEL)))
                .build();
    }
}
