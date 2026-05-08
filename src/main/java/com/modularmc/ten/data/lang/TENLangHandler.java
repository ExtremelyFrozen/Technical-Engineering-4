package com.modularmc.ten.data.lang;

import com.modularmc.ten.TEN;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public final class TENLangHandler {

    public static void init(RegistrateLangProvider provider) {
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
        addRaw(provider, "dire.down", "Down");
        addRaw(provider, "dire.up", "Up");
        addRaw(provider, "dire.north", "North");
        addRaw(provider, "dire.south", "South");
        addRaw(provider, "dire.west", "West");
        addRaw(provider, "dire.east", "East");
        addRaw(provider, "dire.front", "Front");
        addRaw(provider, "dire.back", "Back");
        addRaw(provider, "dire.left", "Left");
        addRaw(provider, "dire.right", "Right");
    }

    private static void addCommon(RegistrateLangProvider provider) {
        add(provider, "cable.0", "Transfer: 1 kFE");
        add(provider, "cable_quartz.0", "Transfer: 10 kFE");
        add(provider, "cable_azure.0", "Transfer: 100 kFE");
        add(provider, "cable_star.0", "Transfer: Infinite FE");
        add(provider, "spanner", "Spanner");
        add(provider, "energy_capacity", "FE Capacity");
        add(provider, "shift", "Press [SHIFT] to see more");
        add(provider, "info.too_much_upgrades", "This machine has too many upgrades!");
        add(provider, "info.not_support_upgrade", "This machine does not support this upgrade!");
        add(provider, "info.upgrade_successfully", " installed successfully.");
        add(provider, "key.c", "Change Holding Item Mode");
        add(provider, "locked_slot", "This slot has not been unlocked yet.");
        add(provider, "jei_addition_chance", "Additional Chance: ");
    }

    private static void addChannel(RegistrateLangProvider provider) {
        add(provider, "channel.pos", "Position: ");
        add(provider, "channel.in", "Input Channel");
        add(provider, "channel.out", "Output Channel");
        add(provider, "channel_energy", "Energy Channel");
        add(provider, "channel_item", "Item Channel");
        add(provider, "channel_fluid", "Fluid Channel");
        add(provider, "channel_connector", "Channel Connector");
        add(provider, "channel_connector.0", "When in [Add Input Channel] Mode: ");
        add(provider, "channel_connector.1", "Add the second clicked channel to the first one as an input");
        add(provider, "channel_connector.2", "When in [Add Output Channel] Mode: ");
        add(provider, "channel_connector.3", "Add the second clicked channel to the first one as an output");
        add(provider, "channel_connector.4", "Sneak and press the mode switch key to clear the selected position.");
        add(provider, "channel_connector.mode.in", "Mode: Add Input Channel");
        add(provider, "channel_connector.mode.out", "Mode: Add Output Channel");
        add(provider, "channel_connector.mode.rem", "Mode: Remove Channel");
        add(provider, "channel.pointer_last", "Adding to: ");
        add(provider, "channel.bind", "Successfully bound ");
        add(provider, "channel.remove", "Successfully removed ");
        add(provider, "channel.to", " to ");
        add(provider, "channel.from", " from ");
        add(provider, "channel.first_click", "Target: ");
        add(provider, "channel", "Channel");
        add(provider, "channel.not_found", "Channel not found at: ");
    }

    private static void addUpgradeTips(RegistrateLangProvider provider) {
        add(provider, "augmented_levelup.0", "Machine: Extra 1 upgrade slot, 20% global ability improvement.");
        add(provider, "powered_levelup.0", "Machine: Extra 2 upgrade slots, 35% global ability improvement.");
        add(provider, "relic_levelup.0", "Machine: Extra 3 upgrade slots, 75% global ability improvement.");
        add(provider, "range_levelup.0", "Effect machine: 50% range improvement.");
        add(provider, "photosyn_levelup.0", "Machine: Extra 1 upgrade slot, 10% power suppression.");
        add(provider, "smoke_levelup.0", "Smelter: Faster when smelting food.");
        add(provider, "blast_levelup.0", "Smelter: Faster when smelting minerals.");
        add(provider, "potion_levelup.0", "Beacon Simulator: Higher effect level.");
        add(provider, "stream_levelup.0", "Machine: Infinite FE transfer.");
        add(provider, "knowledge_levelup.0", "Machine: Unlocks all upgrade slots.");
        add(provider, "magma_levelup.0", "Quarry: Produces magma blocks and magma cream.");
        add(provider, "ice_levelup.0", "Quarry: Produces various ice blocks.");
        add(provider, "mineral_levelup.0", "Quarry: Only mines ore blocks.");
    }

    private static void addLevels(RegistrateLangProvider provider) {
        add(provider, "level.0", " (Common)");
        add(provider, "level.1", " (Hard)");
        add(provider, "level.2", " (Rapid)");
        add(provider, "level.3", " (Powered)");
        add(provider, "level.4", " (Shining)");
        add(provider, "level.5", " (Strong)");
        add(provider, "level.6", " (Top)");
    }

    private static void addSpannerInfo(RegistrateLangProvider provider) {
        add(provider, "info.spanner.dire.energy", "Energy Transfer: ");
        add(provider, "info.spanner.dire.item", "Item Transfer: ");
        add(provider, "info.spanner.dire.redstone", "Redstone Mode: ");
        add(provider, "info.spanner.work_radius", "Work Radius: ");
        add(provider, "info.spanner.bind_pos", "Bound Position: ");
        add(provider, "info.spanner.mode", "Mode: ");
        add(provider, "info.mode.0", "Energy");
        add(provider, "info.mode.1", "Item");
        add(provider, "info.mode.2", "Redstone");
        add(provider, "info.mode.3", "Binding");
        add(provider, "info.mode.4", "Destroy");
    }

    private static void addMachineInfo(RegistrateLangProvider provider) {
        add(provider, "info.front", "Front");
        add(provider, "info.back", "Back");
        add(provider, "info.left", "Left");
        add(provider, "info.right", "Right");
        add(provider, "info.up", "Up");
        add(provider, "info.down", "Down");
        add(provider, "info.energy", "Energy");
        add(provider, "info.item", "Item");
        add(provider, "info.fluid", "Fluid");
        add(provider, "info.bar_mode", "Mode: ");
        add(provider, "info.in", "Active Input");
        add(provider, "info.out", "Active Output");
        add(provider, "info.be_in", "Passive Input");
        add(provider, "info.be_out", "Passive Output");
        add(provider, "info.both", "Passive Both-Side");
        add(provider, "info.none", "None");
        add(provider, "info.off", "Off");
        add(provider, "info.low", "Low");
        add(provider, "info.high", "High");
        add(provider, "info.bar_ideas", "Information: ");
        add(provider, "info.bar_redstone", "Redstone Control: ");
        add(provider, "info.bar_control", "Transfer Control: ");
        add(provider, "info.bar_energy", "Power: ");
        add(provider, "info.bar_upgrade", "Upgrades: ");
        add(provider, "info.bar_energy_fact", "Actual Power: ");
        add(provider, "info.bar_energy_max", "Maximum Power: ");
        add(provider, "info.bar_energy_in_max", "Maximum Energy Input: ");
        add(provider, "info.bar_energy_out_max", "Maximum Energy Output: ");
        add(provider, "info.bar_item_in_max", "Maximum Item Input: ");
        add(provider, "info.bar_item_out_max", "Maximum Item Output: ");
        add(provider, "info.bar_fluid_in_max", "Maximum Fluid Input: ");
        add(provider, "info.bar_fluid_out_max", "Maximum Fluid Output: ");
        add(provider, "info.smelter.0", "Turns energy into heat.");
        add(provider, "info.smelter.1", "Provides higher speed than a Furnace.");
        add(provider, "info.smelter.2", "The more energy it stores, the faster it works.");
        add(provider, "info.pulverizer.0", "Crush ores into powder.");
        add(provider, "info.pulverizer.1", "Classic tech-mod gameplay.");
        add(provider, "info.pulverizer.2", "It also has other uses, such as pulverizing blaze rods and bones.");
        add(provider, "info.pulverizer.4", "Stone can be pulverized into gravel, then into sand and dirt.");
        add(provider, "info.compressor.0", "Compresses items into necessary materials.");
        add(provider, "info.compressor.1", "Put metal ingots in, then get plates out.");
        add(provider, "info.cell.0", "Stores plenty of energy.");
        add(provider, "info.cell.1", "It can also charge items.");
        add(provider, "info.mob_ripper.0", "Attacks mobs in a range.");
        add(provider, "info.mob_ripper.1", "Consider offering it a Netherite Sword?");
        add(provider, "info.beacon_simulator.0", "Applies potion effects to players in an area.");
        add(provider, "info.beacon_simulator.1", "It needs a potion item template.");
        add(provider, "info.beacon_simulator.2", "Its range increases along with the level.");
        add(provider, "info.beacon_simulator.3", "Keep an eye on your energy!");
        add(provider, "info.farm_manager.0", "Throw your farm work to it!");
        add(provider, "info.farm_manager.1", "Give it some seeds and they will be planted automatically.");
        add(provider, "info.farm_manager.2", "Ripe crops will also be harvested.");
        add(provider, "info.engine_extraction.0", "A common generator.");
        add(provider, "info.engine_extraction.1", "Extracts energy from fuel.");
        add(provider, "info.engine_metal.0", "Generates energy from metal.");
        add(provider, "info.engine_metal.1", "Throw all useless ingots into it now!");
        add(provider, "info.engine_metal.2", "Besides, Netherite generates the most energy.");
        add(provider, "info.engine_metal.3", "But who would do that?");
        add(provider, "info.engine_biomass.0", "Reuses biomass energy.");
        add(provider, "info.engine_biomass.1", "Leaves, logs, plants, and more all work!");
        add(provider, "info.engine_solar.0", "Stores solar energy with photosynthesis.");
        add(provider, "info.engine_solar.1", "It must be placed under the sun on clear days.");
        add(provider, "info.quarry.0", "Automatically digs all blocks below it.");
        add(provider, "info.quarry.1", "It needs a pickaxe!");
        add(provider, "info.quarry.2", "Pay attention to your underground builds.");
        add(provider, "info.psionicant.0", "Explore...");
        add(provider, "info.induction_furnace.0", "The Induction Furnace can be used to make alloys.");
        add(provider, "info.induction_furnace.1", "It produces more ingots than hand crafting.");
        add(provider, "info.enchantment_flusher.0", "Want to recycle enchantments from items?");
        add(provider, "info.enchantment_flusher.1", "There is an easy way: the Enchantment Flusher.");
        add(provider, "info.enchantment_flusher.2", "Put in a book or a tool, then just wait.");
        add(provider, "info.refiner.0", "Refines fluids into advanced materials.");
        add(provider, "info.refiner.1", "If you want to progress further, this is essential.");
        add(provider, "info.matter_condenser.0", "The Matter Condenser can condense Bizarrerie.");
        add(provider, "info.matter_condenser.1", "Insert catalysts to increase processing speed.");
        add(provider, "info.matter_condenser.2", "Where do those even come from?");
    }

    private static void addAdvancements(RegistrateLangProvider provider) {
        add(provider, "adv.root", "Technical Engineering 3");
        add(provider, "adv.root.0", "And the dream begins.");
        add(provider, "adv.copper", "Isn't that copper?");
        add(provider, "adv.copper.0", "Get a copper ore from caves.");
        add(provider, "adv.engine", "Fire Energy");
        add(provider, "adv.engine.0", "Craft an Extraction Engine.");
        add(provider, "adv.compress", "Do not put your hand in there!");
        add(provider, "adv.compress.0", "Craft a Compressor.");
        add(provider, "adv.crush", "Double Ores");
        add(provider, "adv.crush.0", "Craft a Pulverizer and get doubled ores.");
        add(provider, "adv.cable", "Where will it go?");
        add(provider, "adv.cable.0", "Craft some Glass Energy Cables.");
        add(provider, "adv.cell", "Where it will go.");
        add(provider, "adv.cell.0", "Craft an Energy Cell to store energy.");
        add(provider, "adv.span", "Machine Engineer");
        add(provider, "adv.span.0", "Craft a Spanner to configure machines.");
        add(provider, "adv.relic", "Present and Past");
        add(provider, "adv.relic.0", "Get an Upgrade: Shulker Kit, wherever it may be.");
        add(provider, "adv.bizarrerie", "UU?");
        add(provider, "adv.bizarrerie.0", "Create a Bizarrerie with the Psionicant.");
        add(provider, "adv.psionic", "It Is Not Scientific");
        add(provider, "adv.psionic.0", "Craft a Psionicant.");
    }

    private static void addEmi(RegistrateLangProvider provider) {
        addRaw(provider, "emi.category.kenergyengineering.pulverizer", "Pulverizer");
        addRaw(provider, "emi.category.kenergyengineering.compressor", "Compressor");
        addRaw(provider, "emi.category.kenergyengineering.refiner", "Refiner");
        addRaw(provider, "emi.category.kenergyengineering.induction_furnace", "Induction Furnace");
        addRaw(provider, "emi.category.kenergyengineering.psionicant", "Psionicant");
    }

    private static void add(RegistrateLangProvider provider, String suffix, String value) {
        addRaw(provider, TEN.MOD_ID + "." + suffix, value);
    }

    private static void addRaw(RegistrateLangProvider provider, String key, String value) {
        provider.add(key, value);
    }

    private TENLangHandler() {}
}
