package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

public class TENModels {

    public static BlockModelBuilder cubeAll(BlockStateProvider prov, String name) {
        return prov.models().cubeAll(name, TEN.id("block/" + name));
    }

    private static String machineTex(String name) {
        if (name.startsWith("engine_")) return "machine/engine/" + name;
        if (name.startsWith("channel_")) return "machine/channel/" + name;
        // Strip machine_ prefix for machines, keep as-is for others
        return "machine/" + name.replaceFirst("^machine_", "");
    }

    public static BlockModelBuilder machine(BlockStateProvider prov, String name) {
        String tex = machineTex(name);
        return prov.models().cube(name,
                TEN.id("machine/machine_bottom"),
                TEN.id("machine/machine_top"),
                TEN.id(tex),
                TEN.id("machine/machine_side"),
                TEN.id("machine/machine_side"),
                TEN.id("machine/machine_side"))
                .texture("particle", TEN.id("machine/machine_side"));
    }

    public static BlockModelBuilder machineActive(BlockStateProvider prov, String name) {
        String tex = machineTex(name);
        return prov.models().cube(name + "_active",
                TEN.id("machine/machine_bottom"),
                TEN.id("machine/machine_top"),
                TEN.id(tex + "_active"),
                TEN.id("machine/machine_side"),
                TEN.id("machine/machine_side"),
                TEN.id("machine/machine_side"))
                .texture("particle", TEN.id("machine/machine_side"));
    }
}
