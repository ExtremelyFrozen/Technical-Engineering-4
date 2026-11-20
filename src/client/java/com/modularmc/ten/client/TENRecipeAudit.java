package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.api.recipe.RecipeSignature;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * <p>
 * <b>过滤必须按实例类型做，不能按 {@code TENRecipeTypes.TYPES} 强转</b>：那个注册表里不全是机器配方
 * （{@code engine_fuel} 是生产端的燃料表，没有「输入签名」）。早先这里把每个类型都当成
 * {@code RecipeType<FormsCombinedRecipe>} 再逐条取 {@code input()}，新类型一进来就抛
 * {@code ClassCastException}；而本监听器挂在 {@code RecipesUpdatedEvent} 上，该事件由
 * {@code ClientboundUpdateRecipesPacket} 在处理线程上触发，异常会直接断连——
 * 客户端看到的是「连接已丢失 / 网络协议错误」。
 */
public final class TENRecipeAudit {

    private TENRecipeAudit() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TENRecipeAudit::onRecipesUpdated);
    }

    private static void onRecipesUpdated(RecipesUpdatedEvent event) {
        // 本监听器只做诊断，跑在封包处理线程上：自身出错不该把玩家踢下线，兜住并记 ERROR
        try {
            audit(event);
        } catch (Exception e) {
            TEN.LOGGER.error("配方歧义扫描失败（不影响游戏）", e);
        }
    }

    private static void audit(RecipesUpdatedEvent event) {
        // 一次遍历全表，按类型与输入签名分组；非机器配方在 instanceof 处被丢掉
        Map<RecipeType<?>, Map<String, List<ResourceLocation>>> byType = new LinkedHashMap<>();
        for (var loaded : event.getRecipeManager().getRecipes()) {
            if (!(loaded.value() instanceof FormsCombinedRecipe recipe)) continue;
            byType.computeIfAbsent(recipe.getType(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(RecipeSignature.ofInputs(recipe.input()), k -> new ArrayList<>())
                    .add(loaded.id());
        }

        for (var typeEntry : byType.entrySet()) {
            for (var entry : typeEntry.getValue().entrySet()) {
                if (entry.getValue().size() < 2) continue;
                // findRecipe 取第一条命中，后面的同签名配方永不生效
                TEN.LOGGER.error("机器配方歧义 [{}]：输入 {} 被 {} 条配方同时占用，只有第一条生效：{}",
                        BuiltInRegistries.RECIPE_TYPE.getKey(typeEntry.getKey()),
                        entry.getKey(), entry.getValue().size(), entry.getValue());
            }
        }
    }
}
