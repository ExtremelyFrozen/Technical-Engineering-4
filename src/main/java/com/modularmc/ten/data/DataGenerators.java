package com.modularmc.ten.data;

import com.modularmc.ten.TEN;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // Registrate handles block/item models, blockstates, and lang entries automatically.
    }
}
