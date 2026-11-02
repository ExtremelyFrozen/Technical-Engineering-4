package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.TransmitterNetworkRegistry;
import com.modularmc.ten.api.transmission.energy.EnergyNetwork;
import com.modularmc.ten.api.transmission.energy.EnergyTransmitter;
import com.modularmc.ten.api.transmission.energy.TransmitterEnergyStorage;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import dev.vfyjxf.taffy.style.TaffyPosition;

/**
 * 能量线缆方块实体：持有 {@link EnergyTransmitter}，组网/拆网由
 * TransmitterNetworkRegistry 在 level tick 内延迟驱动，本 BE 不再自带 BFS 扫描。
 * 能量常态归网络所有；BE 持久化存网络均分份额（区块卸载不丢能），orphan 时用本地缓冲兜底。
 * 对外能力入口 getEnergy(side) 签名不变（CommonProxy 注册点无需改动）。
 */
public class CableBlockEntity extends CmBlockEntity implements ITransmitterProvider {

    private final EnergyTransmitter transmitter;

    private boolean joinPending;

    /**
     * 输出面位图镜像（bit d = Direction d 为 PULL）：LDLib2 @DescSynced 自动同步到客户端，
     * 供 Jade/模型读取方向模式（transmitter 本体不经 managed 字段体系，无客户端同步）；
     * 服务端在 onModeChange 后重写。
     */
    @Persisted
    @DescSynced
    public byte pullFacesMask;

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        int capacity = capacityFor(state);
        int transfer = transferFor(state);
        this.transmitter = new EnergyTransmitter(this, capacity, transfer);
    }

    @Override
    public EnergyTransmitter getTransmitter() {
        return transmitter;
    }

    public IEnergyStorage getEnergy(Direction side) {
        return new TransmitterEnergyStorage(transmitter, side);
    }

    public boolean hasUi() {
        return false;
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (joinPending) {
            joinPending = false;
            transmitter.refreshConnections();
            TransmitterNetworkRegistry.join(transmitter);
        }
        // 网络传输由注册表驱动；此处仅同步活性状态（驱动 core/part/connect 的 active 模型）
        EnergyNetwork net = transmitter.getNetwork();
        setActive(net != null && net.getLastMoved() > 0);
    }

    // ───── ITransmitterProvider：网络接入生命周期 ─────

    @Override
    public void onLoad() {
        super.onLoad();
        joinNetwork();
    }

    private void joinNetwork() {
        // 延迟到服务端首 tick：注册回调（onLoad）期间查询邻居会触发
        // 邻居 BE 创建→邻居 join→互查的无限递归（GameTest StackOverflow 已验证）。
        if (level != null && !level.isClientSide()) {
            joinPending = true;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            TransmitterNetworkRegistry.remove(transmitter);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide()) {
            TransmitterNetworkRegistry.remove(transmitter);
        }
        super.onChunkUnloaded();
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    @Override
    public void notifyChanges() {
        // 不走 markDirty/sendBlockUpdated：那会触发邻居 neighborChanged → refreshConnections 递归。
        // 直接传 transmitter 引用刷新外观（查自身 BE 会在注册流程中再创建 BE，见 updateConnectionState 注释）。
        setChanged();
        if (level != null && !level.isClientSide()) {
            if (getBlockState().getBlock() instanceof com.modularmc.ten.common.block.machine.CableBased cable) {
                cable.updateConnectionState(level, worldPosition, transmitter);
            }
            refreshPullFacesMask();
        }
    }

    /** 重算输出面位图镜像（服务端；@DescSynced 变化时自动推客户端）。 */
    public void refreshPullFacesMask() {
        byte mask = 0;
        for (Direction d : Direction.values()) {
            if (transmitter.getConnectionTypeRaw(d) == ConnectionType.PULL) {
                mask |= (byte) (1 << d.ordinal());
            }
        }
        pullFacesMask = mask;
    }

    // ───── 持久化（含旧版单 BE 存储迁移）─────

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        transmitter.read(registries, tag);
        if (tag.contains("Energy", Tag.TAG_LONG)) {
            transmitter.setBuffer(tag.getLong("Energy"));
        } else if (tag.contains("energy", Tag.TAG_INT)) {
            // 旧版数据迁移：原单 BE storage 能量读入本地缓冲，首次 join 由 absorbBuffer 上缴网络
            transmitter.setBuffer(tag.getInt("energy"));
        }
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        transmitter.write(registries, tag);
        EnergyNetwork net = transmitter.getNetwork();
        long share;
        if (net != null && net.size() > 0) {
            // 均分份额存档：余数给 root 成员，保证 Σ份额 = 网络能量（卸载不丢 FE）
            share = net.getBuffer() / net.size();
            if (isNetworkRootMember(net)) {
                share += net.getBuffer() % net.size();
            }
        } else {
            share = transmitter.getBuffer();
        }
        tag.putLong("Energy", share);
    }

    private boolean isNetworkRootMember(EnergyNetwork net) {
        BlockPos min = null;
        for (EnergyTransmitter t : net.getTransmitters()) {
            if (min == null || t.getBlockPos().asLong() < min.asLong()) {
                min = t.getBlockPos();
            }
        }
        return worldPosition.equals(min);
    }

    // ───── GUI ─────

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = TENMachineBlockUIFactory.createRoot(TENConstants.LEGACY_SHEET);
        root.addChild(label(8, 8, "Energy Cable"));
        root.addChild(label(8, 20, "Stored: " + transmitter.getNetworkEnergy() + " FE"));
        root.addChild(label(8, 32, "Rate: " + transferFor(getBlockState()) + " FE/t"));
        return TENMachineBlockUIFactory.buildModularUI(root, holder.player);
    }

    // ───── 分级参数（沿袭旧版平衡数值）─────

    private static int capacityFor(BlockState state) {
        String name = SafeOperationHelper.regNameOf(state.getBlock());
        if ("cable_star".equals(name)) {
            return Integer.MAX_VALUE;
        }
        if ("cable_azure".equals(name)) {
            return 50_000;
        }
        if ("cable_quartz".equals(name)) {
            return 20_000;
        }
        return 1_000;
    }

    private static int transferFor(BlockState state) {
        String name = SafeOperationHelper.regNameOf(state.getBlock());
        if ("cable_star".equals(name)) {
            return 200_000;
        }
        if ("cable_azure".equals(name)) {
            return 4_000;
        }
        if ("cable_quartz".equals(name)) {
            return 1_000;
        }
        return 200;
    }

    private static Label label(int x, int y, String text) {
        Label label = new Label();
        label.setText(Component.literal(text));
        label.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
        });
        return label;
    }

    private void setActive(boolean active) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE) && state.getValue(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE, active), 3);
        }
    }
}
