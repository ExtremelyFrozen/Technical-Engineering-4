package com.modularmc.ten.api.option;

/**
 * 机器类型常量表：0-3 为形态大类（产能/逐 tick 加工/周期效果/范围型），
 * 10+ 具体机器、20+ 功能方块（破坏器/成型器/冷却器）、30+ 引擎、40+ 储能单元；
 * 供 UI 背景选择与行为分支。
 */
public class MachineType {

    public static final int GENERATOR = 0;
    public static final int MACHINE_PROCESS = 1;
    public static final int MACHINE_EFFECT = 2;
    public static final int MACHINE_RADIUSED = 3;

    public static final int FURNACE = 10;
    public static final int PULVERIZER = 11;
    public static final int COMPRESSOR = 12;
    public static final int REFINER = 13;
    public static final int INDUCTION_FURNACE = 14;
    public static final int PSIONICANT = 15;
    public static final int MATTER_CONDENSER = 16;
    public static final int ENCHANTMENT_FLUSHER = 17;
    public static final int BEACON = 20;
    public static final int MOB_RIPPER = 21;
    public static final int QUARRY = 22;
    public static final int FARM = 23;
    public static final int ENGINE_SOLAR = 30;
    public static final int ENGINE_EXTRACTION = 31;
    public static final int ENGINE_METAL = 32;
    public static final int ENGINE_BIOMASS = 33;
    public static final int CELL = 40;
    public static final int CREATIVE_CELL = 50;

    public static final int BLOCK_BREAKER = 24;
    public static final int BLOCK_FORMER = 25;
    public static final int COOLER = 26;
}
