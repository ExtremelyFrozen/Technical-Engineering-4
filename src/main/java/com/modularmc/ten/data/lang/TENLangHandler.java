package com.modularmc.ten.data.lang;

import com.modularmc.ten.TEN;

import java.util.LinkedHashMap;

/**
 * Central registry for all Chinese (zh_cn) and English (en_us) lang entries.
 * <p>
 * Previously depended on {@code RegistrateLangProvider}. Now entries are populated
 * into static maps consumed by {@link com.modularmc.ten.data.DataGenerators}.
 * <p>
 * The two maps serve:
 * <ul>
 * <li>{@link #ZH_ENTRIES} → Chinese translations (consumed by DataGenerators)</li>
 * <li>{@link #EN_ENTRIES} → English translations (consumed by DataGenerators)</li>
 * </ul>
 */
public final class TENLangHandler {

    /** Chinese translation entries (key → zh_cn). */
    public static final LinkedHashMap<String, String> ZH_ENTRIES = new LinkedHashMap<>();
    /** English translation entries (key → en_us). */
    public static final LinkedHashMap<String, String> EN_ENTRIES = new LinkedHashMap<>();

    // ════════════════════════════════════════════════════════════════
    // Static initialiser — all entries populated at class load
    // ════════════════════════════════════════════════════════════════

    static {
        addDirections();
        addCommon();
        addChannel();
        addUpgradeTips();
        addLevels();
        addSpannerInfo();
        addMachineInfo();
        addAdvancements();
        addJeiEngine();
        addEmi();
    }

    // ── Directions ──────────────────────────────────────────────────
    private static void addDirections() {
        add("dire.down", "Down", "底侧");
        add("dire.up", "Up", "顶侧");
        add("dire.north", "North", "北侧");
        add("dire.south", "South", "南侧");
        add("dire.west", "West", "西侧");
        add("dire.east", "East", "东侧");
        add("dire.front", "Front", "前侧");
        add("dire.back", "Back", "后侧");
        add("dire.left", "Left", "左侧");
        add("dire.right", "Right", "右侧");
    }

    // ── Common ──────────────────────────────────────────────────────
    private static void addCommon() {
        add("cable.0", "Transfer: 1 kFE", "传输能量: 1 kFE");
        add("cable_quartz.0", "Transfer: 10 kFE", "传输能量: 10 kFE");
        add("cable_azure.0", "Transfer: 100 kFE", "传输能量: 100 kFE");
        add("cable_star.0", "Transfer: Infinite FE", "传输能量: 无限 FE");
        add("not_consumed", "Not consumed", "不消耗");
        add("shift", "Press [SHIFT] to see more", "按住 [SHIFT] 查看更多信息");
        add("info.too_much_upgrades", "This machine has too many upgrades!", "这台机器的升级组件太多了！");
        add("info.not_support_upgrade", "This machine does not support this upgrade!", "这台机器不支持这个升级！");
        add("info.upgrade_successfully", " installed successfully.", " 安装成功。");
        add("key.c", "Change Holding Item Mode", "Change Holding Item Mode");
        add("locked_slot", "This slot has not been unlocked yet.", "未解锁槽位");
        add("upgrade_slot", "Upgrade Slot", "升级槽位");
        add("jei_addition_chance", "Additional Chance: %s%%", "副产概率: %s%%");
        add("jei_addition_chance_rolls", "Additional Chance: %s%% × %s rolls", "副产概率: %s%% × %s次");
        add("energy_capacity.charging_on", "Charging enabled", "全身充能已开启");
        add("energy_capacity.charging_off", "Charging disabled", "全身充能已关闭");
        add("key.categories." + TEN.MOD_ID, "Kenergy Engineering", "Kenergy Engineering");
        add("key." + TEN.MOD_ID + ".toggle_charge", "Toggle Energy Unit Charging", "切换能量单元充能");
    }

