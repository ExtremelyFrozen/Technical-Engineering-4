package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.client.guide.TENGuideMeClient;
import com.modularmc.ten.client.renderer.RangeDisplayBER;
import com.modularmc.ten.common.data.TENBlockEntities;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

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
        // 同一次同步也用来扫描同类型内的重复输入签名（兼容层导入邻居数据时的主要回归）
        TENRecipeAudit.init();

        // 配置界面入口：NeoForge 自带的 ConfigurationScreen 靠 IConfigScreenFactory 扩展点被发现，
        // 不注册则 FML 的 Mods 界面与 Mod Menu 都不显示 Config 按钮（两者读同一扩展点）。
        // 注册留在客户端源集：专用服完全不会加载这三个客户端类型。
        ModList.get().getModContainerById(TEN.MOD_ID).ifPresent(container -> container
                .registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new));

        if (ModList.get().isLoaded("guideme")) {
            TENGuideMeClient.init();
        }

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

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // GuideME 的 dispatch 模型只把自家 guideme:item/guide_base 登记进烘焙表，
        // 各册 itemSettings 自带的 itemModel 它不管 → ItemOverrides 现场 getModel() 拿到
        // missing 模型（紫黑方块）。AE2/Powah 都不用这个字段，所以踩不到这条。
        if (ModList.get().isLoaded("guideme")) {
            event.register(new ModelResourceLocation(TENGuideMeClient.GUIDE_ITEM_MODEL, "standalone"));
        }
    }
}