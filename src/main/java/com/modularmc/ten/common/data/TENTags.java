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
     * 面配置「禁止主动模式」标签，按资源类型各一个：带标签的方块不允许把该资源的某个面
     * 配成主动输入（机器自拉）或主动输出（机器自推），只能留在「关闭 + 被动三态」。
     *
     * <p>
     * <b>基础模组当前不打任何条目</b>——这是留给整合包/附属方块的覆盖点，不是内部配置表。
     * 方向级限制已由设备功能性给出（{@code supportsFaceDirection}：能量输出只给产能类，
     * 物品/流体按槽位与罐体方向），标签只负责回答「这个方块整体该不该自搬」这类代码里
     * 推不出的策略问题。
     *
     * <p>
     * <b>为什么是「去除」而不是「赋予」</b>：按「赋予」写，每台设备都要逐个打标签，
     * 新加机器忘打就静默失去功能；按「去除」写，默认放开、显式收回，漏配只会多给能力
     * 而不是悄悄拿走能力。
     *
     * <p>
     * <b>三个标签极性必须一致</b>：都表示「去除可主动」，判定走同一条
     * {@code allowsActiveFace} 分支，不许出现某个资源用白名单、另一个用黑名单的混搭。
     */
    public static final TagKey<Block> NO_ACTIVE_FACE_ENERGY = blockTag("no_active_face_energy");
    public static final TagKey<Block> NO_ACTIVE_FACE_ITEM = blockTag("no_active_face_item");
    public static final TagKey<Block> NO_ACTIVE_FACE_FLUID = blockTag("no_active_face_fluid");

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
