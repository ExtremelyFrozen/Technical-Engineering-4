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
     * <b>为什么是「去除」而不是「赋予」</b>：按「赋予」写，21 台设备（15 机器 + 4 引擎 +
     * 2 单元）要各打三条共 63 条，其中流体一条对无罐机器毫无信息量（有无罐体已由
     * {@code supportsFaceResource} 判掉）、物品与能量两条对这 21 台全是「允许」，
     * 真正承载的事实只有「频道方块排除在外」。按「去除」写，三个标签各一条就说完。
     *
     * <p>
     * 功能上也更贴合设备现实：危险面不是「机器会自搬」，而是「共享存储型方块不该自搬」——
     * 频道的面只是 {@code SharedStorage} 的被动接入点，本地罐/槽是断开后的回流缓冲，
     * 机器一旦自搬就绕过了共享存储。默认放开 + 显式去除，新加机器不会因为忘打标签而
     * 静默退化成被动-only。
     *
     * <p>
     * <b>三个标签极性必须一致</b>：都表示「去除可主动」，判定走同一条
     * {@code allowsActiveFace} 分支，不许出现某个资源用白名单、另一个用黑名单的混搭。
     * 默认允许的另一面是：新加共享存储型方块（不止频道）必须记得补标签。
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
