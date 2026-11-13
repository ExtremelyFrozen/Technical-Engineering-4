package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.transmission.CableRenderState;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.block.machine.CableBased;
import com.modularmc.ten.common.block.machine.DirectionalMachineBlock;
import com.modularmc.ten.common.block.machine.HorizontalMachineBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

public class TENModels {

    public static BlockModelBuilder cubeAll(BlockStateProvider prov, String name) {
        return prov.models().cubeAll(name, TEN.id("block/" + name));
    }

    private static String machineTex(String name) {
        return "block/" + name;
    }

    public static BlockModelBuilder machine(BlockStateProvider prov, String name) {
        String tex = machineTex(name);
        return prov.models().cube(name,
                TEN.id("block/machine_bottom"),
                TEN.id("block/machine_top"),
                TEN.id(tex),
                TEN.id("block/machine_side"),
                TEN.id("block/machine_side_connection"),
                TEN.id("block/machine_side_connection_flipped"))
                .texture("particle", TEN.id("block/machine_side"));
    }

    public static BlockModelBuilder machineActive(BlockStateProvider prov, String name) {
        String tex = machineTex(name);
        return prov.models().cube(name + "_active",
                TEN.id("block/machine_bottom"),
                TEN.id("block/machine_top"),
                TEN.id(tex + "_active"),
                TEN.id("block/machine_side"),
                TEN.id("block/machine_side_connection"),
                TEN.id("block/machine_side_connection_flipped"))
                .texture("particle", TEN.id("block/machine_side"));
    }

    // 引擎方块 —— 顶/侧面同纹，底面单独
    public static BlockModelBuilder engine(BlockStateProvider prov, String name) {
        return prov.models()
                .cubeBottomTop(name,
                        TEN.id("block/" + name),
                        TEN.id("block/" + name),
                        TEN.id("block/" + name + "_bottom"))
                .texture("particle", TEN.id("block/" + name));
    }

    public static BlockModelBuilder engineActive(BlockStateProvider prov, String name) {
        String activeName = name + "_active";
        return prov.models()
                .cubeBottomTop(activeName,
                        TEN.id("block/" + activeName),
                        TEN.id("block/" + activeName),
                        TEN.id("block/" + name + "_bottom"))
                .texture("particle", TEN.id("block/" + activeName));
    }

    public static void cellBlockstate(BlockStateProvider prov, Block block, String baseName) {
        var empty = prov.models().getExistingFile(TEN.id("block/" + baseName + "_empty"));
        var normal = prov.models().getExistingFile(TEN.id("block/" + baseName));
        var builder = prov.getVariantBuilder(block);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int y = switch (dir) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            builder.partialState()
                    .with(HorizontalMachineBlock.FACING, dir)
                    .with(HorizontalMachineBlock.ACTIVE, false)
                    .modelForState().modelFile(empty).rotationY(y).addModel()
                    .partialState()
                    .with(HorizontalMachineBlock.FACING, dir)
                    .with(HorizontalMachineBlock.ACTIVE, true)
                    .modelForState().modelFile(normal).rotationY(y).addModel();
        }
    }

    public static void channelBlockstate(BlockStateProvider prov, Block block, String name) {
        var channelModel = prov.models().getExistingFile(TEN.id("block/channel/" + name));
        var channelActive = prov.models().getExistingFile(TEN.id("block/channel/" + name + "_active"));
        var normal = prov.models().getBuilder(name).parent(channelModel);
        var active = prov.models().getBuilder(name + "_active").parent(channelActive);
        var builder = prov.getVariantBuilder(block);
        for (Direction dir : Direction.values()) {
            int x = 0, y = 0;
            switch (dir) {
                case EAST -> y = 90;
                case SOUTH -> y = 180;
                case WEST -> y = 270;
                case UP -> x = 270;
                case DOWN -> x = 90;
            }
            builder.partialState()
                    .with(DirectionalMachineBlock.FACING, dir)
                    .with(DirectionalMachineBlock.ACTIVE, false)
                    .modelForState().modelFile(normal).rotationX(x).rotationY(y).addModel()
                    .partialState()
                    .with(DirectionalMachineBlock.FACING, dir)
                    .with(DirectionalMachineBlock.ACTIVE, true)
                    .modelForState().modelFile(active).rotationX(x).rotationY(y).addModel();
        }
    }

    /**
     * 线缆/管道的 blockstate 只登记一个占位核心模型。
     * <p>
     * 连接外观不再靠 26 条 multipart 条件拼——那需要六个方向各一个 0..2 属性，单方块 2916 个状态。
     * 现在由客户端 {@code CableBakedModel} 读 {@link CableRenderState} 动态组合核心/连接部件；
     * 这里四个 ACTIVE × WATERLOGGED 组合都指向同一个模型，保证每个状态都能解析到模型。
     * 零件模型（core/part/connect 及 active 版）是 {@code src/main/resources} 下的手写资源，
     * 由客户端在 {@code ModelEvent.RegisterAdditional} 里登记后烘焙。
     */
    public static void cableDynamic(BlockStateProvider prov, Block block, String name) {
        String dir = CableRenderState.modelDir(name);
        var core = prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_core"));
        var builder = prov.getVariantBuilder(block);
        for (boolean active : new boolean[] { false, true }) {
            for (boolean waterlogged : new boolean[] { false, true }) {
                builder.partialState()
                        .with(BaseMachineBlock.ACTIVE, active)
                        .with(CableBased.WATERLOGGED, waterlogged)
                        .modelForState().modelFile(core).addModel();
            }
        }
    }
}
