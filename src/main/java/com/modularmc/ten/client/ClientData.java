package com.modularmc.ten.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientData {

    public static final Map<BlockPos, List<Integer>> energy = new HashMap<>();
    public static final Map<BlockPos, List<Integer>> item = new HashMap<>();
    public static final Map<BlockPos, List<Integer>> fluid = new HashMap<>();
    public static final Map<BlockPos, Integer> redstone = new HashMap<>();
    public static final Map<BlockPos, Integer> radius = new HashMap<>();
    public static final Map<BlockPos, BlockPos> binds = new HashMap<>();
    public static final Map<BlockPos, List<BlockPos>> channelInputs = new HashMap<>();
    public static final Map<BlockPos, List<BlockPos>> channelOutputs = new HashMap<>();
    public static final Map<BlockPos, Integer> channelModeInput = new HashMap<>();
    public static final Map<BlockPos, Integer> channelModeOutput = new HashMap<>();

    public static List<Integer> getOrFill(Map<BlockPos, List<Integer>> map, BlockPos pos, int size) {
        return map.computeIfAbsent(pos, k -> {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < size; i++) list.add(0);
            return list;
        });
    }
}
