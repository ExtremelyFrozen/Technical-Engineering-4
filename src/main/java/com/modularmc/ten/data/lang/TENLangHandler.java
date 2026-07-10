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
 *   <li>{@link #ZH_ENTRIES} → Chinese translations (consumed by DataGenerators)</li>
 *   <li>{@link #EN_ENTRIES} → English translations (consumed by DataGenerators)</li>
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
        add("jei_addition_chance", "Additional Chance: ", "副产概率: ");
        add("energy_capacity.charging_on", "Charging enabled", "全身充能已开启");
        add("energy_capacity.charging_off", "Charging disabled", "全身充能已关闭");
        add("key.categories." + TEN.MOD_ID, "Kenergy Engineering", "Kenergy Engineering");
        add("key." + TEN.MOD_ID + ".toggle_charge", "Toggle Energy Unit Charging", "切换能量单元充能");
    }

    // ── Channel ─────────────────────────────────────────────────────
    private static void addChannel() {
        add("channel.pos", "Position: ", "坐标: ");
        add("channel.in", "Input Channel", "输入频道");
        add("channel.out", "Output Channel", "输出频道");
        add("channel_connector.0", "When in [Add Input Channel] Mode: ", "处于[添加抽取频道]模式时: ");
        add("channel_connector.1", "Add the second clicked channel to the first one as an input", "在第一次选取的频道中添加第二次选取的频道作为输入");
        add("channel_connector.2", "When in [Add Output Channel] Mode: ", "处于[添加推送频道]模式时: ");
        add("channel_connector.3", "Add the second clicked channel to the first one as an output", "在第一次选取的频道中添加第二次选取的频道作为输出");
        add("channel_connector.4", "Sneak-right-click in air to clear the selected position.", "潜行并在空中右键来清空已选取的坐标。");
        add("channel_connector.mode.in", "Mode: Add Input Channel", "模式: 添加抽取频道");
        add("channel_connector.mode.out", "Mode: Add Output Channel", "模式: 添加推送频道");
        add("channel_connector.mode.rem", "Mode: Remove Channel", "模式: 移除频道");
        add("channel.pointer_last", "Adding to: ", "添加至: ");
        add("channel.bind", "Successfully bound ", "成功绑定 ");
        add("channel.remove", "Successfully removed ", "成功移除 ");
        add("channel.to", " to ", " 至 ");
        add("channel.from", " from ", " 从 ");
        add("channel.first_click", "Target: ", "绑定目标: ");
        add("channel", "Channel", "频道");
        add("channel.not_found", "Channel not found at: ", "无法获取的频道，位于: ");
    }

    // ── Upgrade Tips ────────────────────────────────────────────────
    private static void addUpgradeTips() {
        add("augmented_levelup.0", "Machine: +1 upgrade slot\n      +20% throughput", "机器：额外升级槽+1\n      全局性能+20%");
        add("powered_levelup.0", "Machine: +2 upgrade slots\n      +35% throughput", "机器：额外升级槽+2\n      全局性能+35%");
        add("relic_levelup.0", "Machine: +3 upgrade slots\n      +75% throughput", "机器：额外升级槽+3\n      全局性能+75%");
        add("knowledge_levelup.0", "Machine: +Max upgrade slots", "机器：额外升级槽+Max");
        add("stream_levelup.0", "Machine: Infinite FE transfer", "机器：无限 FE 传输");
        add("photosyn_levelup.0", "Machine: +1 upgrade slot\n      -10% throughput", "机器：额外升级槽+1\n      全局性能-10%");
        add("range_levelup.0", "Effect Machine: +50% range", "效益机器：范围+50%");
        add("blast_levelup.0", "Smelter: Blast furnace recipes only\n      Processing speed x2", "冶炼机：限定高炉配方\n      处理速度x2");
        add("smoke_levelup.0", "Smelter: Smoker recipes only\n      Processing speed x2", "冶炼机：限定烟熏配方\n      处理速度x2");
        add("magma_levelup.0", "Quarry: Magma production mode\n      Produces magma blocks & cream", "采矿场：切换为岩浆产出模式\n      产出岩浆块与岩浆膏");
        add("ice_levelup.0", "Quarry: Ice production mode\n      Produces various ice blocks", "采矿场：切换为冰块产出模式\n      产出各类冰块");
        add("mineral_levelup.0", "Quarry: Ore-only mining mode\n      Only mines c:ores blocks", "采矿场：切换为矿物开采模式\n      仅挖掘矿物块");
        add("potion_levelup.0", "Beacon Simulator: Buff level +1", "信标模拟器：药水等级+1");
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
        add("info.psionicant.0", "Explore...", "探索...");
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
        add("adv.root", "Technical Engineering 3", "科能工程3");
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
