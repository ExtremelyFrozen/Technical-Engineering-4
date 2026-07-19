package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.resources.Identifier;

/**
 * Material variant definition shared by:
 * <ul>
 * <li>{@link TENItems#registerVariants(String, String, String, Mat...)} — item registration</li>
 * <li>TENRecipeGen — recipe auto-generation</li>
 * </ul>
 */
public enum Mat {

    // id cn ore deep raw ingot nugget block?
    IRON("iron", "铁", false, false, false, true, true, false),
    GOLD("gold", "金", false, false, false, true, true, false),
    COPPER("copper", "铜", false, false, false, true, true, false),
    TIN("tin", "锡", true, true, true, true, true, true),
    NICKEL("nickel", "镍", true, true, true, true, true, true),
    POWERED_TIN("powered_tin", "充能锡", false, false, false, true, true, true),
    CHLORIUM("chlorium", "叶绿", false, false, false, true, true, true),
    MUSHRIUM("mushrium", "蘑菇", false, false, false, true, true, false),
    STARLIGHT("starlight", "星辉", false, false, false, false, false, false),
    NETHERITE("netherite", "下界合金", false, false, false, true, true, false),
    DIAMOND("diamond", "钻石", false, false, false, false, true, false),
    EMERALD("emerald", "绿宝石", false, false, false, false, true, false),
    LAPIS("lapis", "青金石", false, false, false, false, true, false),
    QUARTZ("quartz", "石英", false, false, false, false, true, false),
    AMETHYST("amethyst", "紫水晶", false, false, false, false, false, false),
    REDSTONE("redstone", "红石", false, false, false, false, false, false);

    /** Registry suffix: "tin" → {@code kenergyengineering:tin_ingot}. */
    public final String id;
    /** Chinese name for translation entries. */
    /** Which variant forms are actually registered for this material. */
    private final java.util.BitSet registeredForms = new java.util.BitSet();
    private static final String[] FORM_NAMES = { "dust", "ingot", "nugget", "plate", "gear", "rod", "wire" };

    /** Public read-only accessor for validator and tag provider. */
    public static String[] formNames() {
        return FORM_NAMES.clone();
    }

    /** Mark a form category as registered (called from {@code registerVariants}). */
    public void markRegistered(String category) {
        for (int i = 0; i < FORM_NAMES.length; i++) {
            if (FORM_NAMES[i].equals(category)) {
                registeredForms.set(i);
                return;
            }
        }
    }

    /** Whether a given form category is actually registered as items. */
    public boolean isRegistered(String category) {
        for (int i = 0; i < FORM_NAMES.length; i++) {
            if (FORM_NAMES[i].equals(category)) return registeredForms.get(i);
        }
        return false;
    }

    public final String cn;

    // ── Recipe metadata ──────────────────────────────────────────────

    /** Has overworld ore block registered. */
    public final boolean hasOre;
    /** Has deepslate ore block registered. */
    public final boolean hasDeepOre;
    /** Has raw material item registered. */
    public final boolean hasRaw;
    /** Has ingot item registered (or uses vanilla ingot). */
    public final boolean hasIngot;
    /** Has nugget item registered. */
    public final boolean hasNugget;
    /** Has storage block registered. */
    public final boolean hasBlock;

    Mat(String id, String cn,
        boolean hasOre, boolean hasDeepOre, boolean hasRaw,
        boolean hasIngot, boolean hasNugget, boolean hasBlock) {
        this.id = id;
        this.cn = cn;
        this.hasOre = hasOre;
        this.hasDeepOre = hasDeepOre;
        this.hasRaw = hasRaw;
        this.hasIngot = hasIngot;
        this.hasNugget = hasNugget;
        this.hasBlock = hasBlock;
    }

    // ── Name helpers (moved from TENItems.Mat) ───────────────────────

    /** English name of this material alone, e.g. "Powered Tin". */
    public String englishName() {
        return switch (this) {
            case IRON -> "Iron";
            case GOLD -> "Gold";
            case COPPER -> "Copper";
            case TIN -> "Tin";
            case NICKEL -> "Nickel";
            case POWERED_TIN -> "Powered Tin";
            case CHLORIUM -> "Chlorium";
            case MUSHRIUM -> "Mushrium";
            case STARLIGHT -> "Starlight";
            case NETHERITE -> "Netherite";
            case DIAMOND -> "Diamond";
            case EMERALD -> "Emerald";
            case LAPIS -> "Lapis Lazuli";
            case QUARTZ -> "Nether Quartz";
            case AMETHYST -> "Amethyst";
            case REDSTONE -> "Redstone";
        };
    }

    /** Combined English name: "{material} {category}", e.g. "Tin Ingot". */
    public String englishName(String categoryEn) {
        return englishName() + " " + categoryEn;
    }