    // ── Channel ─────────────────────────────────────────────────────
    private static void addChannel() {
        // 末影箱模式 UI（T004）：频道目录 + 创建/接入/退出；T005：删除（仅空频道）
        add("channel.create", "Create", "创建");
        add("channel.leave", "Leave", "退出");
        add("channel.current", "Current: ", "当前频道: ");
        add("channel.delete", "Delete", "删除");
        add("channel.none", "(none)", "(无)");
        // 操作区「当前频道」紧凑显示：未接入状态（可见文本，tooltip 仍用 channel.current+none 全格式）
        add("channel.not_joined", "Not joined", "未接入");
        // Jade 集成：频道接入状态高亮（ChannelJadeProvider tooltip）
        add("channel.jade.joined", "Joined: %s", "已接入: %s");
        add("channel.jade.not_joined", "Not joined to a channel", "未接入频道");
        add("channel", "Channel", "频道");

        // 频道连接器（ChannelConnectorItem，配置复制/应用工具）
        add("channel_connector.copied", "Copied channel configuration", "已复制频道面配置");
        add("channel_connector.applied", "Applied channel configuration", "已应用频道面配置");
        add("channel_connector.applied_join_failed", "Face config applied, but failed to join channel", "面配置已应用，但频道接入失败");
        add("channel_connector.cleared", "Cleared channel configuration", "已清空频道面配置");
        add("channel_connector.invalid_config", "Invalid channel configuration data", "频道面配置数据无效");
        add("channel_connector.0", "Sneak + right-click a channel to copy its face config", "潜行+右键频道方块复制其面配置");
        add("channel_connector.1", "Sneak + right-click a channel again to apply", "再次潜行+右键频道方块应用配置");
        add("channel_connector.2", "Sneak + right-click air or a non-channel block to clear", "潜行+右键空气或非频道方块清空配置");
    }

    // ── Upgrade Tips ────────────────────────────────────────────────
    private static void addUpgradeTips() {
        // ── Generic format keys (shared across upgrades, consumed by UpgradeTooltipFormatter) ──
        // Duration as signed percent change: arg = "-25", "+50", etc.
        add("upgrade_tip.duration.percent", "%s%% duration", "%s%%消耗时间");
        // Power multiplier: arg = "1.3", "2", "0.8"
        add("upgrade_tip.power.multiplier", "x%s energy consumption", "x%s能量消耗");
        // Batch additive count: arg = "+1", "+3" (pre-formatted signed string)
        add("upgrade_tip.batch.add", "%s batch.", "%s批处理。");
        // Photosynthetic FE/t: arg = "+10"
        add("upgrade_tip.photosyn.fe", "%s FE/t photosynthetic power.", "%s FE/t光合供能。");
        // Range additive fraction: arg = "+50" (pre-formatted signed string)
        add("upgrade_tip.range.add", "%s%% initial range", "%s%%初始作用范围");
        // Potion amplifier: arg = "+1" (pre-formatted signed string)
        add("upgrade_tip.potion.amplifier", "%s potion effect level.", "%s药水效果等级。");
        // Unknown upgrade fallback
        add("upgrade_tip.unknown.0", "Unknown upgrade", "未知升级组件");

        // ── Item-specific keys (title .0 and pure-text lines) ──

        // 1 Augmented: title only (values via generic keys)
        add("augmented_levelup.0", "In machines:", "在机器中：");

        // 2 Powered: title only
        add("powered_levelup.0", "In machines:", "在机器中：");

        // 3 Shulker: title only
        add("relic_levelup.0", "In machines:", "在机器中：");

        // 4 Syn: title + pure text lines .4 (sky) and .5 (unique)
        add("photosyn_levelup.0", "In machines:", "在机器中：");
        add("photosyn_levelup.4", "Only generates in open sky, no rain, daytime", "仅在露天、无雨且昼间时供能");
        add("photosyn_levelup.5", "Only 1 per machine", "每台机器仅可安装1个");

        // 5 Range: title only
        add("range_levelup.0", "In effect machines:", "在功能性机器中：");

        // 6 Blast: enables Blast Furnace recipes (mutually exclusive with Smoke)
        add("blast_levelup.0", "In smelter:", "在熔炼机中：");
        add("blast_levelup.1", "Enables Blast Furnace smelting (exclusive with Smoke)", "启用高炉冶炼（与烟熏互斥）");

        // 7 Smoke: enables Smoker recipes (mutually exclusive with Blast)
        add("smoke_levelup.0", "In smelter:", "在熔炼机中：");
        add("smoke_levelup.1", "Enables Smoker smelting (exclusive with Blast)", "启用烟熏冶炼（与高炉互斥）");

        // 8 Potion: title only
        add("potion_levelup.0", "In beacon simulator:", "在模拟信标中：");

        // 9 Ice: quarry output mode → ice
        add("ice_levelup.0", "In quarry:", "在采矿场中：");
        add("ice_levelup.1", "Output mode: Ice", "产出模式修改为冰块");

        // 10 Magma: quarry output mode → magma
        add("magma_levelup.0", "In quarry:", "在采矿场中：");
        add("magma_levelup.1", "Output mode: Magma", "产出模式修改为岩浆");

        // 11 Mineral: quarry mining mode → mineral
        add("mineral_levelup.0", "In quarry:", "在采矿场中：");
        add("mineral_levelup.1", "Mining mode: Mineral", "开采模式修改为矿物");

        // 12 Knowledge: every 10 recipe ticks → 1 mB XP fluid
        add("knowledge_levelup.0", "In smelter:", "在熔炼机中：");
        add("knowledge_levelup.1", "Each recipe produces XP fluid based on cooking time", "每次处理配方按熔炼时间产出经验流体");
        add("knowledge_levelup.2", "10 ticks → 1 mB Liquid XP (per unit)", "每10 tick产1 mB液态经验（每单位）");

        // 13 Stream: unlimited energy transfer
        add("stream_levelup.0", "In machines:", "在机器中：");
        add("stream_levelup.1", "Energy transfer rate: Infinite", "能量传输速率修改为无限");
    }

