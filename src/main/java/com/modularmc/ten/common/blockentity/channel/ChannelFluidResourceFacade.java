package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.api.capability.FluidHandlerResourceAdapter;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.common.channel.SharedStorage;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

/**
 * 流体 UI 门面 handler（F2：先开 UI 后接入 gauge 过期修复）。
 * <p>
 * UI 槽位在构建时绑定此门面（对象引用恒定）；每次读写经 {@link #resolve()}
 * 动态解析：
 * <ul>
 * <li>服务端接入态 → 频道共享 tanks（成员共享，容量=2000×成员数动态）</li>
 * <li>其余情况（未接入 / 客户端镜像）→ 本地缓冲 tanks（断开回流源）</li>
 * </ul>
 * 由此 join/leave 切换后无需重建 UI，gauge 自动指向新后端。
 * <p>
 * 内部按后端切换缓存 {@link FluidHandlerResourceAdapter}（SnapshotJournal 事务安全）；
 * 脏标记由 tank 自身 changeListener 保证（本地→markDirty，共享→registry.setDirty），
 * 因此 commitCallback 为空。
 */
public final class ChannelFluidResourceFacade implements ResourceHandler<FluidResource> {

    private final ChannelFluidBlockEntity channel;
    private FluidHandlerResourceAdapter delegate;
    private List<MachineFluidTank> delegateTanks;

    public ChannelFluidResourceFacade(ChannelFluidBlockEntity channel) {
        this.channel = channel;
    }

    private FluidHandlerResourceAdapter resolve() {
        List<MachineFluidTank> target;
        if (channel.getLevel() != null && !channel.getLevel().isClientSide()) {
            SharedStorage shared = channel.sharedStorage();
            target = shared != null ? shared.getTanks() : channel.tanks;
        } else {
            target = channel.tanks;
        }
        if (delegate == null || delegateTanks != target) {
            // 门面（UI，side=null）：读写全允许；频道 tankType/valid 恒 BOTH/true
            delegate = new FluidHandlerResourceAdapter(
                    target,
                    channel::tankType,
                    (index, stack) -> channel.valid(index.intValue(), stack),
                    () -> true,
                    () -> true,
                    () -> {});
            delegateTanks = target;
        }
        return delegate;
    }

    // ───── ResourceHandler 委托（每次动态解析后端）─────

    @Override
    public int size() {
        return resolve().size();
    }

    @Override
    public FluidResource getResource(int index) {
        return resolve().getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return resolve().getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return resolve().getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resolve().isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return resolve().insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return resolve().extract(index, resource, amount, transaction);
    }
}
