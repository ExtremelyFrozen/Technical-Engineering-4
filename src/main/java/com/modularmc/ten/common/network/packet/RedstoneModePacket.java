package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RedstoneModePacket(int mode, BlockPos pos) implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("redstone_mode");
    public static final Type<RedstoneModePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, RedstoneModePacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeInt(p.mode);
                b.writeBlockPos(p.pos);
            },
            b -> new RedstoneModePacket(b.readInt(), b.readBlockPos()));

    @Override
    public Type<RedstoneModePacket> type() {
        return TYPE;
    }

    public static void handle(RedstoneModePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level.getBlockEntity(p.pos) instanceof CmMachineBlockEntity machine) {
                machine.data.set(CmMachineBlockEntity.RED_MODE, p.mode);
            }
        });
    }
}