    // ── Levels ──────────────────────────────────────────────────────
    private static void addLevels() {
        add("level.0", " (Common)", " (普通)");
        add("level.1", " (Hard)", " (坚硬)");
        add("level.2", " (Rapid)", " (迅捷)");
        add("level.3", " (Powered)", " (充能)");
        add("level.4", " (Shining)", " (闪耀)");
        add("level.5", " (Strong)", " (强力)");
        add("level.6", " (Top)", " (顶级)");
    }

    // ── Spanner Info ────────────────────────────────────────────────
    private static void addSpannerInfo() {
        add("info.spanner.dire.energy", "Energy Transfer: ", "能量传输: ");
        add("info.spanner.dire.item", "Item Transfer: ", "物品传输: ");
        add("info.spanner.dire.redstone", "Redstone Mode: ", "红石模式: ");
        add("info.spanner.work_radius", "Work Radius: ", "工作半径: ");
        add("info.spanner.bind_pos", "Bound Position: ", "绑定坐标: ");
        add("info.spanner.mode", "Mode: ", "模式:");
        add("info.mode.0", "Energy", "能量");
        add("info.mode.1", "Item", "物品");
        add("info.mode.2", "Redstone", "红石");
        add("info.mode.3", "Binding", "绑定");
        add("info.mode.4", "Destroy", "拆除");
        add("spanner.0", "Right-click a machine to rotate it", "右键机器旋转");
        add("spanner.1", "Sneak + right-click a machine to dismantle it", "潜行+右键机器拆卸");
    }

