package com.modularmc.ten.api.blockentity;

/**
 * 机器 UI 槽位布局描述：i1/i2 物品输入槽起止、o1/o2 物品输出槽起止、
 * fi1/fi2 输入流体罐起止、fo1/fo2 输出流体罐起止（索引含端点）。
 */
public record SlotInfo(int i1, int i2, int o1, int o2,
                       int fi1, int fi2, int fo1, int fo2) {}
