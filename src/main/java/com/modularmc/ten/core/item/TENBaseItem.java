package com.modularmc.ten.core.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TENBaseItem extends Item {

    public TENBaseItem() {
        this(new Properties().stacksTo(64));
    }

    public TENBaseItem(Properties prp) {
        super(prp);
    }

    @Override
    public String getDescriptionId() {
        return ComponentHelper.getKey(SafeOperationHelper.regNameOf(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<Component> list = new ArrayList<>();
        String path = SafeOperationHelper.regNameOf(this);

        for (int i = 0; true; i++) {
            String k = ComponentHelper.getKey(path + "." + i);
            Component ttc = ComponentHelper.translated(ComponentHelper.GOLD, k);
            if (ttc.getString().equals(k)) break;
            list.add(ttc);
        }

        if (shift()) {
            tooltip.addAll(list);
        } else if (!list.isEmpty()) {
            tooltip.add(ComponentHelper.translated(ComponentHelper.GOLD, "technicalengineering.shift"));
        }
    }

    public static boolean shift() {
        long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        if (window == 0) return false;
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
