package com.modularmc.ten.client.model;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.transmission.CableRenderState;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 线缆/管道动态模型的零件库。
 * <p>
 * core/part/connect（及线缆的 active 版）本来就是 {@code src/main/resources} 下的独立模型，
 * 原先靠 blockstate 的 26 条 multipart 条件按方向旋转拼装。这里在 {@code RegisterAdditional}
 * 把它们登记成 standalone 模型，烘焙后取出 quad，用 {@link QuadTransformers} 预旋转到六个方向，
 * 交给 {@link CableBakedModel} 按 {@link CableRenderState} 动态组合。
 * <p>
 * 旋转角度与旧 multipart 生成器完全一致：{@code Direction.values()} 的顺序即
 * {@code CableRenderState} 打包时用的 {@code ordinal()}。
 */
public final class CableModelParts {

    /** 按 Direction.ordinal()：DOWN, UP, NORTH, SOUTH, WEST, EAST。 */
    private static final int[][] ROTATIONS = {
            {90, 0}, {270, 0}, {0, 0}, {0, 180}, {0, 270}, {0, 90}
    };

    private static final String[] BASE_PARTS = {"core", "part", "connect"};

    /** 登记所有零件模型，让 ModelBakery 把它们烘焙进 {@code getModels()} 表。 */
    public static void register(ModelEvent.RegisterAdditional event) {
        for (String name : CableRenderState.VARIANTS) {
            for (String suffix : suffixes(name)) {
                event.register(location(name, suffix));
            }
        }
    }

    private static List<String> suffixes(String name) {
        List<String> result = new ArrayList<>();
        for (String base : BASE_PARTS) {
            result.add(base);
        }
        if (CableRenderState.isCable(name)) {
            for (String base : BASE_PARTS) {
                result.add(base + "_active");
            }
        }
        return result;
    }

    private static ModelResourceLocation location(String name, String suffix) {
        String dir = CableRenderState.modelDir(name);
        return new ModelResourceLocation(TEN.id("block/" + dir + "/" + name + "_" + suffix), "standalone");
    }

    /**
     * 从烘焙结果里取零件 quad 并预旋转到六个方向。核心模型缺失时返回 null，
     * 调用方保留原模型，避免资源缺失把整条线缆渲染成紫黑块。
     */
    public static CableModelParts build(Map<ModelResourceLocation, BakedModel> models, String name) {
        List<BakedQuad> coreOff = quads(models, location(name, "core"));
        if (coreOff.isEmpty()) {
            TEN.LOGGER.warn("[KER] 线缆动态模型缺少 core 零件：{}，保留原模型", name);
            return null;
        }
        boolean isCable = CableRenderState.isCable(name);
        List<BakedQuad> coreOn = isCable ? quads(models, location(name, "core_active")) : coreOff;
        List<BakedQuad> partOff = quads(models, location(name, "part"));
        List<BakedQuad> partOn = isCable ? quads(models, location(name, "part_active")) : partOff;
        List<BakedQuad> connectOff = quads(models, location(name, "connect"));
        List<BakedQuad> connectOn = isCable ? quads(models, location(name, "connect_active")) : connectOff;

        return new CableModelParts(
                coreOff, coreOn,
                rotateToDirections(partOff), rotateToDirections(partOn),
                rotateToDirections(connectOff), rotateToDirections(connectOn));
    }

    private static List<BakedQuad> quads(Map<ModelResourceLocation, BakedModel> models, ModelResourceLocation id) {
        BakedModel model = models.get(id);
        if (model == null) {
            return List.of();
        }
        List<BakedQuad> result = model.getQuads(null, null, RandomSource.create(0L), ModelData.EMPTY, null);
        return result == null ? List.of() : List.copyOf(result);
    }

    /** 把 north 方向的零件旋转到六个方向；索引即 Direction.ordinal()。 */
    private static List<List<BakedQuad>> rotateToDirections(List<BakedQuad> base) {
        List<List<BakedQuad>> result = new ArrayList<>(6);
        for (int[] xy : ROTATIONS) {
            result.add(rotate(base, xy[0], xy[1]));
        }
        return List.copyOf(result);
    }

    private static List<BakedQuad> rotate(List<BakedQuad> base, int x, int y) {
        if (base.isEmpty()) {
            return List.of();
        }
        if (x == 0 && y == 0) {
            return base;
        }
        BlockModelRotation rotation = BlockModelRotation.by(x, y);
        Quaternionf q = rotation.getRotation().getLeftRotation();
        // QuadTransformers 绕原点变换，顶点坐标在 [0,1] 方块空间；补一个绕中心 (0.5,0.5,0.5) 的平移。
        Vector3f center = new Vector3f(0.5f, 0.5f, 0.5f);
        Vector3f rotatedCenter = q.transform(center, new Vector3f());
        Vector3f translation = new Vector3f(center).sub(rotatedCenter);
        IQuadTransformer transformer = QuadTransformers.applying(new Transformation(translation, q, null, null));
        List<BakedQuad> transformed = transformer.process(base);

        // 变换只改顶点法线，不改 BakedQuad#getDirection()；面剔除仍会按旧方向判断，必须同步旋转。
        OctahedralGroup group = rotation.actualRotation();
        List<BakedQuad> result = new ArrayList<>(transformed.size());
        for (BakedQuad quad : transformed) {
            result.add(new BakedQuad(quad.getVertices(), quad.getTintIndex(),
                    group.rotate(quad.getDirection()), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return List.copyOf(result);
    }

    private final List<BakedQuad> coreOff;
    private final List<BakedQuad> coreOn;
    private final List<List<BakedQuad>> partOff;
    private final List<List<BakedQuad>> partOn;
    private final List<List<BakedQuad>> connectOff;
    private final List<List<BakedQuad>> connectOn;

    private CableModelParts(List<BakedQuad> coreOff, List<BakedQuad> coreOn,
                            List<List<BakedQuad>> partOff, List<List<BakedQuad>> partOn,
                            List<List<BakedQuad>> connectOff, List<List<BakedQuad>> connectOn) {
        this.coreOff = coreOff;
        this.coreOn = coreOn;
        this.partOff = partOff;
        this.partOn = partOn;
        this.connectOff = connectOff;
        this.connectOn = connectOn;
    }

    public List<BakedQuad> core(boolean active) {
        return active ? coreOn : coreOff;
    }

    public List<BakedQuad> part(boolean active, int direction) {
        return (active ? partOn : partOff).get(direction);
    }

    public List<BakedQuad> connect(boolean active, int direction) {
        return (active ? connectOn : connectOff).get(direction);
    }
}
