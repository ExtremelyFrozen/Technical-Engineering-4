package com.modularmc.ten;

import net.minecraft.resources.Identifier;

public class TENConstants {

    public static final Identifier GUI_HANDLER = TEN.id("textures/gui/handler.png");
    public static final Identifier JEI_HANDLER_1 = TEN.id("textures/gui/jei_handler1.png");
    public static final Identifier JEI_HANDLER_2 = TEN.id("textures/gui/jei_handler2.png");

    // Modular 素材族（JEI 兼容层与 XEI 共享布局引擎专用）
    public static final Identifier JEI_HANDLER_MODULAR = TEN.id("textures/gui/modular/jei_handler.png"); // 底图：上层
                                                                                                         // 150x50，精炼机下层
                                                                                                         // 170x50
    public static final Identifier ITEM_SLOT_SMALL = TEN.id("textures/gui/modular/item_slot_small.png"); // 18x18
    public static final Identifier FLUID_SLOT = TEN.id("textures/gui/modular/fluid_slot.png"); // 18x50
    public static final Identifier ENERGY_GAUGE_BG = TEN.id("textures/gui/modular/energy_gauge_background.png"); // 14x46
    public static final Identifier ENERGY_GAUGE_FILL = TEN.id("textures/gui/modular/energy_gauge_fill.png"); // 14x46
    public static final Identifier PROGRESS_ARROW_SMELTER_BG = TEN.id("textures/gui/modular/progress_arrow_smelter_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_SMELTER_FILL = TEN.id("textures/gui/modular/progress_arrow_smelter_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PULVERIZER_BG = TEN.id("textures/gui/modular/progress_arrow_pulverizer_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PULVERIZER_FILL = TEN.id("textures/gui/modular/progress_arrow_pulverizer_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_COMPRESSOR_BG = TEN.id("textures/gui/modular/progress_arrow_compressor_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_COMPRESSOR_FILL = TEN.id("textures/gui/modular/progress_arrow_compressor_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_REFINER_BG = TEN.id("textures/gui/modular/progress_arrow_refiner_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_REFINER_FILL = TEN.id("textures/gui/modular/progress_arrow_refiner_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_INDUCTION_FURNACE_BG = TEN.id("textures/gui/modular/progress_arrow_induction_furnace_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_INDUCTION_FURNACE_FILL = TEN.id("textures/gui/modular/progress_arrow_induction_furnace_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PSIONICANT_BG = TEN.id("textures/gui/modular/progress_arrow_psionicant_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PSIONICANT_FILL = TEN.id("textures/gui/modular/progress_arrow_psionicant_fill.png"); // 22x16
    public static final Identifier FUEL_GAUGE_01_BG = TEN.id("textures/gui/modular/fuel_gauge_01_background.png"); // 13x13
    public static final Identifier FUEL_GAUGE_01_FILL = TEN.id("textures/gui/modular/fuel_gauge_01_fill.png"); // 13x13

    public static int WORLD_MIN = -64;
    public static int WORLD_MAX = 256;
}
