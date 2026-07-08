package com.modularmc.ten.common.item;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

public class TENBucketItem extends BucketItem {

    public TENBucketItem(Fluid fluid) {
        super(fluid, new Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    }

    public TENBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

}
