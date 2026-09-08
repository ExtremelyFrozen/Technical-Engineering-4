package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.channel.ChannelKey;
import com.modularmc.ten.common.channel.ChannelRegistry;
import com.modularmc.ten.common.channel.ChannelType;
import com.modularmc.ten.common.channel.SharedStorage;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import org.jetbrains.annotations.Nullable;

/**
 * 能量频道方块（末影箱模式）：接入后共享 1 能量单元（容量
 * BASE_ENERGY_CAPACITY(kFE(10))×成员数动态，拒绝缩容：成员减少时
 * stored ≤ 新容量才缩，否则保持当前容量直至清空）。
 * 本地缓冲保留作断开回流源；接入态面能力指向共享存储。
 */
public class ChannelEnergyBlockEntity extends AbstractChannelBlockEntity {

    public ChannelEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 本地断开缓冲 kFE(1)；共享容量 = BASE_ENERGY_CAPACITY(kFE(10)) × 成员数
        setCapacity(kFE(1));
    }

    @Override
    protected ChannelType channelType() {
        return ChannelType.ENERGY;
    }

    @Override
    protected boolean needsEnergyStorage() {
        return true;
    }

    @Override
    public int inventorySize() {
        return 0;
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    // ───── tick：零 tick 传输 + 镜像字段修正为共享值 ─────

    @Override
    public void tick() {
        super.tick();
        if (isJoined()) {
            SharedStorage shared = sharedStorage();
            if (shared != null) {
                // doBaseData 的 batch 容量缩放只作用于本地缓冲；共享容量由成员数决定，
                // 此处把 @DescSynced 镜像字段修正为共享存储的权威值
                energyStored = shared.getEnergy().getEnergyStored();
                maxEnergyStored = shared.getEnergy().getMaxEnergyStored();
                maxStorageEnergy = maxEnergyStored;
                maxReceiveEnergy = shared.getEnergy().getMaxReceive();
                maxExtractEnergy = shared.getEnergy().getMaxExtract();
            }
        }
    }

    // ───── 面能力：接入态指向共享存储 ─────

    @Override
    public boolean hasFaceCapabilityEnergy(Direction side) {
        return isJoined() && (side == null || side == getFacing());
    }

    @Override
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (isJoined()) {
            SharedStorage shared = sharedStorage();
            if (shared != null) {
                return sharedEnergyWrapper(shared.getEnergy(), side);
            }
        }
        return super.getEnergyStorage(side);
    }

    /** 共享能量存储的面权限包装（对照基类 getEnergyStorage 包装语义）。 */
    private IEnergyStorage sharedEnergyWrapper(IEnergyStorage shared, @Nullable Direction side) {
        if (side == null) {
            return shared;
        }
        return new IEnergyStorage() {

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if (!signalAllowRun() || !canReceiveEnergy(side)) {
                    return 0;
                }
                return shared.receiveEnergy(Math.min(maxReceive, maxReceiveEnergy), simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if (!canExternalExtract() || !signalAllowRun() || !canExtractEnergy(side)) {
                    return 0;
                }
                return shared.extractEnergy(Math.min(maxExtract, maxExtractEnergy), simulate);
            }

            @Override
            public int getEnergyStored() {
                return signalAllowRun() || side == null ? shared.getEnergyStored() : 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return shared.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return canExternalExtract() && canExtractEnergy(side);
            }

            @Override
            public boolean canReceive() {
                return canReceiveEnergy(side);
            }
        };
    }

    // ───── 回流：本地缓冲 → 共享（满留本地）─────

    @Override
    protected void pushLocalToShared() {
        ChannelRegistry reg = registry();
        ChannelKey key = joinedKey();
        if (reg == null || key == null || energyStorage == null) {
            return;
        }
        SharedStorage shared = reg.get(key);
        if (shared == null) {
            return;
        }
        var target = shared.getEnergy();
        while (energyStorage.getEnergyStored() > 0) {
            int extracted = energyStorage.extractEnergy(Integer.MAX_VALUE, false);
            if (extracted <= 0) {
                break;
            }
            int accepted = target.receiveEnergy(extracted, false);
            if (accepted < extracted) {
                // 频道满/部分接收 → 剩余放回本地缓冲（内容不丢）
                energyStorage.receiveEnergy(extracted - accepted, false);
                break;
            }
        }
    }

    // ───── UI：能量 gauge（镜像字段已由 tick 修正为共享值）─────

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildChannelUI(holder, TENConstants.MACHINE_GUI, root -> {}, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
        });
    }
}
