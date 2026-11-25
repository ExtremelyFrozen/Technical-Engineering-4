package com.modularmc.ten.common.item;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
/**
 使用该类会导致注册的流体桶无法被neoforge识别为流体容器，原因未知，先换用原版的
*/
public class TENBucketItem extends BucketItem {

    public TENBucketItem(Fluid fluid) {
        super(fluid, new Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    }

    public TENBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public String getDescriptionId() {
        return super.getDescriptionId();
    }
}
