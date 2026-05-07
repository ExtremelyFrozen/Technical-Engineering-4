package com.modularmc.ten.core.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class RawStorageBlock extends TENBaseBlock {

    public RawStorageBlock(double hs) {
        super(createProperties(hs, hs, MapColor.STONE, SoundType.STONE, 0, true));
    }
}
