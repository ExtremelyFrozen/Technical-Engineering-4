package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.TEN;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 升级 tooltip 生成器：数值全部取自 {@link UpgradeConstants} 与
 * {@link UpgradeItem#modifiers()}，模板键由 {@code TENLangHandler} 生成。
 *
 * <p>
 * 只写升级组件自身带来的收益：整合组件给「全局倍率」一个关键词行，增益组件只列
 * {@code 速度 / 耗时 / 功率 / 并行运行数} 四条通道里真正被改动的那些——运行速度（+N）、
 * 运行耗时（±n%）、运行功率（FE/t），需要并行时再补一行并行运行数。
 * 没有变化的维度整行不出现，不写「不变」。独特组件只写卡面专有文本。
 * 插件类别、互斥关系与适用机型都不进文案——冲突在放入槽位的那一刻由
 * {@code validUpgrade} / {@code canApply} 直接拒绝。
 */
public final class UpgradeTooltipFormatter {

    private static final double EPSILON = 1e-6;

    private UpgradeTooltipFormatter() {}

    public static List<Component> format(UpgradeItem item) {
        List<Component> lines = new ArrayList<>();
        String path = regPathFor(item);
        addTitle(lines, path);

        if (item.category() == UpgradeCategory.TIER) {
            addTier(lines, item.modifiers());
        } else if (item.category() == UpgradeCategory.MODIFIER) {
            addModifier(lines, item.modifiers());
        }
        addCardText(lines, item, path);

        return lines;
    }

    /**
     * 整合组件：只给「全局倍率 ×k」这一个关键词行。
     * 速度 / 耗时 / 耗电三者的变化已由该关键词定义，不再逐项展开；
     * 整合组件不带并行运行，也就不出并行运行行。
     */
    private static void addTier(List<Component> lines, UpgradeEffect effect) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.global.multiply", decimal(effect.power()))
                .withStyle(ChatFormatting.GOLD));
    }

    /**
     * 增益组件：只列出实际改变的通道，没有变化的维度整行不写。
     *
     * <p>
     * 可写的有四项：运行速度（+N）、运行耗时（配方本身的长度，±n%）、
     * 运行功率（FE/t）、并行运行数。速度与耗时是两条独立通道——
     * 速度管「每 tick 推进几格」，耗时管「配方有多长」——所以两张卡各写各的行，
     * 同时装时两行都出现。完成一次配方的耗电是功率与耗时的乘积，玩家自己相乘即可。
     */
    private static void addModifier(List<Component> lines, UpgradeEffect effect) {
        if (effect.speed() != 0) addRunSpeed(lines, effect.speed());
        if (!isOne(effect.duration())) addRunDuration(lines, effect.duration());
        if (!isOne(effect.power())) addRunPower(lines, effect.power());
        if (effect.batch() > 0) addBatchCount(lines, effect.batch());
    }

    /** 卡面专有文本：光合注能、范围、药水等级等非数值维度。 */
    private static void addCardText(List<Component> lines, UpgradeItem item, String path) {
        if (item instanceof LevelupSyn) {
            addPhotosynFe(lines, UpgradeConstants.SYN_PHOTOSYN_FE);
            addPureText(lines, path, 4);

        } else if (item instanceof LevelupRg) {
            addRangeFraction(lines, UpgradeConstants.RG_RANGE_FRACTION);

        } else if (item instanceof LevelupPotion) {
            addAmplifier(lines, UpgradeConstants.POTION_AMPLIFIER);

        } else if (item instanceof LevelupKnow) {
            addPureText(lines, path, 1);
            addPureText(lines, path, 2);

        } else if (item instanceof LevelupBlast || item instanceof LevelupSmoke || item instanceof LevelupIce || item instanceof LevelupMagma || item instanceof LevelupMineral || item instanceof LevelupStream) {
            addPureText(lines, path, 1);
        }
    }

    /** 速度行写整数增量：N 只能是自然数，所以是「运行速度 +2」而不是百分比。 */
    private static void addRunSpeed(List<Component> lines, int add) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.run.speed", formatSignedInt(add))
                .withStyle(ChatFormatting.GOLD));
    }

    /** 耗时行写配方长度的百分比变化：{@code 0.90 → -10%}。 */
    private static void addRunDuration(List<Component> lines, double value) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.run.duration", change(value))
                .withStyle(ChatFormatting.GOLD));
    }

    private static void addRunPower(List<Component> lines, double value) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.run.power", change(value))
                .withStyle(ChatFormatting.GOLD));
    }

    /**
     * 倍率的卡面写法：整数倍率写 {@code xN}，其余写成百分比变化 {@code +20% / -25%}。
     * 数值源恒为 0.05 的整数倍，百分比必然是 5 的整数倍，不做二次取整。
     */
    private static String change(double value) {
        if (Math.abs(value - Math.rint(value)) < EPSILON) {
            return "x" + (long) Math.rint(value);
        }
        long pct = Math.round((value - 1.0) * 100.0);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    /** 多级变体：II / III 档的本地化路径加数字后缀，I 档沿用基础路径。 */
    static String regPathFor(UpgradeItem item) {
        String base = basePath(item);
        return item.variant() > 1 ? base + "_" + item.variant() : base;
    }

    private static String basePath(UpgradeItem item) {
        if (item instanceof LevelupAug) return "augmented_levelup";
        if (item instanceof LevelupPower) return "powered_levelup";
        if (item instanceof LevelupShulker) return "relic_levelup";
        if (item instanceof LevelupSyn) return "photosyn_levelup";
        if (item instanceof LevelupRg) return "range_levelup";
        if (item instanceof LevelupBlast) return "blast_levelup";
        if (item instanceof LevelupSmoke) return "smoke_levelup";
        if (item instanceof LevelupPotion) return "potion_levelup";
        if (item instanceof LevelupIce) return "ice_levelup";
        if (item instanceof LevelupMagma) return "magma_levelup";
        if (item instanceof LevelupMineral) return "mineral_levelup";
        if (item instanceof LevelupKnow) return "knowledge_levelup";
        if (item instanceof LevelupStream) return "stream_levelup";
        if (item instanceof LevelupEfficiency) return "efficiency_levelup";
        if (item instanceof LevelupParallel) return "parallel_levelup";
        if (item instanceof LevelupDuration) return "duration_levelup";
        if (item instanceof LevelupSpeed) return "speed_levelup";
        return item.getClass().getSimpleName();
    }

    private static boolean isOne(double value) {
        return Math.abs(value - 1.0) < EPSILON;
    }

    private static String decimal(double value) {
        String raw = String.format(Locale.ROOT, "%.2f", value);
        if (raw.contains(".")) {
            raw = raw.replaceAll("0*$", "").replaceAll("\\.$", "");
        }
        return raw;
    }

    public static String formatSignedInt(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private static void addTitle(List<Component> lines, String path) {
        MutableComponent title = Component.translatable(TEN.MOD_ID + "." + path + ".0")
                .withStyle(ChatFormatting.GOLD)
                .withStyle(ChatFormatting.BOLD);
        lines.add(title);
    }

    private static void addBatchCount(List<Component> lines, int count) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.batch.add", formatSignedInt(count))
                .withStyle(ChatFormatting.GOLD));
    }

    private static void addPhotosynFe(List<Component> lines, int fe) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.photosyn.fe", formatSignedInt(fe))
                .withStyle(ChatFormatting.GOLD));
    }

    private static void addRangeFraction(List<Component> lines, double fraction) {
        int pct = (int) Math.round(fraction * 100.0);
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.range.add", formatSignedInt(pct))
                .withStyle(ChatFormatting.GOLD));
    }

    private static void addAmplifier(List<Component> lines, int amp) {
        lines.add(Component.translatable(TEN.MOD_ID + ".upgrade_tip.potion.amplifier", formatSignedInt(amp))
                .withStyle(ChatFormatting.GOLD));
    }

    private static void addPureText(List<Component> lines, String path, int index) {
        MutableComponent text = Component.translatable(TEN.MOD_ID + "." + path + "." + index)
                .withStyle(ChatFormatting.GOLD);
        lines.add(text);
    }
}
