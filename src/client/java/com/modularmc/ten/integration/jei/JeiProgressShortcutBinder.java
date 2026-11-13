package com.modularmc.ten.integration.jei;

import com.modularmc.ten.common.gui.MachineProgressShortcut;

import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;

import mezz.jei.api.constants.VanillaTypes;

import java.util.function.Supplier;

/**
 * JEI 侧的用途快捷键绑定。只在 JEI 存在、由 {@code TENJeiPlugin} 注册时被加载，
 * 因此这里的 JEI 类型不会漏进总是加载的 GUI 工厂。
 *
 * <p>LDLib2 的 GUI handler 会把 {@code clickableIngredient} 事件派发给鼠标下的元素；
 * 我们把机器方块物品交给 JEI 的 factory，JEI 自己的「显示用途」键就会以该物品
 * （已注册为机器配方类别的 catalyst）打开用途页。键位由 JEI 配置决定，不在这里写死。
 */
public final class JeiProgressShortcutBinder {

    private JeiProgressShortcutBinder() {}

    public static void bind(UIElement element, Supplier<ItemStack> machineStack) {
        LDLibJEIPlugin.clickableIngredient(element, () -> {
            ItemStack stack = machineStack.get();
            return stack.isEmpty() ? null
                    : LDLibJEIPlugin.createTypedIngredient(VanillaTypes.ITEM_STACK, stack).orElse(null);
        });
    }
}
