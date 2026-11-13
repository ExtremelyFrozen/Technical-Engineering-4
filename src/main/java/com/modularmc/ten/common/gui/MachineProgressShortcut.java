package com.modularmc.ten.common.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * 机器进度条与 JEI/EMI「显示用途」快捷键之间的桥。
 *
 * <p>
 * 本类只依赖 LDLib2 的 {@link UIElement} 与原版 {@link ItemStack}，不引用任何 XEI 类型；
 * 各集成在插件初始化时注册自己的 {@link Binder}（JEI 在 client 源集、EMI 在 main 源集），
 * 缺邻居时整个包仍可正常加载，专用服务器上绑定表为空。
 *
 * <p>
 * 绑给进度条的 ingredient 是机器方块自身的物品：JEI 的 recipe catalyst 与 EMI 的
 * workstation 都已把该物品登记为对应配方类别的入口，因此按各自的「显示用途」键会打开
 * 这台机器的配方用途页，不需要自己读键位或硬编码类别跳转。
 */
public final class MachineProgressShortcut {

    @FunctionalInterface
    public interface Binder {

        void bind(UIElement element, Supplier<ItemStack> machineStack);
    }

    private static final List<Binder> BINDERS = new CopyOnWriteArrayList<>();

    private MachineProgressShortcut() {}

    /** 由 XEI 插件调用；重复注册（JEI 重载等）用去重避免同一元素挂多份监听器。 */
    public static void register(Binder binder) {
        if (binder != null && !BINDERS.contains(binder)) {
            BINDERS.add(binder);
        }
    }

    /** 给进度条元素挂上所有已安装 XEI 的用途查询入口。 */
    public static void bind(UIElement element, CmMachineBlockEntity machine) {
        if (element == null || machine == null) return;
        Supplier<ItemStack> stack = () -> machineStack(machine);
        for (Binder binder : BINDERS) {
            binder.bind(element, stack);
        }
    }

    /** 机器方块自身对应的物品；没有方块物品的机器返回空栈。 */
    public static ItemStack machineStack(CmMachineBlockEntity machine) {
        var item = machine.getBlockState().getBlock().asItem();
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
