package com.modularmc.ten.utils;

import com.modularmc.ten.TEN;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class AdvancementHelper {

    public static void giveAdvancement(String name, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        AdvancementHolder adv = serverPlayer.level().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(TEN.MOD_ID, name));
        if (adv == null) return;

        AdvancementProgress ap = serverPlayer.getAdvancements().getOrStartProgress(adv);
        if (!ap.isDone()) {
            for (String criterion : ap.getCompletedCriteria()) {
                serverPlayer.getAdvancements().award(adv, criterion);
            }
        }
    }
}
