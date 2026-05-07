package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CheckResponsePacket implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("check_response");
    public static final Type<CheckResponsePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, CheckResponsePacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {},
            b -> new CheckResponsePacket());

    @Override
    public Type<CheckResponsePacket> type() {
        return TYPE;
    }

    public static void handle(CheckResponsePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PacketDistributor.sendToServer(new CheckRequestPacket());
        });
    }
}
