package com.modularmc.ten.client.model;

import com.modularmc.ten.api.transmission.CableRenderState;
import com.modularmc.ten.api.transmission.ITransmitterProvider;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线缆/管道的动态烘焙模型：按 {@link CableRenderState} 把核心与六个方向的连接部件拼成 quad 列表。
 * <p>
 * 与 AE2 {@code CableBusModel} / Ender IO {@code ConduitBlockModel} 同构——连接组合不进 BlockState，
 * 一个模型按 ModelData 输出；拼装结果按打包值缓存，最多 2 × 3^6 = 1458 项，天然有界。
 * 与通用属性（AO、粒子、渲染层）仍委托给占位核心模型，保证与原模型观感一致。
 */
public class CableBakedModel implements IDynamicBakedModel {

    private final BakedModel delegate;
    private final CableModelParts parts;
    /** key 为 CableRenderState.packed()（低 13 位），value 是该连接组合的全部 quad。 */
    private final Map<Integer, List<BakedQuad>> compositionCache = new ConcurrentHashMap<>();

    public CableBakedModel(BakedModel delegate, CableModelParts parts) {
        this.delegate = delegate;
        this.parts = parts;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                                    ModelData data, net.minecraft.client.renderer.RenderType renderType) {
        CableRenderState render = data.get(CableRenderState.PROPERTY);
        int packed = render == null ? 0 : (render.packed() & 0x1FFF);
        List<BakedQuad> all = compositionCache.computeIfAbsent(packed, this::compose);
        if (side == null) {
            return all;
        }
        List<BakedQuad> facing = new ArrayList<>(all.size());
        for (BakedQuad quad : all) {
            if (quad.getDirection() == side) {
                facing.add(quad);
            }
        }
        return facing;
    }

    /**
     * 直接读 BE 当前值，不依赖 {@code ModelDataManager} 的区块快照。
     * 渲染态由 BE 更新标签送达，客户端先落值再 {@code requestModelDataUpdate()}；
     * 快照按区块批量刷新，与本模型取数不共用一个时序，读快照会有一帧以上的旧外观。
     */
    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        if (level.getBlockEntity(pos) instanceof ITransmitterProvider provider) {
            return ModelData.of(CableRenderState.PROPERTY, new CableRenderState(provider.getRenderPacked()));
        }
        return data;
    }

    private List<BakedQuad> compose(int packed) {
        CableRenderState render = new CableRenderState(packed);
        boolean active = render.active();
        List<BakedQuad> result = new ArrayList<>(parts.core(active));
        for (int i = 0; i < 6; i++) {
            int conn = render.connection(i);
            if (conn == 1) {
                result.addAll(parts.part(active, i));
            } else if (conn == 2) {
                result.addAll(parts.connect(active, i));
            }
        }
        return List.copyOf(result);
    }

    // ─── 以下统一委托占位核心模型 ───

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, net.minecraft.client.renderer.RenderType type) {
        return delegate.useAmbientOcclusion(state, data, type);
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return delegate.getParticleIcon(data);
    }

    @Override
    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return delegate.getOverrides();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return delegate.getRenderTypes(state, rand, data);
    }
}
