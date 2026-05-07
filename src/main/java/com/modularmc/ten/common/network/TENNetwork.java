package com.modularmc.ten.common.network;

import com.modularmc.ten.common.network.packet.*;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class TENNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // C→S
        registrar.playToServer(RedstoneModePacket.TYPE, RedstoneModePacket.CODEC, RedstoneModePacket::handle);
        registrar.playToServer(TransferModePacket.TYPE, TransferModePacket.CODEC, TransferModePacket::handle);
        registrar.playToServer(ToolModePacket.TYPE, ToolModePacket.CODEC, ToolModePacket::handle);
        registrar.playToServer(CheckRequestPacket.TYPE, CheckRequestPacket.CODEC, CheckRequestPacket::handle);

        // S→C
        registrar.playToClient(FaceInfoPacket.TYPE, FaceInfoPacket.CODEC, FaceInfoPacket::handle);
        registrar.playToClient(BindPacket.TYPE, BindPacket.CODEC, BindPacket::handle);
        registrar.playToClient(ChannelInfoPacket.TYPE, ChannelInfoPacket.CODEC, ChannelInfoPacket::handle);
        registrar.playToClient(CheckResponsePacket.TYPE, CheckResponsePacket.CODEC, CheckResponsePacket::handle);
    }
}
