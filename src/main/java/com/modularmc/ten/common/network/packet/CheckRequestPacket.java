package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CheckRequestPacket implements CustomPacketPayload {

    public static boolean CHECKED = false;
    public static final ResourceLocation ID = TEN.id("check_request");
    public static final Type<CheckRequestPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, CheckRequestPacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {},
            b -> new CheckRequestPacket());

    @Override
    public Type<CheckRequestPacket> type() {
        return TYPE;
    }

    public static void handle(CheckRequestPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CHECKED = true);
    }
}
