package com.modularmc.ten.common.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class OreBlock extends TENBaseBlock {

    public OreBlock(double hs) {
        super(createProperties(hs, hs, MapColor.STONE, SoundType.STONE, 0, true));
    }
}
