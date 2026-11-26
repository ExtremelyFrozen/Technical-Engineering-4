package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.transmission.CableRenderState;
import com.modularmc.ten.client.guide.TENGuideMeClient;
import com.modularmc.ten.client.model.CableBakedModel;
import com.modularmc.ten.client.model.CableModelParts;
import com.modularmc.ten.client.renderer.RangeDisplayBER;
import com.modularmc.ten.common.data.TENBlockEntities;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Map;

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
        // 初始化客户端配方缓存，经 NeoForge 事件总线接收服务端同步的机器配方，供 JEI 在无集成服务器时使用。
        TENClientRecipeCache.init();
        // 配方歧义扫描也在客户端配方同步完成后运行。
        TENRecipeAudit.init();

        // 配置界面入口：NeoForge 自带的 ConfigurationScreen 靠 IConfigScreenFactory 扩展点被发现，
        // 不注册则 FML 的 Mods 界面与 Mod Menu 都不显示 Config 按钮（两者读同一扩展点）。
        // 注册留在客户端源集，专用服务器不会加载这些客户端类型。
        ModList.get().getModContainerById(TEN.MOD_ID).ifPresent(container -> container
                .registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new));

        if (ModList.get().isLoaded("guideme")) {
            TENGuideMeClient.init();
        }

        // 范围显示：为范围机器注册统一 BER；rangeVisible 开启且 getRangeBoxes() 非空时绘制，否则不绘制。
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
        // GuideME 的 dispatch 不会自动登记各册 itemSettings 中的 itemModel，
        // 需通过 RegisterAdditional 显式注册，否则只能得到 missing 模型。
        if (ModList.get().isLoaded("guideme")) {
            event.register(new ModelResourceLocation(TENGuideMeClient.GUIDE_ITEM_MODEL, "standalone"));
        }
        // 线缆/管道的连接零件以 standalone 模型登记，动态 BakedModel 按 ModelData 拼装。
        CableModelParts.register(event);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (String name : CableRenderState.VARIANTS) {
            CableModelParts parts = CableModelParts.build(models, name);
            if (parts == null) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getOptional(TEN.id(name)).orElse(null);
            if (block == null) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel original = models.get(key);
                if (original == null || original instanceof CableBakedModel) {
                    continue;
                }
                models.put(key, new CableBakedModel(original, parts));
            }
        }
    }
}
