package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.item.IModeChangable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ToolModePacket implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("tool_mode");
    public static final Type<ToolModePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, ToolModePacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {},
            b -> new ToolModePacket());

    @Override
    public Type<ToolModePacket> type() {
        return TYPE;
    }

    public static void handle(ToolModePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().getMainHandItem().getItem() instanceof IModeChangable c) {
                c.change(ctx.player());
            }
        });
    }
}
