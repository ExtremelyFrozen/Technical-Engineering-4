package com.modularmc.ten.common;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * 机器唤醒事件：数据包/配方重载时让全部待机机器在下一 tick 重评。
 * <p>
 * 待机机器不轮询配方、也没有配方重载的事件通道；这里只做一次静态代际自增，
 * 各机器在待机路径里用一次 int 比较即可感知，不需要维护全局 BE 注册表或依赖长周期探针。
 */
@EventBusSubscriber(modid = TEN.MOD_ID)
public final class MachineWakeEvents {

    private MachineWakeEvents() {}

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        CmMachineBlockEntity.bumpRecipeGeneration();
    }
}