    /**
     * The mod item ID for a given suffix.
     * Vanilla materials redirect ingot/nugget to {@code minecraft:} namespace.
     */
    public String itemId(String suffix) {
        if ((this == IRON || this == GOLD || this == COPPER) && suffix.equals("ingot"))
            return "minecraft:" + id + "_" + suffix;
        if ((this == IRON || this == GOLD) && (suffix.equals("nugget") || suffix.equals("raw_nugget")))
            return "minecraft:" + id + "_" + suffix;
        if (this == NETHERITE && suffix.equals("ingot"))
            return "minecraft:netherite_ingot";
        if (this == DIAMOND && suffix.equals("ingot"))
            return "minecraft:diamond";
        if (this == EMERALD && suffix.equals("ingot"))
            return "minecraft:emerald";
        if (this == LAPIS && suffix.equals("ingot"))
            return "minecraft:lapis_lazuli";
        if (this == QUARTZ && suffix.equals("ingot"))
            return "minecraft:quartz";
        if (this == REDSTONE && suffix.equals("ingot"))
            return "minecraft:redstone";
        if (this == AMETHYST && suffix.equals("ingot"))
            return "minecraft:amethyst_shard";
        if (suffix.equals("raw") && hasRaw)
            return TEN.MOD_ID + ":raw_" + id;
        if (suffix.equals("raw_block") && hasRaw)
            return TEN.MOD_ID + ":raw_" + id + "_block";
        if (suffix.startsWith("raw_") && hasRaw)
            return TEN.MOD_ID + ":raw_" + id;
        if (suffix.equals("deep_ore"))
            return TEN.MOD_ID + ":deep_" + id + "_ore";
        return TEN.MOD_ID + ":" + id + "_" + suffix;
    }

    /** Common tag path: "c:ingots/tin". */
    public String tag(String category) {
        return "c:" + category + "/" + id;
    }

    /** Recipe resource location: "kenergyengineering:compressor/tin_plate". */
    public Identifier recipeId(String prefix, String suffix) {
        return TEN.id(prefix + "/" + id + "_" + suffix);
    }

    /** Recipe resource location (single segment): "kenergyengineering:vanilla/mould_gear". */
    public Identifier recipeId(String prefix) {
        return TEN.id(prefix + "/" + id);
    }

    /**
     * Whether this material has a given variant form registered.
     * Mirrors the actual {@code registerVariants()} calls in {@link TENItems}.
     * <p>
     * NOTE: Must be kept in sync with {@code TENItems.registerVariants()} —
     * if a form is added/removed in registration, update this method accordingly.
     */
    public boolean hasForm(String category) {
        return switch (category) {
            case "dust" -> this != REDSTONE;
            case "ingot" -> hasIngot;
            case "nugget" -> hasNugget;
            case "plate" -> this != STARLIGHT;
            case "gear" -> this != STARLIGHT;
            case "rod" -> hasIngot;
            case "wire" -> hasIngot;
            default -> false;
        };
    }

    /**
     * Tag category for compressor source.
     * {@code "ingots"} for ingot-based, {@code "gems"} for gem-based,
     * or {@code null} for direct-item materials (redstone).
     */
    public String compressTagCategory() {
        if (hasIngot) return "ingots";
        return switch (this) {
            case DIAMOND, EMERALD, LAPIS, QUARTZ, AMETHYST -> "gems";
            default -> null;
        };
    }

    /** For compressor recipe: whether the source uses a tag (true) or direct item (false). */
    public boolean compressUsesTag() {
        return compressTagCategory() != null;
    }

    /** For compressor recipe: the source tag or item ID. */
    public String compressSource() {
        String cat = compressTagCategory();
        return cat != null ? tag(cat) : itemId("ingot");
    }

    // ── Raw block pulverizer support ─────────────────────────────

    /**
     * Whether this material has a raw storage block suitable for pulverizer
     * processing. Separate from {@link #hasRaw} (which controls item
     * registration) — vanilla iron/gold/copper have minecraft-namespace
     * raw blocks even though their raw items are not registered by this mod.
     */
    public boolean hasRawBlock() {
        return this == IRON || this == GOLD || this == COPPER || this == TIN || this == NICKEL;
    }

    /**
     * Resource location string for the raw storage block item/block.
     * Vanilla materials use the minecraft namespace; mod materials use
     * the mod namespace.
     */
    public String rawBlockId() {
        if (this == IRON) return "minecraft:raw_iron_block";
        if (this == GOLD) return "minecraft:raw_gold_block";
        if (this == COPPER) return "minecraft:raw_copper_block";
        return TEN.MOD_ID + ":raw_" + id + "_block";
    }

    /**
     * Common tag path for the raw storage block, e.g. "c:storage_blocks/raw_iron".
     */
    public String rawBlockTag() {
        return "c:storage_blocks/raw_" + id;
    }
}
