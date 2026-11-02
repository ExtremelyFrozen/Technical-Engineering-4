package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TENTags {

    public static final TagKey<Block> MACHINES = blockTag("machines");
    /**
     * 扳手类物品（旋转、拆解、管道连接模式切换的交互判定）。SpannerItem 经 datagen 注册入内。
     * <p>
     * 判定只认这个自有标签，不认 {@code c:tools/wrench}：NeoForge 对该 convention tag 的说明是
     * 「不要用它判断工具行为」，各家扳手语义不同（IE 砸多方块、EIO 拆机器、Mekanism 改配置），
     * 放开会让别的扳手误触发拆解。物品归类另由
     * {@code data/c/tags/item/tools/wrench.json} 补上，供整合包与配方匹配。
     */
    public static final TagKey<Item> SPANNER = itemTag("spanner");

    private static TagKey<Block> blockTag(String path) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, path));
    }

    private static TagKey<Item> itemTag(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, path));
    }

    public static void init() {}
}
