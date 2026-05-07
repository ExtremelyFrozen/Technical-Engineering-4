package com.modularmc.ten.common.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

public class TENBucketItem extends BucketItem {

    public TENBucketItem(Fluid fluid) {
        super(fluid, new Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    }

    @Override
    public String getDescriptionId() {
        return ComponentHelper.getKey(SafeOperationHelper.regNameOf(this));
    }
}
