package com.modularmc.ten.common.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class MetalStorageBlock extends TENBaseBlock {

    public MetalStorageBlock(double hs) {
        super(createProperties(hs, hs, MapColor.METAL, SoundType.METAL, 0, true));
    }
}
