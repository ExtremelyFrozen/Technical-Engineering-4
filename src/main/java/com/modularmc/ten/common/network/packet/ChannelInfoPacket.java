package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;
import com.modularmc.ten.client.ClientData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ChannelInfoPacket(int inMode, int outMode, List<BlockPos> inputs, List<BlockPos> outputs, BlockPos pos) implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("channel_info");
    public static final Type<ChannelInfoPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, ChannelInfoPacket> CODEC = StreamCodec.ofMember(
            ChannelInfoPacket::write, ChannelInfoPacket::new);

    public ChannelInfoPacket(FriendlyByteBuf b) {
        this(b.readInt(), b.readInt(), readList(b), readList(b), b.readBlockPos());
    }

    private static List<BlockPos> readList(FriendlyByteBuf b) {
        int size = b.readInt();
        List<BlockPos> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(b.readBlockPos());
        return list;
    }

    public static void write(ChannelInfoPacket p, FriendlyByteBuf b) {
        b.writeInt(p.inMode);
        b.writeInt(p.outMode);
        b.writeInt(p.inputs.size());
        for (var pos : p.inputs) b.writeBlockPos(pos);
        b.writeInt(p.outputs.size());
        for (var pos : p.outputs) b.writeBlockPos(pos);
        b.writeBlockPos(p.pos);
    }

    @Override
    public Type<ChannelInfoPacket> type() {
        return TYPE;
    }

    public static void handle(ChannelInfoPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientData.channelInputs.get(p.pos).clear();
            ClientData.channelOutputs.get(p.pos).clear();
            var inputs = ClientData.channelInputs.computeIfAbsent(p.pos, k -> new ArrayList<>());
            var outputs = ClientData.channelOutputs.computeIfAbsent(p.pos, k -> new ArrayList<>());
            for (int i = 0; i < p.inputs.size(); i++) {
                while (inputs.size() <= i) inputs.add(null);
                inputs.set(i, p.inputs.get(i));
            }
            for (int i = 0; i < p.outputs.size(); i++) {
                while (outputs.size() <= i) outputs.add(null);
                outputs.set(i, p.outputs.get(i));
            }
            ClientData.channelModeInput.put(p.pos, p.inMode);
            ClientData.channelModeOutput.put(p.pos, p.outMode);
        });
    }
}
