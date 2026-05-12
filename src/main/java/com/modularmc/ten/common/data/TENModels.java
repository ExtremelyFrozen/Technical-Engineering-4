package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
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
                TEN.id("block/machine_side"),
                TEN.id("block/machine_side"))
                .texture("particle", TEN.id("block/machine_side"));
    }

    public static BlockModelBuilder machineActive(BlockStateProvider prov, String name) {
        String tex = machineTex(name);
        return prov.models().cube(name + "_active",
                TEN.id("block/machine_bottom"),
                TEN.id("block/machine_top"),
                TEN.id(tex + "_active"),
                TEN.id("block/machine_side"),
                TEN.id("block/machine_side"),
                TEN.id("block/machine_side"))
                .texture("particle", TEN.id("block/machine_side"));
    }

    public static BlockModelBuilder engine(BlockStateProvider prov, String name) {
        return prov.models().cube(name,
                TEN.id("machine/engine/" + name + "_bottom"),   // down
                TEN.id("machine/engine/" + name),                // up
                TEN.id("machine/engine/" + name),                // north
                TEN.id("machine/engine/" + name),                // south
                TEN.id("machine/engine/" + name),                // west
                TEN.id("machine/engine/" + name))                // east
                .texture("particle", TEN.id("machine/engine/" + name));
    }

    public static BlockModelBuilder engineActive(BlockStateProvider prov, String name) {
        return prov.models().cube(name + "_active",
                TEN.id("machine/engine/" + name + "_bottom"),        // down
                TEN.id("machine/engine/" + name + "_active"),         // up
                TEN.id("machine/engine/" + name + "_active"),         // north
                TEN.id("machine/engine/" + name + "_active"),         // south
                TEN.id("machine/engine/" + name + "_active"),         // west
                TEN.id("machine/engine/" + name + "_active"))         // east
                .texture("particle", TEN.id("machine/engine/" + name + "_active"));
    }

    public static void energyCellBlockstate(BlockStateProvider prov, Block block) {
        var empty = prov.models().getExistingFile(TEN.id("block/energy_cell_empty"));
        var normal = prov.models().getExistingFile(TEN.id("block/energy_cell"));
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

    public static void cableMultipart(BlockStateProvider prov, Block block, String name) {
        boolean isCable = name.startsWith("cable");
        String dir = name.startsWith("cable") ? "cable" : "pipe";

        var core = prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_core"));
        var coreActive = isCable ? prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_core_active")) : core;
        var part = prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_part"));
        var partActive = isCable ? prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_part_active")) : part;
        var connect = prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_connect"));
        var connectActive = isCable ? prov.models().getExistingFile(TEN.id("block/" + dir + "/" + name + "_connect_active")) : connect;

        var builder = prov.getMultipartBuilder(block);

        builder.part().modelFile(core).addModel()
                .condition(HorizontalMachineBlock.ACTIVE, false).end();
        builder.part().modelFile(coreActive).addModel()
                .condition(HorizontalMachineBlock.ACTIVE, true).end();

        for (Direction direction : Direction.values()) {
            int x = 0, y = 0;
            switch (direction) {
                case EAST -> y = 90;
                case SOUTH -> y = 180;
                case WEST -> y = 270;
                case UP -> x = 270;
                case DOWN -> x = 90;
            }

            var connProp = CableBased.CONNECTION.get(direction);

            builder.part().modelFile(part).rotationX(x).rotationY(y).addModel()
                    .condition(connProp, 1)
                    .condition(HorizontalMachineBlock.ACTIVE, false).end();
            builder.part().modelFile(partActive).rotationX(x).rotationY(y).addModel()
                    .condition(connProp, 1)
                    .condition(HorizontalMachineBlock.ACTIVE, true).end();

            builder.part().modelFile(connect).rotationX(x).rotationY(y).addModel()
                    .condition(connProp, 2)
                    .condition(HorizontalMachineBlock.ACTIVE, false).end();
            builder.part().modelFile(connectActive).rotationX(x).rotationY(y).addModel()
                    .condition(connProp, 2)
                    .condition(HorizontalMachineBlock.ACTIVE, true).end();
        }
    }
}
