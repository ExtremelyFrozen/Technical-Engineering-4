package com.modularmc.ten.data.lang;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public final class TENLangHandler {

    public static final java.util.LinkedHashMap<String, String> ZH_ENTRIES = new java.util.LinkedHashMap<>();

    public static void init(RegistrateLangProvider provider) {
        // Bare keys (kenergyengineering.xxx) = same value as item./block. prefixed keys.
        // Registrate generates prefixed keys from .lang() calls but not bare keys.
        TENItems.EN_NAMES.forEach((id, en) -> provider.add(TEN.MOD_ID + "." + id, en));
        TENBlocks.EN_NAMES.forEach((id, en) -> provider.add(TEN.MOD_ID + "." + id, en));

        addDirections(provider);
        addCommon(provider);
        addChannel(provider);
        addUpgradeTips(provider);
        addLevels(provider);
        addSpannerInfo(provider);
        addMachineInfo(provider);
        addAdvancements(provider);
        addEmi(provider);
    }

    private static void addDirections(RegistrateLangProvider provider) {
        addRaw(provider, "dire.down", "Down", "底侧");
        addRaw(provider, "dire.up", "Up", "顶侧");
        addRaw(provider, "dire.north", "North", "北侧");
        addRaw(provider, "dire.south", "South", "南侧");
        addRaw(provider, "dire.west", "West", "西侧");
        addRaw(provider, "dire.east", "East", "东侧");
        addRaw(provider, "dire.front", "Front", "前侧");
        addRaw(provider, "dire.back", "Back", "后侧");
        addRaw(provider, "dire.left", "Left", "左侧");
        addRaw(provider, "dire.right", "Right", "右侧");
    }

    private static void addCommon(RegistrateLangProvider provider) {
        add(provider, "cable.0", "Transfer: 1 kFE", "传输能量: 1 kFE");
        add(provider, "cable_quartz.0", "Transfer: 10 kFE", "传输能量: 10 kFE");
        add(provider, "cable_azure.0", "Transfer: 100 kFE", "传输能量: 100 kFE");
        add(provider, "cable_star.0", "Transfer: Infinite FE", "传输能量: 无限 FE");
        // Removed: spanner — now auto-derived from item registration
        add(provider, "not_consumed", "Not consumed", "不消耗");
        // Removed: energy_capacity — now auto-derived from item registration
        add(provider, "shift", "Press [SHIFT] to see more", "按住 [SHIFT] 查看更多信息");
        add(provider, "info.too_much_upgrades", "This machine has too many upgrades!", "这台机器的升级组件太多了！");
        add(provider, "info.not_support_upgrade", "This machine does not support this upgrade!", "这台机器不支持这个升级！");
        add(provider, "info.upgrade_successfully", " installed successfully.", " 安装成功。");
        add(provider, "key.c", "Change Holding Item Mode", "Change Holding Item Mode");
        add(provider, "locked_slot", "This slot has not been unlocked yet.", "未解锁槽位");
        add(provider, "jei_addition_chance", "Additional Chance: ", "副产概率: ");
        add(provider, "energy_capacity.charging_on", "Charging enabled", "全身充能已开启");
        add(provider, "energy_capacity.charging_off", "Charging disabled", "全身充能已关闭");
        add(provider, "key.categories." + TEN.MOD_ID, "Kenergy Engineering", "Kenergy Engineering");
        add(provider, "key." + TEN.MOD_ID + ".toggle_charge", "Toggle Energy Unit Charging", "切换能量单元充能");
    }

    private static void addChannel(RegistrateLangProvider provider) {
        // 末影箱模式 UI：频道目录 + 创建/接入/退出；删除（仅空频道）
        add(provider, "channel.create", "Create", "创建");
        add(provider, "channel.leave", "Leave", "退出");
        add(provider, "channel.current", "Current: ", "当前频道: ");
        add(provider, "channel.delete", "Delete", "删除");
        add(provider, "channel.none", "(none)", "(无)");
        // 操作区「当前频道」紧凑显示
        add(provider, "channel.not_joined", "Not joined", "未接入");
        // Jade 集成：频道接入状态高亮
        add(provider, "channel.jade.joined", "Joined: %s", "已接入: %s");
        add(provider, "channel.jade.not_joined", "Not joined to a channel", "未接入频道");
        // Jade 集成：管道过滤模式（PipeJadeProvider）
        add(provider, "pipe.filter.whitelist", "Whitelist", "白名单");
        add(provider, "pipe.filter.blacklist", "Blacklist", "黑名单");
        add(provider, "channel", "Channel", "频道");

        // 频道连接器（ChannelConnectorItem，配置复制/应用工具）
        add(provider, "channel_connector.copied", "Copied channel configuration", "已复制频道面配置");
        add(provider, "channel_connector.applied", "Applied channel configuration", "已应用频道面配置");
        add(provider, "channel_connector.applied_join_failed", "Face config applied, but failed to join channel", "面配置已应用，但频道接入失败");
        add(provider, "channel_connector.cleared", "Cleared channel configuration", "已清空频道面配置");
        add(provider, "channel_connector.invalid_config", "Invalid channel configuration data", "频道面配置数据无效");
        add(provider, "channel_connector.0", "Sneak + right-click a channel to copy its face config", "潜行+右键频道方块复制其面配置");
        add(provider, "channel_connector.1", "Sneak + right-click a channel again to apply", "再次潜行+右键频道方块应用配置");
        add(provider, "channel_connector.2", "Sneak + right-click air or a non-channel block to clear", "潜行+右键空气或非频道方块清空配置");
    }

    private static void addUpgradeTips(RegistrateLangProvider provider) {
        // === 通用升级（槽位+百分比） ===
        add(provider, "augmented_levelup.0",
                "Machine: +1 upgrade slot\n      +20% throughput",
                "机器：额外升级槽+1\n      全局性能+20%");
        add(provider, "powered_levelup.0",
                "Machine: +2 upgrade slots\n      +35% throughput",
                "机器：额外升级槽+2\n      全局性能+35%");
        add(provider, "relic_levelup.0",
                "Machine: +3 upgrade slots\n      +75% throughput",
                "机器：额外升级槽+3\n      全局性能+75%");
        add(provider, "knowledge_levelup.0",
                "Machine: +Max upgrade slots",
                "机器：额外升级槽+Max");
        add(provider, "stream_levelup.0",
                "Machine: Infinite FE transfer",
                "机器：无限 FE 传输");
        add(provider, "photosyn_levelup.0",
                "Machine: +1 upgrade slot\n      -10% throughput",
                "机器：额外升级槽+1\n      全局性能-10%");

        // === 范围扩展 ===
        add(provider, "range_levelup.0",
                "Effect Machine: +50% range",
                "效益机器：范围+50%");

        // === 冶炼升级 ===
        add(provider, "blast_levelup.0",
                "Smelter: Blast furnace recipes only\n      Processing speed x2",
                "冶炼机：限定高炉配方\n      处理速度x2");
        add(provider, "smoke_levelup.0",
                "Smelter: Smoker recipes only\n      Processing speed x2",
                "冶炼机：限定烟熏配方\n      处理速度x2");

        // === 采石场升级 ===
        add(provider, "magma_levelup.0",
                "Quarry: Magma production mode\n      Produces magma blocks & cream",
                "采矿场：切换为岩浆产出模式\n      产出岩浆块与岩浆膏");
        add(provider, "ice_levelup.0",
                "Quarry: Ice production mode\n      Produces various ice blocks",
                "采矿场：切换为冰块产出模式\n      产出各类冰块");
        add(provider, "mineral_levelup.0",
                "Quarry: Ore-only mining mode\n      Only mines c:ores blocks",
                "采矿场：切换为矿物开采模式\n      仅挖掘矿物块");

        // === 信标升级 ===
        add(provider, "potion_levelup.0",
                "Beacon Simulator: Buff level +1",
                "信标模拟器：药水等级+1");
    }

    private static void addLevels(RegistrateLangProvider provider) {
        add(provider, "level.0", " (Common)", " (普通)");
        add(provider, "level.1", " (Hard)", " (坚硬)");
        add(provider, "level.2", " (Rapid)", " (迅捷)");
        add(provider, "level.3", " (Powered)", " (充能)");
        add(provider, "level.4", " (Shining)", " (闪耀)");
        add(provider, "level.5", " (Strong)", " (强力)");
        add(provider, "level.6", " (Top)", " (顶级)");
    }

    private static void addSpannerInfo(RegistrateLangProvider provider) {
        add(provider, "info.spanner.dire.energy", "Energy Transfer: ", "能量传输: ");
        add(provider, "info.spanner.dire.item", "Item Transfer: ", "物品传输: ");
        add(provider, "info.spanner.dire.redstone", "Redstone Mode: ", "红石模式: ");
        add(provider, "info.spanner.work_radius", "Work Radius: ", "工作半径: ");
        add(provider, "info.spanner.bind_pos", "Bound Position: ", "绑定坐标: ");
        add(provider, "info.spanner.mode", "Mode: ", "模式:");
        add(provider, "info.mode.0", "Energy", "能量");
        add(provider, "info.mode.1", "Item", "物品");
        add(provider, "info.mode.2", "Redstone", "红石");
        add(provider, "info.mode.3", "Binding", "绑定");
        add(provider, "info.mode.4", "Destroy", "拆除");
    }

    private static void addMachineInfo(RegistrateLangProvider provider) {
        add(provider, "info.front", "Front", "前侧");
        add(provider, "info.back", "Back", "后侧");
        add(provider, "info.left", "Left", "左侧");
        add(provider, "info.right", "Right", "右侧");
        add(provider, "info.up", "Up", "顶侧");
        add(provider, "info.down", "Down", "底侧");
        add(provider, "info.energy", "Energy", "能量");
        add(provider, "info.item", "Item", "物品");
        add(provider, "info.fluid", "Fluid", "流体");
        add(provider, "info.bar_mode", "Mode: ", "传输模式: ");
        add(provider, "info.in", "Active Input", "主动输入");
        add(provider, "info.out", "Active Output", "主动输出");
        add(provider, "info.be_in", "Passive Input", "被动输入");
        add(provider, "info.be_out", "Passive Output", "被动输出");
        add(provider, "info.both", "Passive Both-Side", "被动双向");
        add(provider, "info.none", "None", "无");
        add(provider, "info.off", "Off", "禁用");
        add(provider, "info.low", "Low", "低电平");
        add(provider, "info.high", "High", "高电平");
        add(provider, "info.bar_ideas", "Information: ", "信息: ");
        add(provider, "info.bar_redstone", "Redstone Control: ", "红石控制: ");
        add(provider, "info.bar_control", "Transfer Control: ", "传输配置: ");
        add(provider, "info.bar_energy", "Power: ", "功率:");
        add(provider, "info.bar_upgrade", "Upgrades: ", "升级: ");
        add(provider, "info.bar_energy_fact", "Actual Power: ", "实际功率: ");
        add(provider, "info.bar_energy_max", "Maximum Power: ", "最大功率: ");
        add(provider, "info.bar_energy_in_max", "Maximum Energy Input: ", "最大能量输入功率: ");
        add(provider, "info.bar_energy_out_max", "Maximum Energy Output: ", "最大能量输出功率: ");
        add(provider, "info.bar_item_in_max", "Maximum Item Input: ", "最大物品输入功率: ");
        add(provider, "info.bar_item_out_max", "Maximum Item Output: ", "最大物品输出功率: ");
        add(provider, "info.bar_fluid_in_max", "Maximum Fluid Input: ", "最大流体输入功率: ");
        add(provider, "info.bar_fluid_out_max", "Maximum Fluid Output: ", "最大流体输出功率: ");
        add(provider, "info.smelter.0", "Turns energy into heat.", "使用能量加热，");
        add(provider, "info.smelter.1", "Provides higher speed than a Furnace.", "提供比熔炉更快的速度。");
        add(provider, "info.smelter.2", "The more energy it stores, the faster it works.", "储存的能量越多，熔炼速度越快。");
        add(provider, "info.pulverizer.0", "Crush ores into powder.", "将矿物打成粉末来增产。");
        add(provider, "info.pulverizer.1", "Classic tech-mod gameplay.", "科技模组经典的玩法 ~");
        add(provider, "info.pulverizer.2", "It also has other uses, such as pulverizing blaze rods and bones.", "兼具一些其它功能，例如研磨烈焰棒和骨粉。");
        add(provider, "info.pulverizer.4", "Stone can be pulverized into gravel, then into sand and dirt.", "还可以把石头打成砂砾，进而打成沙子和泥土，等等。");
        add(provider, "info.compressor.0", "Compresses items into necessary materials.", "压缩物品，制作必需材料。");
        add(provider, "info.compressor.1", "Put metal ingots in, then get plates out.", "放进金属锭，收获金属板。");
        add(provider, "info.energy_cell.0", "Stores plenty of energy.", "可以存储大量能量。");
        add(provider, "info.energy_cell.1", "It can also charge items.", "另外，它还有能力充能物品。");
        add(provider, "info.mob_ripper.0", "Attacks mobs in a range.", "对一定范围内的生物造成伤害。");
        add(provider, "info.mob_ripper.1", "Consider offering it a Netherite Sword?", "考虑给它一把下界合金剑吗？");
        add(provider, "info.beacon_simulator.0", "Applies potion effects to players in an area.", "在一定区域内扩散药水效果。");
        add(provider, "info.beacon_simulator.1", "It needs a potion item template.", "需要一个药水物品模板。");
        add(provider, "info.beacon_simulator.2", "Its range increases along with the level.", "范围随等级提升而增大。");
        add(provider, "info.beacon_simulator.3", "Keep an eye on your energy!", "不过，当心你的能量！");
        add(provider, "info.farm_manager.0", "Throw your farm work to it!", "把你的农活都交给它吧！");
        add(provider, "info.farm_manager.1", "Give it some seeds and they will be planted automatically.", "放点种子，它们就会自动被种下。");
        add(provider, "info.farm_manager.2", "Ripe crops will also be harvested.", "成熟的作物也会被采收。");
        add(provider, "info.engine_extraction.0", "A common generator.", "一台普通的发电机，");
        add(provider, "info.engine_extraction.1", "Extracts energy from fuel.", "可以从燃料当中提取能量。");
        add(provider, "info.engine_metal.0", "Generates energy from metal.", "使用金属发电。");
        add(provider, "info.engine_metal.1", "Throw all useless ingots into it now!", "现在把所有没用的锭扔进去吧！");
        add(provider, "info.engine_metal.2", "Besides, Netherite generates the most energy.", "还有，下界合金发电是最多的，");
        add(provider, "info.engine_metal.3", "But who would do that?", "（但是谁会这么干呢？）");
        add(provider, "info.engine_biomass.0", "Reuses biomass energy.", "把生物能重利用。");
        add(provider, "info.engine_biomass.1", "Leaves, logs, plants, and more all work!", "无论树叶，树干，还是其它植物都可以！");
        add(provider, "info.engine_solar.0", "Stores solar energy with photosynthesis.", "用光合作用存储太阳能。");
        add(provider, "info.engine_solar.1", "It must be placed under the sun on clear days.", "必须放在太阳下，而且是晴天。");
        add(provider, "info.quarry.0", "Automatically digs all blocks below it.", "自动挖掘它下方的所有方块。");
        add(provider, "info.quarry.1", "It needs a pickaxe!", "需要一把镐子！");
        add(provider, "info.quarry.2", "Pay attention to your underground builds.", "注意你的地下工事。");
        add(provider, "info.psionicant.0", "Explore...", "探索...");
        add(provider, "info.induction_furnace.0", "The Induction Furnace can be used to make alloys.", "感应炉可以用来制作合金。");
        add(provider, "info.induction_furnace.1", "It produces more ingots than hand crafting.", "比你手工做的产量高！");
        add(provider, "info.enchantment_flusher.0", "Want to recycle enchantments from items?", "想回收物品上的附魔吗？");
        add(provider, "info.enchantment_flusher.1", "There is an easy way: the Enchantment Flusher.", "这有个简单的方法！用祛魔机。");
        add(provider, "info.enchantment_flusher.2", "Put in a book or a tool, then just wait.", "放本书，或者工具，然后只要等着就行了。");
        add(provider, "info.refiner.0", "Refines fluids into advanced materials.", "精炼流体以产出更高级的材料。");
        add(provider, "info.refiner.1", "If you want to progress further, this is essential.", "想在科技上走得更远，这是必不可少的。");
        add(provider, "info.matter_condenser.0", "The Matter Condenser can condense Bizarrerie.", "物质结晶器可以凝聚出奇异物质。");
        add(provider, "info.matter_condenser.1", "Insert catalysts to increase processing speed.", "放入各种催化剂提高凝聚速度。");
        add(provider, "info.matter_condenser.2", "Where do those even come from?", "这些玩意从哪来的？");
        // Legacy cell keys (kept for compatibility)
        add(provider, "info.cell.0", "Stores plenty of energy.", "可以存储大量能量。");
        add(provider, "info.cell.1", "It can also charge items.", "另外，它还有能力充能物品。");
        // P1-1 新机器
        add(provider, "info.block_breaker.0", "Breaks the block in front of it.", "破坏机器正面一格的方块。");
        add(provider, "info.block_breaker.1", "Place a tool in the left slot; it consumes durability.", "将工具放入左侧槽位，破坏会消耗耐久。");
        add(provider, "info.block_former.0", "Places the block in front of it.", "放置机器正面一格的方块。");
        add(provider, "info.block_former.1", "Candidates auto-refill the input slot until depleted.", "候选栏会自动补充输入槽，直到候选耗尽。");
        add(provider, "info.cooler.0", "Reduces the work time of the machine in front.", "减少正面机器的工作耗时。");
        add(provider, "info.cooler.1", "Uses coolant (Ice/Packed Ice/Blue Ice).", "消耗冷却剂（冰/浮冰/蓝冰）。");
        add(provider, "info.coolant_progress", "Coolant left", "剩余冷却剂");
        add(provider, "info.psionicant.1", "Each recipe requires its own material pairing.", "每种配方都需要特定的材料配对。");
        add(provider, "info.pulverizer.3", "Stone can be pulverized into gravel, then into sand and dirt.", "还可以把石头打成砂砾，进而打成沙子和泥土，等等。");
    }

    private static void addAdvancements(RegistrateLangProvider provider) {
        add(provider, "adv.root", "Technical Engineering 3", "科能工程3");
        add(provider, "adv.root.0", "And the dream begins.", "梦开始的地方");
        add(provider, "adv.copper", "Isn't that copper?", "这不是铜吗？");
        add(provider, "adv.copper.0", "Get a copper ore from caves.", "从山洞里搞个铜矿石。");
        add(provider, "adv.engine", "Fire Energy", "火力发电");
        add(provider, "adv.engine.0", "Craft an Extraction Engine.", "制作萃取发电机。");
        add(provider, "adv.compress", "Do not put your hand in there!", "别把手伸里头！");
        add(provider, "adv.compress.0", "Craft a Compressor.", "制作压缩机。");
        add(provider, "adv.crush", "Double Ores", "双倍矿产");
        add(provider, "adv.crush.0", "Craft a Pulverizer and get doubled ores.", "制作粉碎机，然后获得双倍矿物。");
        add(provider, "adv.cable", "Where will it go?", "它要去哪？");
        add(provider, "adv.cable.0", "Craft some Glass Energy Cables.", "制作一点玻璃能量线缆。");
        add(provider, "adv.energy_cell", "Where it will go.", "它要去那。");
        add(provider, "adv.energy_cell.0", "Craft an Energy Cell to store energy.", "制作一个玻璃能量单元来储存能量。");
        add(provider, "adv.span", "Machine Engineer", "机械工程师");
        add(provider, "adv.span.0", "Craft a Spanner to configure machines.", "扳手，它会帮助你调配机器的。");
        add(provider, "adv.relic", "Present and Past", "过去与现在");
        add(provider, "adv.relic.0", "Get an Upgrade: Shulker Kit, wherever it may be.", "得到一个 升级：潜影组件，无论在哪。");
        add(provider, "adv.bizarrerie", "UU?", "UU?");
        add(provider, "adv.bizarrerie.0", "Create a Bizarrerie with the Psionicant.", "用灵能处理器制作一个奇异物质。");
        add(provider, "adv.psionic", "It Is Not Scientific", "这不科学");
        add(provider, "adv.psionic.0", "Craft a Psionicant.", "制作一个灵能处理器。");
    }

    private static void addEmi(RegistrateLangProvider provider) {
        addRaw(provider, "emi.category.kenergyengineering.pulverizer", "Pulverizer", "粉碎机");
        addRaw(provider, "emi.category.kenergyengineering.compressor", "Compressor", "压缩机");
        addRaw(provider, "emi.category.kenergyengineering.refiner", "Refiner", "精炼机");
        addRaw(provider, "emi.category.kenergyengineering.induction_furnace", "Induction Furnace", "感应炉");
        addRaw(provider, "emi.category.kenergyengineering.psionicant", "Psionicant", "灵能处理器");
    }

    private static void add(RegistrateLangProvider provider, String suffix, String en, String cn) {
        addRaw(provider, TEN.MOD_ID + "." + suffix, en, cn);
    }

    private static void addRaw(RegistrateLangProvider provider, String key, String en, String cn) {
        ZH_ENTRIES.put(key, cn);
        provider.add(key, en);
    }

    private TENLangHandler() {}
}
