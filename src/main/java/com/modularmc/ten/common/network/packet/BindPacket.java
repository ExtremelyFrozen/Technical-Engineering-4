package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;
import com.modularmc.ten.client.ClientData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BindPacket(BlockPos bind, BlockPos pos) implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("bind");
    public static final Type<BindPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, BindPacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeBlockPos(p.bind);
                b.writeBlockPos(p.pos);
            },
            b -> new BindPacket(b.readBlockPos(), b.readBlockPos()));

    @Override
    public Type<BindPacket> type() {
        return TYPE;
    }

    public static void handle(BindPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientData.binds.put(p.pos, p.bind));
    }
}
