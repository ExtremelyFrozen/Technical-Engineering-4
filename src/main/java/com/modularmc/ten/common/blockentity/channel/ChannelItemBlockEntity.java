package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.TransferNetworks;
import com.modularmc.ten.common.channel.ChannelKey;
import com.modularmc.ten.common.channel.ChannelRegistry;
import com.modularmc.ten.common.channel.ChannelType;
import com.modularmc.ten.common.channel.SharedStorage;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import org.jetbrains.annotations.Nullable;

/**
 * 物品频道方块（末影箱模式）：接入后共享 9 槽 3×3 存储（每槽堆叠 64×成员数动态）。
 * 本地缓冲保留作断开回流源；接入态面能力指向共享 handler。
 */
public class ChannelItemBlockEntity extends AbstractChannelBlockEntity {

    public ChannelItemBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected ChannelType channelType() {
        return ChannelType.ITEM;
    }

    @Override
    public int inventorySize() {
        // 本地缓冲 3×3（与共享槽位数同构，客户端 UI 占位/断开回流源）
        return SharedStorage.ITEM_SLOTS;
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.BOTH;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return true;
    }

    // ───── 面能力：接入态指向共享 handler（零 tick）─────

    @Override
    public boolean hasFaceCapabilityItem(Direction side) {
        return isJoined() && (side == null || side == getFacing());
    }

    @Override
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (isJoined()) {
            SharedStorage shared = sharedStorage();
            if (shared != null) {
                return sharedItemWrapper(shared.getItemHandler(), side);
            }
        }
        return super.getItemHandler(side);
    }

    /** 共享 ItemHandler 的面权限包装（复用基类 canReceiveItem/canExtractItem 语义）。 */
    private IItemHandler sharedItemWrapper(IItemHandler shared, @Nullable Direction side) {
        if (side == null) {
            return shared;
        }
        return new IItemHandler() {

            @Override
            public int getSlots() {
                return shared.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return canExtractItem(side) ? shared.getStackInSlot(slot) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!signalAllowRun() || !canReceiveItem(side)) {
                    return stack;
                }
                if (!slotType(slot).canIn() || !valid(slot, stack)) {
                    return stack;
                }
                return shared.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!signalAllowRun() || !canExtractItem(side) || !slotType(slot).canOut()) {
                    return ItemStack.EMPTY;
                }
                return shared.extractItem(slot, Math.min(amount, maxExtractItem), simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return shared.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slotType(slot).canIn() && valid(slot, stack) && shared.isItemValid(slot, stack);
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
        var sharedHandler = shared.getItemHandler();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = TransferNetworks.insertItem(sharedHandler, stack.copy(), false);
            itemHandler.setStackInSlot(i, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
        }
    }

    // ───── UI：9 槽 3×3（绑定共享门面）─────

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildChannelUI(holder, TENConstants.MACHINE_GUI, root -> {
            // 3×3 槽群垂直居中（机器 GUI 布局规则 v4 内容区 y=5..77 中线 41）：
            // 群范围 14..68（行距 18px 不变），上下留白对称 9px，与列表容器中线对齐。
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 0, 7, 14));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 1, 25, 14));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 2, 43, 14));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 3, 7, 32));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 4, 25, 32));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 5, 43, 32));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 6, 7, 50));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 7, 25, 50));
            root.addChild(TENMachineBlockUIFactory.channelItemSlot(this, 8, 43, 50));
        }, root -> {});
    }
}
