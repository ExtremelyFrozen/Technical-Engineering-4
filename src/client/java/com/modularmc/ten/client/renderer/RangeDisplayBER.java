package com.modularmc.ten.client.renderer;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 范围显示（Range Display）客户端渲染器。
 * <p>
 * 参考 just-dire-things 的 {@code AreaAffectingBER}（26.1.2 同款参照）：
 * 用客户端 BlockEntityRenderer 每帧绘制工作范围线框，而非服务端粒子
 * （更稳定、不受粒子寿命/距离限制）。
 * <p>
 * 1.21.1 线为经典 BER 签名（无 RenderState 抽象）：render() 直接读
 * {@code rangeVisible}（@DescSynced 客户端同步）与 {@code getRangeBoxes()}
 * （世界绝对坐标），减去机器位置转为 BER 本地坐标后绘制。
 * <p>
 * 线框走原版 {@link RenderType#lines()}（lines shader，顶点 normal 表示线方向
 * 用于控制线宽），与原版结构线框同渲染管线。
 */
public class RangeDisplayBER implements BlockEntityRenderer<CmMachineBlockEntity> {

    /** 范围边框颜色（ARGB，26.1.2 同色蓝，alpha 取 1.0F 渲染）。 */
    private static final int LINE_COLOR_ARGB = 0xFF4A90D9;

    public RangeDisplayBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CmMachineBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.rangeVisible || blockEntity.getLevel() == null) {
            return;
        }
        List<AABB> absBoxes = blockEntity.getRangeBoxes();
        if (absBoxes.isEmpty()) {
            return;
        }
        var bePos = blockEntity.getBlockPos();
        double ox = bePos.getX(), oy = bePos.getY(), oz = bePos.getZ();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        for (AABB box : absBoxes) {
            drawBoxLines(poseStack.last(), buffer, box.move(-ox, -oy, -oz));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CmMachineBlockEntity blockEntity) {
        // 范围通常大于方块本体，视锥裁剪需按扩展包围盒判定
        return blockEntity.rangeVisible;
    }

    /**
     * NeoForge IBlockEntityRendererExtension：按实际范围扩展渲染包围盒，
     * 避免线框被方块尺寸视锥剔除（Quarry 深至世界底、Beacon 半径 32）。
     */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(CmMachineBlockEntity blockEntity) {
        if (blockEntity.rangeVisible) {
            List<AABB> boxes = blockEntity.getRangeBoxes();
            if (!boxes.isEmpty()) {
                AABB total = boxes.get(0);
                for (int i = 1; i < boxes.size(); i++) {
                    total = total.minmax(boxes.get(i));
                }
                return total;
            }
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
    }

    @Override
    public int getViewDistance() {
        // 大范围机器（采矿场/信标）从远处也应可见
        return 128;
    }

    /**
     * 沿 AABB 的 12 条棱绘制线框（每棱 2 顶点，normal 表示线方向，
     * 供 rendertype_lines shader 计算线宽）。
     */
    private static void drawBoxLines(PoseStack.Pose pose, VertexConsumer builder, AABB box) {
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        // 12 条棱
        line(pose, builder, minX, minY, minZ, maxX, minY, minZ, 1, 0, 0);
        line(pose, builder, maxX, minY, minZ, maxX, minY, maxZ, 0, 0, 1);
        line(pose, builder, maxX, minY, maxZ, minX, minY, maxZ, -1, 0, 0);
        line(pose, builder, minX, minY, maxZ, minX, minY, minZ, 0, 0, -1);

        line(pose, builder, minX, maxY, minZ, maxX, maxY, minZ, 1, 0, 0);
        line(pose, builder, maxX, maxY, minZ, maxX, maxY, maxZ, 0, 0, 1);
        line(pose, builder, maxX, maxY, maxZ, minX, maxY, maxZ, -1, 0, 0);
        line(pose, builder, minX, maxY, maxZ, minX, maxY, minZ, 0, 0, -1);

        line(pose, builder, minX, minY, minZ, minX, maxY, minZ, 0, 1, 0);
        line(pose, builder, maxX, minY, minZ, maxX, maxY, minZ, 0, 1, 0);
        line(pose, builder, maxX, minY, maxZ, maxX, maxY, maxZ, 0, 1, 0);
        line(pose, builder, minX, minY, maxZ, minX, maxY, maxZ, 0, 1, 0);
    }

    private static void line(PoseStack.Pose pose, VertexConsumer builder,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float nx, float ny, float nz) {
        float r = (LINE_COLOR_ARGB >> 16 & 0xFF) / 255.0F;
        float g = (LINE_COLOR_ARGB >> 8 & 0xFF) / 255.0F;
        float b = (LINE_COLOR_ARGB & 0xFF) / 255.0F;
        builder.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0F).setNormal(pose, nx, ny, nz);
        builder.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0F).setNormal(pose, nx, ny, nz);
    }
}
