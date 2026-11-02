package com.modularmc.ten.api.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * 传输节点基类（移植自 TE4-New，有裁剪）：服务端权威的连接状态与网络归属。
 * <p>
 * 裁剪项及原因（与 New 版差异）：
 * <ul>
 * <li>染色（DyeColor）：本工程管道资源无染色渲染支持，染色会成无视觉反馈的孤立功能</li>
 * <li>Filter/Blocker：无逐跳运输过滤门，无红石控制器变体</li>
 * <li>客户端网络镜像/reduced update tag：连接外观走 CableBased.CONNECTION blockstate</li>
 * <li>内容兼容钩子（isContentsCompatible）：本工程无流体管道，无内容约束场景</li>
 * </ul>
 * 连接位语义：transmitter 位=管道间连接（渲染为 connect）；acceptor 位=管道-设备
 * 连接（有效模式取自 connectionTypes，PUSH/PULL 在此生效）。
 */
public abstract class Transmitter<AC, NET extends Network<AC, NET, T>, T extends Transmitter<AC, NET, T>> {

    final ITransmitterProvider tile;
    private final ConnectionType[] connectionTypes = {
            ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL,
            ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL
    };
    byte currentTransmitterConnections;
    byte currentAcceptorConnections;
    private @Nullable NET network;
    private boolean orphan = true;

    protected Transmitter(ITransmitterProvider tile) {
        this.tile = tile;
    }

    public static boolean connectionBit(byte c, Direction s) {
        return (c & (1 << s.ordinal())) > 0;
    }

    public static byte setBit(byte c, boolean v, Direction s) {
        return (byte) ((c & ~(1 << s.ordinal())) | ((v ? 1 : 0) << s.ordinal()));
    }

    public void rebuild() {
        if (!isRemote() && hasNetwork()) {
            TransmitterNetworkRegistry.remove(self());
            TransmitterNetworkRegistry.join(self());
        }
    }

    public byte getTransmitterConnections() {
        return currentTransmitterConnections;
    }

    public byte getAcceptorConnections() {
        return currentAcceptorConnections;
    }

    @SuppressWarnings("unchecked")
    T self() {
        return (T) this;
    }

    public BlockPos getBlockPos() {
        return tile.getBlockPos();
    }

    public @Nullable Level getLevel() {
        return tile.getLevel();
    }

    public boolean isRemote() {
        return tile.getLevel() == null || tile.getLevel().isClientSide();
    }

    public ITransmitterProvider getTile() {
        return tile;
    }

    public boolean isValid() {
        return !tile.isInvalid();
    }

    /**
     * 边的有效连接模式：无连接位 → NONE；管道间连接恒为 NORMAL；
     * 管道-设备连接取本方向的设定模式（PUSH/PULL 在此生效）。
     */
    public ConnectionType getConnectionType(Direction side) {
        int i = side.ordinal();
        if (!connectionBit((byte) (currentTransmitterConnections | currentAcceptorConnections), side)) {
            return ConnectionType.NONE;
        }
        if (connectionBit(currentTransmitterConnections, side)) {
            return ConnectionType.NORMAL;
        }
        return connectionTypes[i];
    }

    public ConnectionType getConnectionTypeRaw(@Nullable Direction side) {
        return side == null ? ConnectionType.NONE : connectionTypes[side.ordinal()];
    }

    public void setConnectionTypeRaw(Direction side, ConnectionType type) {
        connectionTypes[side.ordinal()] = type;
    }

    byte getAllCurrentConnections() {
        return (byte) (currentTransmitterConnections | currentAcceptorConnections);
    }

    public @Nullable NET getNetwork() {
        return network;
    }

    public boolean hasNetwork() {
        return !orphan && network != null;
    }

    public boolean isOrphan() {
        return orphan;
    }

    public void setOrphan(boolean o) {
        orphan = o;
    }

    /**
     * 网络归属切换。服务端返回 true 表示归属发生变化、调用方需处理更新
     * （客户端无网络镜像，恒返回 false 的客户端分支已随镜像一并裁剪）。
     */
    public boolean setNetwork(@Nullable NET net, boolean requestNow) {
        if (network == net) {
            return false;
        }
        network = net;
        orphan = network == null;
        if (!isRemote()) {
            if (requestNow) {
                requestsUpdate();
            } else {
                return true;
            }
        }
        return false;
    }

    public abstract NET createEmptyNetwork(UUID id);

    public abstract NET createNetworkByMerging(Collection<NET> nets);

    public abstract boolean supportsTransmission(Transmitter<?, ?, ?> other);

    protected abstract boolean isValidAcceptor(Direction side);

    /** 全量刷新连接位：新增位通知邻居重连、消失位通知邻居断开，最后刷新 blockstate。 */
    public void refreshConnections() {
        if (isRemote()) {
            return;
        }
        byte pt = getPossibleTransmitterConnections();
        byte pa = getPossibleAcceptorConnections();
        byte ne = 0, removed = 0;
        if ((pt | pa) != getAllCurrentConnections()) {
            if (pt != currentTransmitterConnections) {
                byte diff = (byte) (pt ^ currentTransmitterConnections);
                ne = (byte) (diff & ~currentTransmitterConnections); // 新出现的位
                removed = (byte) (diff & currentTransmitterConnections); // 消失的位
            }
        }
        currentTransmitterConnections = pt;
        currentAcceptorConnections = pa;
        if (ne != 0) {
            checkReconnect(ne);
        }
        if (removed != 0) {
            notifyDisconnected(removed);
        }
        tile.notifyChanges();
    }

