package com.modularmc.ten.common.network.packet;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.client.ClientData;
import com.modularmc.ten.utils.DirectionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FaceInfoPacket(int energy, int item, int fluid, int redstone, int radius, BlockPos pos, Direction dir) implements CustomPacketPayload {

    public static final ResourceLocation ID = TEN.id("face_info");
    public static final Type<FaceInfoPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, FaceInfoPacket> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeInt(p.energy);
                b.writeInt(p.item);
                b.writeInt(p.fluid);
                b.writeInt(p.redstone);
                b.writeInt(p.radius);
                b.writeBlockPos(p.pos);
                b.writeEnum(p.dir);
            },
            b -> new FaceInfoPacket(b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readBlockPos(), b.readEnum(Direction.class)));

    public FaceInfoPacket(CmMachineBlockEntity machine, Direction dir) {
        this(
                machine.energyFaceMode.getOrDefault(dir, 0),
                machine.itemFaceMode.getOrDefault(dir, 0),
                machine.fluidFaceMode.getOrDefault(dir, 0),
                machine.data.get(CmMachineBlockEntity.RED_MODE),
                machine instanceof RadiusMachineBlockEntity r ? r.radius : 0,
                machine.getBlockPos(), dir);
    }

    @Override
    public Type<FaceInfoPacket> type() {
        return TYPE;
    }

    public static void handle(FaceInfoPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            int i = DirectionHelper.direToInt(p.dir);
            ClientData.getOrFill(ClientData.energy, p.pos, 6).set(i, p.energy);
            ClientData.getOrFill(ClientData.item, p.pos, 6).set(i, p.item);
            ClientData.getOrFill(ClientData.fluid, p.pos, 6).set(i, p.fluid);
            ClientData.redstone.put(p.pos, p.redstone);
            ClientData.radius.put(p.pos, p.radius);
        });
    }
}