    // ── Machine Info ────────────────────────────────────────────────
    private static void addMachineInfo() {
        add("info.front", "Front", "前侧");
        add("info.back", "Back", "后侧");
        add("info.left", "Left", "左侧");
        add("info.right", "Right", "右侧");
        add("info.up", "Up", "顶侧");
        add("info.down", "Down", "底侧");
        add("info.energy", "Energy", "能量");
        add("info.item", "Item", "物品");
        add("info.fluid", "Fluid", "流体");
        add("info.bar_mode", "Mode: ", "传输模式: ");
        add("info.in", "Active Input", "主动输入");
        add("info.out", "Active Output", "主动输出");
        add("info.be_in", "Passive Input", "被动输入");
        add("info.be_out", "Passive Output", "被动输出");
        add("info.both", "Passive Both-Side", "被动双向");
        add("info.none", "None", "无");
        add("info.off", "Off", "禁用");
        add("info.low", "Low", "低电平");
        add("info.high", "High", "高电平");
        add("info.bar_ideas", "Information: ", "信息: ");
        add("info.bar_redstone", "Redstone Control: ", "红石控制: ");
        add("info.bar_control", "Transfer Control: ", "传输配置: ");
        add("info.bar_energy", "Power: ", "功率:");
        add("info.bar_upgrade", "Upgrades: ", "升级: ");
        add("info.bar_energy_fact", "Actual Power: ", "实际功率: ");
        add("info.bar_energy_max", "Maximum Power: ", "最大功率: ");
        add("info.bar_energy_in_max", "Maximum Energy Input: ", "最大能量输入功率: ");
        add("info.bar_energy_out_max", "Maximum Energy Output: ", "最大能量输出功率: ");
        add("info.bar_item_in_max", "Maximum Item Input: ", "最大物品输入功率: ");
        add("info.bar_item_out_max", "Maximum Item Output: ", "最大物品输出功率: ");
        add("info.bar_fluid_in_max", "Maximum Fluid Input: ", "最大流体输入功率: ");
        add("info.bar_fluid_out_max", "Maximum Fluid Output: ", "最大流体输出功率: ");
        add("info.smelter.0", "Turns energy into heat.", "使用能量加热，");
        add("info.smelter.1", "Provides higher speed than a Furnace.", "提供比熔炉更快的速度。");
        add("info.smelter.2", "The more energy it stores, the faster it works.", "储存的能量越多，熔炼速度越快。");
        add("info.pulverizer.0", "Crush ores into powder.", "将矿物打成粉末来增产。");
        add("info.pulverizer.1", "Classic tech-mod gameplay.", "科技模组经典的玩法 ~");
        add("info.pulverizer.2", "It also has other uses, such as pulverizing blaze rods and bones.", "兼具一些其它功能，例如研磨烈焰棒和骨粉。");
        add("info.pulverizer.3", "Stone can be pulverized into gravel, then into sand and dirt.", "还可以把石头打成砂砾，进而打成沙子和泥土，等等。");
        add("info.compressor.0", "Compresses items into necessary materials.", "压缩物品，制作必需材料。");
        add("info.compressor.1", "Put metal ingots in, then get plates out.", "放进金属锭，收获金属板。");
        add("info.energy_cell.0", "Stores plenty of energy.", "可以存储大量能量。");
        add("info.energy_cell.1", "It can also charge items.", "另外，它还有能力充能物品。");
        add("info.mob_ripper.0", "Attacks mobs in a range.", "对一定范围内的生物造成伤害。");
        add("info.mob_ripper.1", "Consider offering it a Netherite Sword?", "考虑给它一把下界合金剑吗？");
        add("info.beacon_simulator.0", "Applies potion effects to players in an area.", "在一定区域内扩散药水效果。");
        add("info.beacon_simulator.1", "It needs a potion item template.", "需要一个药水物品模板。");
        add("info.beacon_simulator.2", "Its range increases along with the level.", "范围随等级提升而增大。");
        add("info.beacon_simulator.3", "Keep an eye on your energy!", "不过，当心你的能量！");
        add("info.farm_manager.0", "Throw your farm work to it!", "把你的农活都交给它吧！");
        add("info.farm_manager.1", "Give it some seeds and they will be planted automatically.", "放点种子，它们就会自动被种下。");
        add("info.farm_manager.2", "Ripe crops will also be harvested.", "成熟的作物也会被采收。");
        add("info.engine_extraction.0", "A common generator.", "一台普通的发电机，");
        add("info.engine_extraction.1", "Extracts energy from fuel.", "可以从燃料当中提取能量。");
        add("info.engine_metal.0", "Generates energy from metal.", "使用金属发电。");
        add("info.engine_metal.1", "Throw all useless ingots into it now!", "现在把所有没用的锭扔进去吧！");
        add("info.engine_metal.2", "Besides, Netherite generates the most energy.", "还有，下界合金发电是最多的，");
        add("info.engine_metal.3", "But who would do that?", "（但是谁会这么干呢？）");
        add("info.engine_biomass.0", "Reuses biomass energy.", "把生物能重利用。");
        add("info.engine_biomass.1", "Leaves, logs, plants, and more all work!", "无论树叶，树干，还是其它植物都可以！");
        add("info.engine_solar.0", "Stores solar energy with photosynthesis.", "用光合作用存储太阳能。");
        add("info.engine_solar.1", "It must be placed under the sun on clear days.", "必须放在太阳下，而且是晴天。");
        add("info.quarry.0", "Automatically digs all blocks below it.", "自动挖掘它下方的所有方块。");
        add("info.quarry.1", "It needs a pickaxe!", "需要一把镐子！");
        add("info.quarry.2", "Pay attention to your underground builds.", "注意你的地下工事。");
        add("info.psionicant.0", "Transforms specific pairs of materials into new items.",
                "将特定的两种材料转化为新的物品。");
        add("info.psionicant.1", "Each recipe requires its own material pairing.",
                "每种产物都需要对应的材料组合。");
        add("info.induction_furnace.0", "The Induction Furnace can be used to make alloys.", "感应炉可以用来制作合金。");
        add("info.induction_furnace.1", "It produces more ingots than hand crafting.", "比你手工做的产量高！");
        add("info.enchantment_flusher.0", "Want to recycle enchantments from items?", "想回收物品上的附魔吗？");
        add("info.enchantment_flusher.1", "There is an easy way: the Enchantment Flusher.", "这有个简单的方法！用祛魔机。");
        add("info.enchantment_flusher.2", "Put in a book or a tool, then just wait.", "放本书，或者工具，然后只要等着就行了。");
        add("info.refiner.0", "Refines fluids into advanced materials.", "精炼流体以产出更高级的材料。");
        add("info.refiner.1", "If you want to progress further, this is essential.", "想在科技上走得更远，这是必不可少的。");
        add("info.matter_condenser.0", "The Matter Condenser can condense Bizarrerie.", "物质结晶器可以凝聚出奇异物质。");
        add("info.matter_condenser.1", "Insert catalysts to increase processing speed.", "放入各种催化剂提高凝聚速度。");
        add("info.matter_condenser.2", "Where do those even come from?", "这些玩意从哪来的？");
        add("info.cell.0", "Stores plenty of energy.", "可以存储大量能量。");
        add("info.cell.1", "It can also charge items.", "另外，它还有能力充能物品。");
    }