    public void refreshConnections(Direction side) {
        if (isRemote()) {
            return;
        }
        boolean pt = getPossibleTransmitterConnection(side), pa = getPossibleAcceptorConnection(side);
        boolean had = connectionBit(getAllCurrentConnections(), side);
        currentTransmitterConnections = setBit(currentTransmitterConnections, pt, side);
        currentAcceptorConnections = setBit(currentAcceptorConnections, pa, side);
        if ((pt || pa) != had) {
            tile.notifyChanges();
            // 本侧连接丢失时通知邻居同样刷新，保证双侧连接位一致
            if (had) {
                notifyDisconnected(setBit((byte) 0, true, side));
            }
        }
    }

    byte getPossibleTransmitterConnections() {
        byte b = 0;
        for (Direction d : Direction.values()) {
            if (getPossibleTransmitterConnection(d)) {
                b |= (byte) (1 << d.ordinal());
            }
        }
        return b;
    }

    byte getPossibleAcceptorConnections() {
        byte b = 0;
        for (Direction d : Direction.values()) {
            if (getPossibleAcceptorConnection(d)) {
                b |= (byte) (1 << d.ordinal());
            }
        }
        return b;
    }

    boolean getPossibleTransmitterConnection(Direction side) {
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        if (level.getBlockEntity(getBlockPos().relative(side)) instanceof ITransmitterProvider tb) {
            Transmitter<?, ?, ?> o = tb.getTransmitter();
            return supportsTransmission(o) && getConnectionTypeRaw(side) != ConnectionType.NONE && o.getConnectionTypeRaw(side.getOpposite()) != ConnectionType.NONE;
        }
        return false;
    }

    boolean getPossibleAcceptorConnection(Direction side) {
        Level level = getLevel();
        if (level == null) {
            return false;
        }
        BlockPos target = getBlockPos().relative(side);
        if (level.getBlockEntity(target) instanceof ITransmitterProvider) {
            return false;
        }
        return isValidAcceptor(side);
    }

    private void checkReconnect(byte ne) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = getBlockPos();
        for (Direction d : Direction.values()) {
            if (connectionBit(ne, d)) {
                if (level.getBlockEntity(pos.relative(d)) instanceof ITransmitterProvider tb) {
                    tb.getTransmitter().refreshConnections(d.getOpposite());
                }
            }
        }
    }

    private void notifyDisconnected(byte removed) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = getBlockPos();
        for (Direction d : Direction.values()) {
            if (connectionBit(removed, d)) {
                if (level.getBlockEntity(pos.relative(d)) instanceof ITransmitterProvider tb) {
                    tb.getTransmitter().refreshConnections(d.getOpposite());
                }
            }
        }
    }

    public void onModeChange(Direction side) {
        markDirtyAcceptor(side);
        if (getPossibleTransmitterConnections() != currentTransmitterConnections) {
            markDirtyTransmitters();
        }
        tile.notifyChanges();
    }

    public void onNeighborBlockChange(Direction side) {
        refreshConnections(side);
    }

    void markDirtyTransmitters() {
        tile.notifyChanges();
        requestsUpdate();
    }

    void markDirtyAcceptor(Direction side) {
        if (hasNetwork()) {
            network.acceptorChanged(self(), side);
        }
    }

    public void requestsUpdate() {
        tile.notifyChanges();
    }

    /** 拆网时取回本节点缓冲份额（BufferedTransmitter 覆写实现无损分流）。 */
    public void validateAndTakeShare() {
        takeShare();
    }

    public abstract void takeShare();

    /** BFS 组网的边判定：类型一致 + 双侧模式均非 NONE。 */
    public boolean isValidTransmitterBasic(ITransmitterProvider neighborTile, Direction side) {
        Transmitter<?, ?, ?> other = neighborTile.getTransmitter();
        if (!supportsTransmission(other)) {
            return false;
        }
        return getConnectionTypeRaw(side) != ConnectionType.NONE && other.getConnectionTypeRaw(side.getOpposite()) != ConnectionType.NONE;
    }

    public CompoundTag write(HolderLookup.Provider prov, CompoundTag tag) {
        tag.putByte("Tran", currentTransmitterConnections);
        tag.putByte("Acc", currentAcceptorConnections);
        tag.putIntArray("CT", getRawCT());
        return tag;
    }

    public void read(HolderLookup.Provider prov, CompoundTag tag) {
        if (tag.contains("Tran")) {
            currentTransmitterConnections = tag.getByte("Tran");
        }
        if (tag.contains("Acc")) {
            currentAcceptorConnections = tag.getByte("Acc");
        }
        readRawCT(tag);
    }

    private int[] getRawCT() {
        int[] r = new int[6];
        for (int i = 0; i < 6; i++) {
            r[i] = connectionTypes[i].ordinal();
        }
        return r;
    }

    private void readRawCT(CompoundTag tag) {
        if (tag.contains("CT", Tag.TAG_INT_ARRAY)) {
            int[] r = tag.getIntArray("CT");
            for (int i = 0; i < r.length && i < 6; i++) {
                connectionTypes[i] = ConnectionType.of(r[i]);
            }
        }
    }
}
