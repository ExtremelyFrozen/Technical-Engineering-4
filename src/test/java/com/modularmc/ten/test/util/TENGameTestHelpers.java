package com.modularmc.ten.test.util;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.testframework.gametest.GameTestPlayer;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TENGameTestHelpers {

    private TENGameTestHelpers() {}

    public static GameTestPlayer makeTickingMockServerPlayerInLevel(GameTestHelper helper, GameType gameType) {
        final CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        final GameTestPlayer serverplayer = new GameTestPlayer(helper.getLevel().getServer(), helper.getLevel(), commonlistenercookie.gameProfile(), commonlistenercookie.clientInformation(), helper);
        final Connection connection = new Connection(PacketFlow.SERVERBOUND) {

            @Override
            public void tick() {
                super.tick();
                serverplayer.resetLastActionTime();
            }

            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet, @Nullable ChannelFutureListener listeners, boolean flush) {
                super.send(packet, listeners, flush);
                if (packet instanceof ClientboundKeepAlivePacket ckp) {
                    serverplayer.connection.handleKeepAlive(new ServerboundKeepAlivePacket(ckp.getId()));
                }
            }
        };
        EmbeddedChannel embeddedchannel = new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverplayer, commonlistenercookie);
        helper.getLevel().getServer().getConnection().getConnections().add(connection);
        serverplayer.gameMode.changeGameModeForPlayer(gameType);
        serverplayer.setYRot(180);
        serverplayer.connection.chunkSender.sendNextChunks(serverplayer);
        serverplayer.connection.chunkSender.onChunkBatchReceivedByClient(64f);
        return serverplayer;
    }
}