    // ── Advancements ────────────────────────────────────────────────
    private static void addAdvancements() {
        add("adv.root", "Kenergy Engineering: Retechnicalized", "科能工程:再技术化");
        add("adv.root.0", "And the dream begins.", "梦开始的地方");
        add("adv.copper", "Isn't that copper?", "这不是铜吗？");
        add("adv.copper.0", "Get a copper ore from caves.", "从山洞里搞个铜矿石。");
        add("adv.engine", "Fire Energy", "火力发电");
        add("adv.engine.0", "Craft an Extraction Engine.", "制作萃取发电机。");
        add("adv.compress", "Do not put your hand in there!", "别把手伸里头！");
        add("adv.compress.0", "Craft a Compressor.", "制作压缩机。");
        add("adv.crush", "Double Ores", "双倍矿产");
        add("adv.crush.0", "Craft a Pulverizer and get doubled ores.", "制作粉碎机，然后获得双倍矿物。");
        add("adv.cable", "Where will it go?", "它要去哪？");
        add("adv.cable.0", "Craft some Glass Energy Cables.", "制作一点玻璃能量线缆。");
        add("adv.energy_cell", "Where it will go.", "它要去那。");
        add("adv.energy_cell.0", "Craft an Energy Cell to store energy.", "制作一个玻璃能量单元来储存能量。");
        add("adv.span", "Machine Engineer", "机械工程师");
        add("adv.span.0", "Craft a Spanner to configure machines.", "扳手，它会帮助你调配机器的。");
        add("adv.relic", "Present and Past", "过去与现在");
        add("adv.relic.0", "Get an Upgrade: Shulker Kit, wherever it may be.", "得到一个 升级：潜影组件，无论在哪。");
        add("adv.bizarrerie", "UU?", "UU?");
        add("adv.bizarrerie.0", "Create a Bizarrerie with the Psionicant.", "用灵能处理器制作一个奇异物质。");
        add("adv.psionic", "It Is Not Scientific", "这不科学");
        add("adv.psionic.0", "Craft a Psionicant.", "制作一个灵能处理器。");
    }

    // ── JEI Engine Fuel Tooltips ─────────────────────────────────────
    private static void addJeiEngine() {
        add("jei.base_output", "Base Output: %s FE", "基础产出: %s FE");
        add("jei.base_rate", "Base Rate: %s FE/t", "基础功率: %s FE/t");
        add("jei.duration_ticks", "Duration: %s ticks", "持续时间: %s tick");
        add("jei.total_energy", "Total Energy: %s FE", "总能量: %s FE");
        add("jei.base_rate_short", "%s FE/t", "%s FE/t");
        add("jei.duration_short", "%s ticks", "%s tick");
        add("jei.total_short", "%s FE", "%s FE");

        // P3: Smelter three JEI category titles
        add("jei.category.smelter_smelting", "Smelter — Smelting", "熔炼机 — 熔炉");
        add("jei.category.smelter_blasting", "Smelter — Blasting", "熔炼机 — 高炉");
        add("jei.category.smelter_smoking", "Smelter — Smoking", "熔炼机 — 烟熏");
    }

    // ── EMI ─────────────────────────────────────────────────────────
    private static void addEmi() {
        add("emi.category.kenergyengineering.pulverizer", "Pulverizer", "粉碎机");
        add("emi.category.kenergyengineering.compressor", "Compressor", "压缩机");
        add("emi.category.kenergyengineering.refiner", "Refiner", "精炼机");
        add("emi.category.kenergyengineering.induction_furnace", "Induction Furnace", "感应炉");
        add("emi.category.kenergyengineering.psionicant", "Psionicant", "灵能处理器");
    }

    // ════════════════════════════════════════════════════════════════
    // Internal helpers
    // ════════════════════════════════════════════════════════════════

    /** Add a translation entry prefixed with {@code kenergyengineering.}. */
    private static void add(String suffix, String en, String cn) {
        EN_ENTRIES.put(TEN.MOD_ID + "." + suffix, en);
        ZH_ENTRIES.put(TEN.MOD_ID + "." + suffix, cn);
    }

    private TENLangHandler() {}
}
