package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IngredientType;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 流体频道方块（末影箱模式）：接入后共享 2 tank 存储（每 tank 2000×成员数 mB 动态）。
 * 本地缓冲保留作断开回流源；接入态面能力指向共享 handler。
 * <p>
 * 1.21.1 适配：26.1.2 的 {@code getFluidResourceHandler}（NeoForge 26.1.2 Resource API）
 * 在经典 Capability 下由 {@link #getFluidHandler} 直接方法承担；UI 槽位绑定动态门面
 * {@link ChannelFluidHandlerFacade}（join/leave 切换后无需重建 UI）。
 */
public class ChannelFluidBlockEntity extends AbstractChannelBlockEntity {

    /** 流体 UI 门面（服务端接入态动态解析共享 tanks，其余解析本地缓冲）。 */
    private ChannelFluidHandlerFacade fluidFacade;

    public ChannelFluidBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks.add(new MachineFluidTank(SharedStorage.BASE_FLUID_CAPACITY));
        tanks.add(new MachineFluidTank(SharedStorage.BASE_FLUID_CAPACITY));
    }

    @Override
    protected ChannelType channelType() {
        return ChannelType.FLUID;
    }

    @Override
    public int inventorySize() {
        return 0;
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.BOTH;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    public ChannelFluidHandlerFacade getChannelFluidFacade() {
        if (fluidFacade == null) {
            fluidFacade = new ChannelFluidHandlerFacade(this);
        }
        return fluidFacade;
    }

    // ───── 面能力：接入态指向共享 handler ─────

    @Override
    public boolean hasFaceCapabilityFluid(Direction side) {
        return isJoined() && (side == null || side == getFacing());
    }

    @Override
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        // UI 槽位绑定入口（side=null）：返回动态门面，join/leave 切换后无需重建 UI，
        // gauge 自动指向共享/本地 tanks
        if (side == null) {
            return getChannelFluidFacade();
        }
        if (isJoined()) {
            SharedStorage shared = sharedStorage();
            if (shared != null) {
                return sharedFluidWrapper(shared.getTanks(), side);
            }
        }
        return super.getFluidHandler(side);
    }

    /** 共享 tanks 的面权限 IFluidHandler 包装。 */
    private IFluidHandler sharedFluidWrapper(List<MachineFluidTank> sharedTanks, @Nullable Direction side) {
        IFluidHandler delegate = sharedCombinedHandler(sharedTanks);
        if (side == null) {
            return delegate;
        }
        return new IFluidHandler() {

            @Override
            public int getTanks() {
                return delegate.getTanks();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return canExtractFluid(side) ? delegate.getFluidInTank(tank) : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return delegate.getTankCapacity(tank);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return delegate.isFluidValid(tank, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!signalAllowRun() || !canReceiveFluid(side)) {
                    return 0;
                }
                return delegate.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (!signalAllowRun() || !canExtractFluid(side)) {
                    return FluidStack.EMPTY;
                }
                return delegate.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (!signalAllowRun() || !canExtractFluid(side)) {
                    return FluidStack.EMPTY;
                }
                return delegate.drain(maxDrain, action);
            }
        };
    }

    /** 共享 tanks 的组合 IFluidHandler（频道 tankType/valid 恒 BOTH/true，直接遍历）。 */
    private IFluidHandler sharedCombinedHandler(List<MachineFluidTank> sharedTanks) {
        return new IFluidHandler() {

            @Override
            public int getTanks() {
                return sharedTanks.size();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank >= 0 && tank < sharedTanks.size() ? sharedTanks.get(tank).getFluid() : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank >= 0 && tank < sharedTanks.size() ? sharedTanks.get(tank).getCapacity() : 0;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank >= 0 && tank < sharedTanks.size() && sharedTanks.get(tank).isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                FluidStack remaining = resource.copy();
                for (MachineFluidTank tank : sharedTanks) {
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
                for (MachineFluidTank tank : sharedTanks) {
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
                for (MachineFluidTank tank : sharedTanks) {
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

    // ───── 回流：本地缓冲 → 共享（满留本地）─────

    @Override
    protected void pushLocalToShared() {
        ChannelRegistry reg = registry();
        ChannelKey key = joinedKey();
        if (reg == null || key == null) {
            return;
        }
        SharedStorage shared = reg.get(key);
        if (shared == null) {
            return;
        }
        var targetTanks = shared.getTanks();
        for (MachineFluidTank tank : tanks) {
            FluidStack stack = tank.getFluid();
            if (stack.isEmpty()) {
                continue;
            }
            FluidStack drained = tank.drain(stack.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            FluidStack remaining = drained.copy();
            for (MachineFluidTank target : targetTanks) {
                int filled = target.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
                remaining.shrink(filled);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                // 频道满 → 剩余留本地缓冲（内容不丢）
                tank.setFluid(remaining);
            }
        }
    }

    // ───── UI：2 tank（绑定共享门面）─────

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildChannelUI(holder, TENConstants.MACHINE_GUI, root -> {}, root -> {
            // 2 tank 垂直居中（机器 GUI 布局规则 v4 内容区 y=5..77 中线 41）：
            // 50px 高 → 16..66，上下留白对称 11px，与列表容器中线对齐。
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 7, 16, 18, 50, 0));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 25, 16, 18, 50, 1));
        });
    }
}
