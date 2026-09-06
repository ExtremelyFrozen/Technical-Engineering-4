package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.data.TENBlockEntities;
import com.modularmc.ten.client.renderer.RangeDisplayBER;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Client setup handler that initializes client-only subsystems.
 * <p>
 * Must be in the {@code client} source set so it can reference client-only
 * classes without classloading issues on dedicated servers.
 */
@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class TENClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Initialize the client recipe cache that receives RecipesUpdatedEvent
        // from the NeoForge game bus. This cache provides TEN machine recipes
        // to the JEI plugin without requiring an integrated server.
        TENClientRecipeCache.init();

        // 范围显示：为有工作生效范围的 7 台机器注册统一 BER（内部按 rangeVisible
        // 开关与 getRangeBoxes 非空决定是否绘制；无覆盖的机器返回空列表零开销）。
        // BlockEntityEntry extends DeferredHolder：get() 返回 BlockEntityType<T>
        event.enqueueWork(() -> {
            BlockEntityRendererProvider<CmMachineBlockEntity> provider = RangeDisplayBER::new;
            BlockEntityRenderers.register(TENBlockEntities.BEACON.get(), provider);
            BlockEntityRenderers.register(TENBlockEntities.MOB_RIP.get(), provider);
            BlockEntityRenderers.register(TENBlockEntities.FARM.get(), provider);
            BlockEntityRenderers.register(TENBlockEntities.QUARRY.get(), provider);
            BlockEntityRenderers.register(TENBlockEntities.BLOCK_BREAKER.get(), provider);
            BlockEntityRenderers.register(TENBlockEntities.BLOCK_FORMER.get(), provider);
            BlockEntityRenderers.register(TENBlockEntities.COOLER.get(), provider);
        });
    }
}