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
     * 面配置「主动模式」授权标签，按资源类型各一个：带标签的方块允许把该资源的某个面
     * 配成主动输入（机器自拉）或主动输出（机器自推）。
     *
     * <p>
     * 为什么要标签而不是代码开关：主动面会让方块绕过外部调度自搬，属于「可能破坏方块自身
     * 存储模型」的危险配置——频道方块的共享存储就是典型反例（面只是被动接入点，机器自搬会
     * 把外部流体灌进本地断开缓冲）。用方块标签声明，整合包与附属方块也能按需授予/收回，
     * 不必改代码。三个标签的组合即该方块的资源 IO 限制：
     * <ul>
     * <li>不带任何标签 → 面板只剩「关闭 + 被动三态」，与共享存储型方块一致；</li>
     * <li>带 {@code face_active_energy} → 能量页可选主动输入/输出；</li>
     * <li>三者全带 → 有对应能力的资源页都能选主动模式（无该资源能力的页仍只剩关闭，
     * 由 {@code supportsFaceResource} 判定）。</li>
     * </ul>
     *
     * <p>
     * 标签只授予「主动」，不授予「存在」：某台机器有没有流体页仍看它自己有没有罐体，
     * 避免给所有机器都塞流体页。
     */
    public static final TagKey<Block> FACE_ACTIVE_ENERGY = blockTag("face_active_energy");
    public static final TagKey<Block> FACE_ACTIVE_ITEM = blockTag("face_active_item");
    public static final TagKey<Block> FACE_ACTIVE_FLUID = blockTag("face_active_fluid");

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
