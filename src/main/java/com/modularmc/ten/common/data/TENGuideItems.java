package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import guideme.Guides;

public final class TENGuideItems {

    private TENGuideItems() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(TENGuideItems::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == TENCreativeModeTabs.TOOL_TAB.get()) {
            event.accept(Guides.createGuideItem(TEN.id("guide")), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
