package com.modularmc.ten.data.lang;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;
import com.modularmc.ten.config.TENConfig;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        addSpannerInfo(provider);
        addMachineInfo(provider);
        addAdvancements(provider);
        addEmi(provider);
        addJei(provider);
        addConfig(provider);
        addGuide(provider);
        verifyTranslationKeys();
    }

    private static void addDirections(RegistrateLangProvider provider) {
        addRaw(provider, "kenergyengineering.dire.down", "Down", "底侧");
        addRaw(provider, "kenergyengineering.dire.up", "Up", "顶侧");
        addRaw(provider, "kenergyengineering.dire.north", "North", "北侧");
        addRaw(provider, "kenergyengineering.dire.south", "South", "南侧");
        addRaw(provider, "kenergyengineering.dire.west", "West", "西侧");
        addRaw(provider, "kenergyengineering.dire.east", "East", "东侧");
        addRaw(provider, "kenergyengineering.dire.front", "Front", "前侧");
        addRaw(provider, "kenergyengineering.dire.back", "Back", "后侧");
        addRaw(provider, "kenergyengineering.dire.left", "Left", "左侧");
        addRaw(provider, "kenergyengineering.dire.right", "Right", "右侧");
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
        add(provider, "key.c", "Change Holding Item Mode", "切换手持物品模式");
        add(provider, "locked_slot", "This slot has not been unlocked yet.", "未解锁槽位");
        add(provider, "energy_capacity.charging_on", "Charging enabled", "全身充能已开启");
        add(provider, "energy_capacity.charging_off", "Charging disabled", "全身充能已关闭");
        // 按键键名由游戏直接查表，不经 MOD_ID 前缀；用 addRaw 而不是 add，否则会多出一层
        // kenergyengineering.，界面上就显示原始键名（4.0.2 的 key.categories.kenergyengineering 即此）。
        addRaw(provider, TEN.KEY_CATEGORY, "Kenergy Engineering: Retechnicalized", "科能工程:再技术化");
        addRaw(provider, TEN.KEY_TOGGLE_CHARGE, "Toggle Energy Unit Charging", "切换能量单元充能");
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
        // Jade 集成：管道自动抽取面（ker 体系连接面单一形态，Jade 辅助显示 PULL 面）
        add(provider, "jade.auto_extract", "Output faces: %s", "输出面：%s");

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
        // 通用格式键（跨升级共享，UpgradeTooltipFormatter 消费）
        add(provider, "upgrade_tip.duration.percent", "%s%% duration", "%s%%消耗时间");
        add(provider, "upgrade_tip.power.multiplier", "x%s energy consumption", "x%s能量消耗");
        add(provider, "upgrade_tip.batch.add", "%s batch.", "%s批处理。");
        add(provider, "upgrade_tip.photosyn.fe", "%s FE/t photosynthetic power.", "%s FE/t光合供能。");
        add(provider, "upgrade_tip.range.add", "%s%% initial range", "%s%%初始作用范围");
        add(provider, "upgrade_tip.potion.amplifier", "%s potion effect level.", "%s药水效果等级。");
        add(provider, "upgrade_tip.unknown.0", "Unknown upgrade", "未知升级组件");
        add(provider, "upgrade_slot", "Upgrade Slot", "升级槽位");

        // === 各升级标题（.0）与纯文本行 ===
        add(provider, "augmented_levelup.0", "In machines:", "在机器中：");
        add(provider, "powered_levelup.0", "In machines:", "在机器中：");
        add(provider, "relic_levelup.0", "In machines:", "在机器中：");
        add(provider, "photosyn_levelup.0", "In machines:", "在机器中：");
        add(provider, "photosyn_levelup.4", "Only generates in open sky, no rain, daytime", "仅在露天、无雨且昼间时供能");
        add(provider, "photosyn_levelup.5", "Only 1 per machine", "每台机器仅可安装1个");
        add(provider, "range_levelup.0", "In effect machines:", "在功能性机器中：");
        add(provider, "blast_levelup.0", "In smelter:", "在熔炼机中：");
        add(provider, "blast_levelup.1", "Enables Blast Furnace smelting (exclusive with Smoke)", "启用高炉冶炼（与烟熏互斥）");
        add(provider, "smoke_levelup.0", "In smelter:", "在熔炼机中：");
        add(provider, "smoke_levelup.1", "Enables Smoker smelting (exclusive with Blast)", "启用烟熏冶炼（与高炉互斥）");
        add(provider, "potion_levelup.0", "In beacon simulator:", "在模拟信标中：");
        add(provider, "ice_levelup.0", "In quarry:", "在采矿场中：");
        add(provider, "ice_levelup.1", "Output mode: Ice", "产出模式修改为冰块");
        add(provider, "magma_levelup.0", "In quarry:", "在采矿场中：");
        add(provider, "magma_levelup.1", "Output mode: Magma", "产出模式修改为岩浆");
        add(provider, "mineral_levelup.0", "In quarry:", "在采矿场中：");
        add(provider, "mineral_levelup.1", "Mining mode: Mineral", "开采模式修改为矿物");
        add(provider, "knowledge_levelup.0", "In smelter:", "在熔炼机中：");
        add(provider, "knowledge_levelup.1", "Each recipe produces XP fluid based on cooking time", "每次处理配方按熔炼时间产出经验流体");
        add(provider, "knowledge_levelup.2", "10 ticks → 1 mB Liquid XP (per unit)", "每10 tick产1 mB液态经验（每单位）");
        add(provider, "stream_levelup.0", "In machines:", "在机器中：");
        add(provider, "stream_levelup.1", "Energy transfer rate: Infinite", "能量传输速率修改为无限");
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
        add(provider, "spanner.0", "Right-click a machine to rotate it", "右键机器旋转");
        add(provider, "spanner.1", "Sneak + right-click a machine to dismantle it", "潜行+右键机器拆卸");
        add(provider, "connection_mode", "Connection mode", "连接模式");
        add(provider, "connection_mode.none", "None", "断开");
        // 设备边（容器视角）：pull=输出（主动从容器抽取）、normal=输入（被动向容器投放）
        add(provider, "connection_mode.pull", "Output", "输出");
        add(provider, "connection_mode.device_normal", "Input", "输入");
        // 管道间边（连接视角）：与设备边分开，避免「输入」误导管道间显示
        add(provider, "connection_mode.pipe_normal", "Connected", "连接");
        add(provider, "connection_mode.push", "Push", "仅输出");
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
        add(provider, "info.bar_energy_use", "Energy Use: ", "当前能量消耗: ");
        add(provider, "info.bar_energy_use_max", "Max Energy Use: ", "最大能量消耗: ");
        // 采石场扫描模式切换（GUI 按钮文案与 tooltip，供后续功能移植）
        add(provider, "info.quarry.scan_mode", "Scan Mode", "扫描模式");
        add(provider, "info.quarry.scan_normal", "Normal", "标准");
        add(provider, "info.quarry.scan_fast", "Fast", "极速");
        add(provider, "info.quarry.scan_normal_tip", "Digs layer by layer; stops automatically when the column is fully mined.", "逐层向下挖掘；整列挖尽后自动停机。");
        add(provider, "info.quarry.scan_fast_tip", "Skips air layers to the nearest solid layer; faster progress.", "跳过空气层直达最近实心层，推进更快。");
        add(provider, "info.quarry.scan_click", "Click to toggle scan mode", "点击切换扫描模式");
        // 范围显示切换按钮（rangeDisplayToggleButton）
        add(provider, "info.range_display", "Range Display", "范围显示");
        add(provider, "info.range_display_on", "Range: On", "范围: 显示");
        add(provider, "info.range_display_off", "Range: Off", "范围: 隐藏");
        add(provider, "info.range_display_on_tip", "Showing the working range outline.", "正在显示工作范围线框。");
        add(provider, "info.range_display_off_tip", "Working range outline is hidden.", "工作范围线框已隐藏。");
        add(provider, "info.range_display_click", "Click to toggle range display", "点击切换范围显示");
        // 应用工具/武器附魔切换按钮（useEnchantmentsToggleButton）
        add(provider, "info.use_enchantments", "Use Enchantments", "应用附魔");
        add(provider, "info.use_enchantments_on", "Enchants: On", "附魔: 开启");
        add(provider, "info.use_enchantments_off", "Enchants: Off", "附魔: 关闭");
        add(provider, "info.use_enchantments_on_tip", "Enchantments are applied.", "应用附魔。");
        add(provider, "info.use_enchantments_off_tip", "Enchantments are ignored.", "忽略附魔。");
        add(provider, "info.use_enchantments_click", "Click to toggle enchantment application", "点击切换附魔应用");
        // Jade 配置界面（Jade 约定 config.jade.plugin_<modid>.<uid>，不加 modid 前缀）
        addRaw(provider, "config.jade.plugin_kenergyengineering.channel_status", "Channel Status", "频道状态");
        addRaw(provider, "config.jade.plugin_kenergyengineering.pipe_status", "Pipe Status", "管道状态");
        addRaw(provider, "config.jade.plugin_kenergyengineering.pipe_pull", "Pipe Output Faces", "管道输出面");
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
        // 新机器信息文案
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
        add(provider, "adv.root", "Kenergy Engineering: Retechnicalized", "科能工程:再技术化");
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
        // 注意：EMI 分类标题查找键为裸键 emi.category.<ns>.<path>（TENRecipeWidget.titleEmi），
        // 不加 kenergyengineering 前缀；26.1.2 生成的双前缀键是其 add() 自动前缀的死键，勿照搬。
        addRaw(provider, "emi.category.kenergyengineering.pulverizer", "Pulverizer", "粉碎机");
        addRaw(provider, "emi.category.kenergyengineering.compressor", "Compressor", "压缩机");
        addRaw(provider, "emi.category.kenergyengineering.refiner", "Refiner", "精炼机");
        addRaw(provider, "emi.category.kenergyengineering.induction_furnace", "Induction Furnace", "感应炉");
        addRaw(provider, "emi.category.kenergyengineering.psionicant", "Psionicant", "灵能处理器");
    }

    private static void addJei(RegistrateLangProvider provider) {
        // JEI 引擎燃料信息
        add(provider, "jei.base_output", "Base Output: %s FE", "基础产出: %s FE");
        add(provider, "jei.base_rate", "Base Rate: %s FE/t", "基础功率: %s FE/t");
        add(provider, "jei.duration_ticks", "Duration: %s ticks", "持续时间: %s tick");
        add(provider, "jei.total_energy", "Total Energy: %s FE", "总能量: %s FE");
        add(provider, "jei.base_rate_short", "%s FE/t", "%s FE/t");
        add(provider, "jei.duration_short", "%s ticks", "%s tick");
        add(provider, "jei.total_short", "%s FE", "%s FE");
        // JEI 冷却剂分类（数值来自 c:coolants 分档标签）
        add(provider, "jei.coolant_interval", "Cooling cycle: %s ticks", "冷却周期: %s tick");
        add(provider, "jei.coolant_reduction", "Progress per cycle: %s ticks", "每周期推进: %s tick");
        add(provider, "jei.coolant_uses", "Uses per item: %s", "每份可用次数: %s");
        add(provider, "jei.coolant_total", "Total progress per item: %s ticks", "每份累计推进: %s tick");
        add(provider, "jei.coolant_lifetime", "One item lasts: %s ticks", "一份可持续: %s tick");
        add(provider, "jei.coolant_interval_short", "cycle %s t", "周期 %s t");
        add(provider, "jei.coolant_reduction_short", "cut %s t", "推进 %s t");
        add(provider, "jei.coolant_uses_short", "%s uses", "可用 %s 次");
        // JEI 冶炼分类标题
        add(provider, "jei.category.smelter_smelting", "Smelter \u2014 Smelting", "熔炼机 \u2014 熔炉");
        add(provider, "jei.category.smelter_blasting", "Smelter \u2014 Blasting", "熔炼机 \u2014 高炉");
        add(provider, "jei.category.smelter_smoking", "Smelter \u2014 Smoking", "熔炼机 \u2014 烟熏");
        // JEI 槽位覆盖（chance/rolls）
        // 位置参数：en_ud 由 Registrate 翻转 en_us 自动生成，翻转后 %s 视觉顺序颠倒，
        // %1$s/%2$s 锚定参数语义，避免「附加几率: 9% × 40 rolls」式错乱
        add(provider, "jei_addition_chance", "Additional Chance: %1$s%%", "副产概率: %s%%");
        add(provider, "jei_addition_chance_rolls", "Additional Chance: %1$s%% × %2$s rolls", "附加几率: %s%% × %s 次");
    }

    private static void add(RegistrateLangProvider provider, String suffix, String en, String cn) {
        addRaw(provider, TEN.MOD_ID + "." + suffix, en, cn);
    }

    private static void addGuide(RegistrateLangProvider provider) {
        addRaw(provider, "item.kenergyengineering.guide", "Kenergy Engineering Guide", "科能工程指南");
        addRaw(provider, "item.kenergyengineering.guide.tooltip", "Open the in-game guide", "打开游戏内指南");
    }

    /**
     * 配置界面的键由 NeoForge 拼：{@code <modid>.configuration.<路径>}（分区/组同理，点分）
     * 与 {@code ...<路径>.tooltip}（没有 tooltip 时回退 TOML 注释原文）。
     * 少一条就在界面上直接显示键名，所以每个 define 项都要有名字，说明按需要补 tooltip。
     */
    private static void addConfig(RegistrateLangProvider provider) {
        addRaw(provider, "kenergyengineering.configuration.title", "Kenergy Engineering Configuration", "科能工程：再技术化 配置");
        addRaw(provider, "kenergyengineering.configuration.section.common", "Common", "通用");
        addRaw(provider, "kenergyengineering.configuration.section.client", "Client", "客户端");

        addRaw(provider, "kenergyengineering.configuration.client", "Client", "客户端");
        addRaw(provider, "kenergyengineering.configuration.client.tooltip",
                "Client-side display options; never affects server behaviour.",
                "客户端显示选项，不影响服务端行为。");
        addRaw(provider, "kenergyengineering.configuration.client.showMachineHUD", "Show machine HUD", "显示机器 HUD");
        addRaw(provider, "kenergyengineering.configuration.client.showMachineHUD.tooltip", "Show the machine information overlay on HUD.", "在 HUD 上显示机器信息栏。");
        addRaw(provider, "kenergyengineering.configuration.client.showCableHUD", "Show cable HUD", "显示管道 HUD");
        addRaw(provider, "kenergyengineering.configuration.client.showCableHUD.tooltip", "Show the cable network statistics overlay on HUD.", "在 HUD 上显示管道的能量传输统计。");

        addRaw(provider, "kenergyengineering.configuration.machine", "Machines", "机器");
        addRaw(provider, "kenergyengineering.configuration.machine.tooltip",
                "Defaults shared by every machine of the same type.",
                "同一类机器共享的默认参数。");
        addRaw(provider, "kenergyengineering.configuration.machine.energyMultiplier", "Energy multiplier", "能量倍率");
        addRaw(provider, "kenergyengineering.configuration.machine.energyMultiplier.tooltip",
                "Global energy multiplier applied to every machine.", "作用于所有机器的全局能量倍率。");
        addRaw(provider, "kenergyengineering.configuration.machine.baseEnergyCapacity", "Base energy capacity", "基础能量容量");
        addRaw(provider, "kenergyengineering.configuration.machine.baseEnergyCapacity.tooltip",
                "Base FE buffer of a machine before upgrades.", "升级前机器自身的能量缓冲上限（FE）。");
        addMachineToggle(provider, "enableSmelter", "Smelter", "熔炼机");
        addMachineToggle(provider, "enablePulverizer", "Pulverizer", "粉碎机");
        addMachineToggle(provider, "enableCompressor", "Compressor", "压缩机");
        addMachineToggle(provider, "enableRefiner", "Refiner", "精炼机");
        addMachineToggle(provider, "enableInductionFurnace", "Induction Furnace", "感应炉");
        addMachineToggle(provider, "enablePsionicant", "Psionicant", "灵能处理器");
        addMachineToggle(provider, "enableBeacon", "Beacon", "信标");
        addMachineToggle(provider, "enableMobRipper", "Mob Ripper", "生物啃噬者");
        addMachineToggle(provider, "enableQuarry", "Quarry", "采矿场");
        addMachineToggle(provider, "enableEnchantmentFlusher", "Enchantment Flusher", "祛魔机");
        addMachineToggle(provider, "enableCondenser", "Matter Condenser", "物质结晶器");
        addMachineToggle(provider, "enableFarmManager", "Farm Manager", "农场管理器");

        addRaw(provider, "kenergyengineering.configuration.energyUnit", "Energy Unit", "能量模块");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.tooltip",
                "Charging and output behaviour of Energy Units.",
                "能量模块的充电与输出行为。");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.maxEnergy", "Maximum energy", "最大能量");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.maxEnergy.tooltip",
                "Energy stored by one Energy Unit (FE).", "单个能量模块可存储的能量上限（FE）。");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.chargeRate", "Charge rate (FE/t)", "充电速率（FE/刻）");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.chargeRate.tooltip",
                "How fast an Energy Unit charges inside a machine.", "能量模块在机器内被充电的速率。");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.inputRate", "Input rate (FE/t)", "输入速率（FE/刻）");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.inputRate.tooltip",
                "Energy accepted per tick from outside.", "每 tick 可从外部接收的能量上限。");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.outputRate", "Output rate (FE/t)", "输出速率（FE/刻）");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.outputRate.tooltip",
                "Energy emitted per tick to outside.", "每 tick 可向外部输出的能量上限。");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.chargingDefault", "Charging mode by default", "默认充电模式");
        addRaw(provider, "kenergyengineering.configuration.energyUnit.chargingDefault.tooltip",
                "Initial mode of a newly crafted Energy Unit.", "新制作的能量模块的初始模式。");

        addRaw(provider, "kenergyengineering.configuration.farm", "Farm Manager", "农场管理器");
        addRaw(provider, "kenergyengineering.configuration.farm.tooltip",
                "Block lists the Farm Manager harvests.",
                "农场管理器识别的作物方块列表。");
        addRaw(provider, "kenergyengineering.configuration.farm.bushCrops", "Bush crop blocks", "灌木状作物");
        addRaw(provider, "kenergyengineering.configuration.farm.bushCrops.tooltip",
                "Block IDs harvested as bushes (multi-tile crops) instead of by age.",
                "按灌木整体收获（而非按生长阶段）的作物方块 id 列表。");

        addRaw(provider, "kenergyengineering.configuration.compat", "Neighbour mod compatibility", "邻居模组兼容");
        addRaw(provider, "kenergyengineering.configuration.compat.tooltip",
                "Which compat sources are active, and which mod provides a shared material.",
                "哪些兼容来源生效，同种材料由哪个模组提供。");
        addRaw(provider, "kenergyengineering.configuration.compat.disabledSources", "Disabled compat sources", "关闭的兼容来源");
        addRaw(provider, "kenergyengineering.configuration.compat.disabledSources.tooltip",
                "Sources whose compat recipes are dropped at load time and hidden in JEI, e.g. enderio_evolution.",
                "列出的来源其兼容配方在加载期整体消失，JEI 也不再显示，例如 enderio_evolution。");
        addRaw(provider, "kenergyengineering.configuration.compat.providerPriority", "Material provider priority", "材料归属优先级");
        addRaw(provider, "kenergyengineering.configuration.compat.providerPriority.tooltip",
                "Best first. Decides which mod owns an item when several mods ship the same material"
                        + " (ore chain, alloys). A switched-off source is skipped here too.",
                "从优到劣排列。同一种材料（矿石链、合金）由列表中最先「已加载且未被关闭」的模组提供。");

        addRaw(provider, "kenergyengineering.configuration.panelAnimation", "Panel Animation", "面板动画");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.tooltip",
                "Expand/collapse animation of the config and upgrade panels.",
                "配置面板与升级面板的展开、收起动画。");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.enabled", "Enable panel animation", "启用面板展开动画");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.enabled.tooltip", "Animate the config & upgrade panels expanding/collapsing. Disable for instant toggle.", "配置面板与升级面板展开/收起时播放动画。关闭后立即展开/收起（无动画）。");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.speed", "Common panel speed (px/tick)", "通用面板速度（像素/刻）");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.speed.tooltip", "Expand/collapse speed shared by all panels. Higher = faster. Per-panel override takes precedence when nonzero.", "所有展开面板共用的展开/收起速度，数值越大越快。面板独立速度非 0 时优先生效。");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.upgradeSpeed", "Upgrade panel speed override (px/tick)", "升级面板独立速度（像素/刻）");
        addRaw(provider, "kenergyengineering.configuration.panelAnimation.upgradeSpeed.tooltip", "Speed override for the upgrade panel only. 0 = use common speed.", "仅升级面板使用的独立展开/收起速度，0 = 使用通用面板速度。");
    }

    /**
     * 校验「游戏直接查表」的两类键：配置项（label + tooltip）与按键绑定（分类 + 键名）。
     * 缺一条就让 datagen 失败，而不是等玩家在界面里看到原始键名。
     * <p>
     * 配置侧遍历 {@link ModConfigSpec#getSpec()} 的树：组节点只要求组名，叶子节点额外要求
     * {@code .tooltip}。按键侧取 {@link TEN#KEY_CATEGORY} / {@link TEN#KEY_TOGGLE_CHARGE}，
     * 与 {@code TENKeybinds} 共用同一份常量。键集合取自 {@link #ZH_ENTRIES}——{@code addRaw}
     * 每写一条都会登记，所以校验的是这次 datagen 实际产出的键，不是另抄一份清单。
     */
    private static void verifyTranslationKeys() {
        List<String> missing = new ArrayList<>();
        collectMissingKeys(TENConfig.COMMON_SPEC.getSpec().valueMap(), "", missing);
        collectMissingKeys(TENConfig.CLIENT_SPEC.getSpec().valueMap(), "", missing);
        for (String key : new String[] {TEN.KEY_CATEGORY, TEN.KEY_TOGGLE_CHARGE}) {
            if (!ZH_ENTRIES.containsKey(key)) missing.add(key);
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("界面键缺少本地化 " + missing.size() + " 条，游戏里会显示原始键名：\n  "
                    + String.join("\n  ", missing)
                    + "\n在 TENLangHandler 对应 addXxx() 里补齐后再跑 datagen。");
        }
    }

    private static void collectMissingKeys(Map<?, ?> specTree, String prefix, List<String> missing) {
        for (Map.Entry<?, ?> entry : specTree.entrySet()) {
            String path = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            String key = TEN.MOD_ID + ".configuration." + path;
            if (!ZH_ENTRIES.containsKey(key)) missing.add(key);
            if (entry.getValue() instanceof Map<?, ?> child) {
                collectMissingKeys(child, path, missing);
            } else if (!ZH_ENTRIES.containsKey(key + ".tooltip")) {
                missing.add(key + ".tooltip");
            }
        }
    }

    /** 机器开关：中英文都跟着方块术语走，避免界面里出现第二套机器名称。 */
    private static void addMachineToggle(RegistrateLangProvider provider, String path, String en, String cn) {
        addRaw(provider, "kenergyengineering.configuration.machine." + path, "Enable the " + en, "启用" + cn);
        addRaw(provider, "kenergyengineering.configuration.machine." + path + ".tooltip",
                "Whether the " + en + " is available.", cn + "是否可用。");
    }

    private static void addRaw(RegistrateLangProvider provider, String key, String en, String cn) {
        ZH_ENTRIES.put(key, cn);
        provider.add(key, en);
    }

    private TENLangHandler() {}
}
