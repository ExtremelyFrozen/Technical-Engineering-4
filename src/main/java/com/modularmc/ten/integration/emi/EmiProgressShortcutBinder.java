package com.modularmc.ten.integration.emi;

import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;

import java.util.function.Supplier;

/**
 * EMI 侧的用途快捷键绑定。与 EMI 集成同处 main 源集，只在 EMI 存在、
 * 由 {@code TENEmiPlugin#register} 注册时被加载。
 *
 * <p>
 * LDLib2 已通过 {@code addGenericStackProvider} 把 {@code stackProvider} 事件
 * 派发给鼠标下的元素；返回机器方块物品的 stack interaction 后，EMI 的「显示用途」键
 * 会打开该工作站对应的配方页。
 */
public final class EmiProgressShortcutBinder {

    private EmiProgressShortcutBinder() {}

    public static void bind(UIElement element, Supplier<ItemStack> machineStack) {
        LDLibEMIPlugin.stackProvider(element, () -> {
            ItemStack stack = machineStack.get();
            return stack.isEmpty() ? EmiStackInteraction.EMPTY : new EmiStackInteraction(EmiStack.of(stack));
        });
    }
}
