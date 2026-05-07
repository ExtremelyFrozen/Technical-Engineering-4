package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TransferModePacket(BlockPos pos, int changeType, Direction direction) implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("transfer_mode");
    public static final Type<TransferModePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, TransferModePacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeBlockPos(p.pos);
                b.writeInt(p.changeType);
                b.writeEnum(p.direction);
            },
            b -> new TransferModePacket(b.readBlockPos(), b.readInt(), b.readEnum(Direction.class)));

    @Override
    public Type<TransferModePacket> type() {
        return TYPE;
    }

    public static void handle(TransferModePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Level level = ctx.player().level();
            if (level.getBlockEntity(p.pos) instanceof CmMachineBlockEntity machine) {
                int mode;
                switch (p.changeType) {
                    case 0 -> {
                        mode = machine.energyFaceMode.getOrDefault(p.direction, FaceOption.OFF) + 1;
                        if (mode >= FaceOption.size()) mode = 0;
                        machine.energyFaceMode.put(p.direction, mode);
                    }
                    case 1 -> {
                        mode = machine.itemFaceMode.getOrDefault(p.direction, FaceOption.OFF) + 1;
                        if (mode >= FaceOption.size()) mode = 0;
                        machine.itemFaceMode.put(p.direction, mode);
                    }
                    case 2 -> {
                        mode = machine.fluidFaceMode.getOrDefault(p.direction, FaceOption.OFF) + 1;
                        if (mode >= FaceOption.size()) mode = 0;
                        machine.fluidFaceMode.put(p.direction, mode);
                    }
                }
                if (ctx.player() instanceof ServerPlayer sp) {
                    for (Direction d : Direction.values()) {
                        PacketDistributor.sendToPlayer(sp, new FaceInfoPacket(machine, d));
                    }
                }
            }
        });
    }
}
