package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.common.channel.SharedStorage;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * 流体 UI 门面 handler（零 tick：接入后 gauge 直接指向共享存储）。
 * <p>
 * UI 槽位在构建时绑定此门面（对象引用恒定）；每次读写经 {@link #resolve()}
 * 动态解析：
 * <ul>
 * <li>服务端接入态 → 频道共享 tanks（成员共享，容量=2000×成员数动态）</li>
 * <li>其余情况（未接入 / 客户端镜像）→ 本地缓冲 tanks（断开回流源）</li>
 * </ul>
 * 由此 join/leave 切换后无需重建 UI，gauge 自动指向新后端。
 */
public final class ChannelFluidHandlerFacade implements IFluidHandler {

    private final ChannelFluidBlockEntity channel;

    public ChannelFluidHandlerFacade(ChannelFluidBlockEntity channel) {
        this.channel = channel;
    }

    private IFluidHandler resolve() {
        if (channel.getLevel() != null && !channel.getLevel().isClientSide()) {
            SharedStorage shared = channel.sharedStorage();
            if (shared != null) {
                return combinedHandler(shared.getTanks());
            }
        }
        return combinedHandler(channel.tanks);
    }

    /** 频道 tanks 的组合 IFluidHandler（频道 tankType/valid 恒 BOTH/true，直接遍历）。 */
    private static IFluidHandler combinedHandler(List<MachineFluidTank> tanks) {
        return new IFluidHandler() {

            @Override
            public int getTanks() {
                return tanks.size();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getFluid() : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getCapacity() : 0;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank >= 0 && tank < tanks.size() && tanks.get(tank).isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                FluidStack remaining = resource.copy();
                for (MachineFluidTank tank : tanks) {
                    int filled = tank.fill(remaining, action);
                    remaining.shrink(filled);
                    if (remaining.isEmpty()) {
                        break;
                    }
                }
                return resource.getAmount() - remaining.getAmount();
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                FluidStack drained = FluidStack.EMPTY;
                for (MachineFluidTank tank : tanks) {
                    FluidStack current = tank.getFluid();
                    if (current.isEmpty() || !current.is(resource.getFluid())) {
                        continue;
                    }
                    FluidStack piece = tank.drain(resource.getAmount() - drained.getAmount(), action);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    if (drained.isEmpty()) {
                        drained = piece.copy();
                    } else {
                        drained.grow(piece.getAmount());
                    }
                    if (drained.getAmount() >= resource.getAmount()) {
                        break;
                    }
                }
                return drained;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                FluidStack drained = FluidStack.EMPTY;
                for (MachineFluidTank tank : tanks) {
                    FluidStack piece = tank.drain(maxDrain - drained.getAmount(), action);
                    if (piece.isEmpty()) {
                        continue;
                    }
                    if (drained.isEmpty()) {
                        drained = piece.copy();
                    } else if (drained.is(piece.getFluid())) {
                        drained.grow(piece.getAmount());
                    }
                    if (drained.getAmount() >= maxDrain) {
                        break;
                    }
                }
                return drained;
            }
        };
    }

    // ───── IFluidHandler 委托（每次动态解析后端）─────

    @Override
    public int getTanks() {
        return resolve().getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return resolve().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return resolve().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return resolve().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return resolve().fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return resolve().drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return resolve().drain(maxDrain, action);
    }
}
