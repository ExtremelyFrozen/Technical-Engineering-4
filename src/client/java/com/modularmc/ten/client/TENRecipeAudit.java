package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.api.recipe.RecipeSignature;
import com.modularmc.ten.common.data.TENRecipeTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行期重复性扫描：配方表装载完成后，按机器类型分组统计输入签名，
 * 同一签名被两条以上配方占用就打 ERROR。
 * <p>
 * 生成期的 {@link RecipeSignature#claim} 只看得到我们自己写出的 JSON；数据包可以覆盖、
 * 追加配方，邻居的开关也会让一部分配方消失，所以真正生效的那份歧义要在加载后确认。
 * NeoForge 1.21.1 只有客户端的 {@link RecipesUpdatedEvent}（服务端配方同步下来时触发），
 * 没有运行期增删配方的事件，因此扫描挂在客户端；服务器本地那份由同一次加载同步上来，
 * 单机与联机都会在日志里出现同样的行。
 */
public final class TENRecipeAudit {

    private TENRecipeAudit() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TENRecipeAudit::onRecipesUpdated);
    }

    @SuppressWarnings("unchecked")
    private static void onRecipesUpdated(RecipesUpdatedEvent event) {
        RecipeManager manager = event.getRecipeManager();
        for (var holder : TENRecipeTypes.TYPES.getEntries()) {
            var type = (RecipeType<FormsCombinedRecipe>) holder.value();
            Map<String, List<ResourceLocation>> bySignature = new LinkedHashMap<>();
            for (var loaded : manager.getAllRecipesFor(type)) {
                bySignature.computeIfAbsent(RecipeSignature.ofInputs(loaded.value().input()), k -> new ArrayList<>())
                        .add(loaded.id());
            }
            for (var entry : bySignature.entrySet()) {
                if (entry.getValue().size() < 2) continue;
                // findRecipe 取第一条命中，后面的同签名配方永不生效
                TEN.LOGGER.error("机器配方歧义 [{}]：输入 {} 被 {} 条配方同时占用，只有第一条生效：{}",
                        holder.getId(), entry.getKey(), entry.getValue().size(), entry.getValue());
            }
        }
    }
}
