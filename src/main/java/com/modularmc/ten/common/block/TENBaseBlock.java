package com.modularmc.ten.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class TENBaseBlock extends Block {

    public static Properties createProperties(double h, double r, MapColor color, SoundType s, int light, boolean solid) {
        var p = BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength((float) h, (float) r)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> light)
                .sound(s);
        if (!solid) p.noOcclusion();
        return p;
    }

    public TENBaseBlock(double h, double r, MapColor color, SoundType s, int light, boolean solid) {
        super(createProperties(h, r, color, s, light, solid));
    }

    public TENBaseBlock(Properties p) {
        super(p);
    }

    @Override
    public String getDescriptionId() {
        return super.getDescriptionId();
    }
}
