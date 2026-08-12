package com.modularmc.ten.common.channel;

import com.modularmc.ten.api.capability.MachineEnergyStorage;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.capability.MachineItemHandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 频道共享存储（末影箱模式核心）。
 * <p>
 * 槽位数固定（物品 9 槽 3×3、流体 2 tank、能量 1 单元），容量=标准容量×成员数
 * 动态重算：
 * <ul>
 * <li>物品：每槽堆叠上限 = {@value #BASE_ITEM_STACK} × 成员数（经 {@link MachineItemHandler#setDynamicSlotLimit}
 * 实现，天然保证槽内已有数量不被缩容）</li>
 * <li>流体：tank 容量 = {@value #BASE_FLUID_CAPACITY} mB × 成员数，拒绝缩容</li>
 * <li>能量：单元容量 = {@value #BASE_ENERGY_CAPACITY} FE（kFE(10)）× 成员数，拒绝缩容</li>
 * </ul>
 * <p>
 * 缩容硬约束（内容不丢）：成员减少时仅当 stored ≤ 新容量才应用新容量，否则保持
 * 当前容量直至清空。
 */
public final class SharedStorage {

    /** 物品频道固定槽位数：3×3 */
    public static final int ITEM_SLOTS = 9;
    /** 流体频道固定 tank 数 */
    public static final int FLUID_TANKS = 2;
    /** 能量频道固定单元数 */
    public static final int ENERGY_UNITS = 1;
    /** 单成员标准：每槽堆叠上限 */
    public static final int BASE_ITEM_STACK = 64;
    /** 单成员标准：tank 容量（mB） */
    public static final int BASE_FLUID_CAPACITY = 2000;
    /** 单成员标准：能量容量 = kFE(10) */
    public static final int BASE_ENERGY_CAPACITY = 10000;

    private final ChannelType type;
    private final MachineItemHandler itemHandler;
    private final List<MachineFluidTank> tanks = new ArrayList<>();
    private final MachineEnergyStorage energy;
    private int memberCount;
    private Runnable changeListener = () -> {};

    private SharedStorage(ChannelType type) {
        this.type = type;
        switch (type) {
            case ITEM -> {
                this.itemHandler = new MachineItemHandler(ITEM_SLOTS);
                this.energy = null;
            }
            case FLUID -> {
                for (int i = 0; i < FLUID_TANKS; i++) {
                    tanks.add(new MachineFluidTank(BASE_FLUID_CAPACITY));
                }
                this.itemHandler = null;
                this.energy = null;
            }
            case ENERGY -> {
                this.itemHandler = null;
                this.energy = new MachineEnergyStorage(BASE_ENERGY_CAPACITY, rate(BASE_ENERGY_CAPACITY), rate(BASE_ENERGY_CAPACITY));
            }
            default -> throw new IllegalStateException("Unsupported channel type: " + type);
        }
    }

    public static SharedStorage forItem() {
        return new SharedStorage(ChannelType.ITEM);
    }

    public static SharedStorage forFluid() {
        return new SharedStorage(ChannelType.FLUID);
    }

    public static SharedStorage forEnergy() {
        return new SharedStorage(ChannelType.ENERGY);
    }

    /** 能量速率与容量同源：max(capacity / 200, 1)。 */
    private static int rate(int capacity) {
        return Math.max(capacity / 200, 1);
    }

    // ───── 受控访问（按类型隔离，非法访问 fail-fast）─────

    public ChannelType type() {
        return type;
    }

    public MachineItemHandler getItemHandler() {
        if (type != ChannelType.ITEM) {
            throw new IllegalStateException("Not an item channel: " + type);
        }
        return itemHandler;
    }

    public List<MachineFluidTank> getTanks() {
        if (type != ChannelType.FLUID) {
            throw new IllegalStateException("Not a fluid channel: " + type);
        }
        return tanks;
    }

    public MachineEnergyStorage getEnergy() {
        if (type != ChannelType.ENERGY) {
            throw new IllegalStateException("Not an energy channel: " + type);
        }
        return energy;
    }

    public int memberCount() {
        return memberCount;
    }

    /**
     * 共享存储内容是否全空（物品槽全空 / 流体 tank 全空 / 能量 0）。
     * 频道删除的前置校验（{@link ChannelRegistry#remove}）：内容非空禁止删除（内容不丢硬约束）。
     */
    public boolean isEmpty() {
        switch (type) {
            case ITEM -> {
                for (int i = 0; i < ITEM_SLOTS; i++) {
                    if (!itemHandler.getStackInSlot(i).isEmpty()) {
                        return false;
                    }
                }
            }
            case FLUID -> {
                for (MachineFluidTank tank : tanks) {
                    if (tank.getFluidAmount() > 0) {
                        return false;
                    }
                }
            }
            case ENERGY -> {
                if (energy.getEnergyStored() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    // ───── 容量模型 ─────

    /**
     * 成员数变更后的容量重算入口（由 {@link ChannelRegistry#join}/{@link ChannelRegistry#leave} 调用）。
     * 容量=标准×max(1, 成员数)；成员减少时拒绝缩容（stored ≤ 新容量才应用）。
     */
    public void refreshMemberCount(int count) {
        this.memberCount = count;
        refreshCapacities();
    }

    private void refreshCapacities() {
        int n = Math.max(1, memberCount);
        switch (type) {
            case ITEM -> {
                int stackLimit = BASE_ITEM_STACK * n;
                // MachineItemHandler#getSlotLimit 内部对已有数量取 max，天然不缩容既有堆叠
                itemHandler.setDynamicSlotLimit((slot, stack) -> stackLimit);
            }
            case FLUID -> {
                int capacity = BASE_FLUID_CAPACITY * n;
                for (MachineFluidTank tank : tanks) {
                    if (tank.getFluidAmount() <= capacity) {
                        tank.setCapacity(capacity);
                    }
                }
            }
            case ENERGY -> {
                int capacity = BASE_ENERGY_CAPACITY * n;
                if (energy.getEnergyStored() <= capacity) {
                    energy.setCapacity(capacity);
                }
                energy.setMaxReceive(rate(capacity));
                energy.setMaxExtract(rate(capacity));
            }
            default -> throw new IllegalStateException("Unsupported channel type: " + type);
        }
        changeListener.run();
    }

    // ───── 内容变更 → 存档脏标记 ─────

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener != null ? listener : () -> {};
        // 按类型非空守卫：ITEM 无 energy、FLUID/ENERGY 无 itemHandler，仅对非空后端挂监听
        if (itemHandler != null) {
            itemHandler.setChangeListener(this.changeListener);
        }
        tanks.forEach(tank -> tank.setChangeListener(this.changeListener));
        if (energy != null) {
            energy.setChangeListener(this.changeListener);
        }
    }

    // ───── 持久化（Codec，经 SavedDataStorage 的 RegistryOps 编解码）─────

    /**
     * 懒加载 Codec：构建引用 {@link FluidStack#OPTIONAL_CODEC}，其类初始化依赖
     * Minecraft 运行时；在无 bootstrap 的纯 JUnit 测试环境中访问 SharedStorage
     * 实例（容量/回流契约）不应触发 Codec 构建。
     */
    private static Codec<SharedStorage> codecInstance;

    public static Codec<SharedStorage> codec() {
        if (codecInstance == null) {
            codecInstance = buildCodec();
        }
        return codecInstance;
    }

    private static Codec<SharedStorage> buildCodec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                ChannelType.CODEC.fieldOf("type").forGetter(SharedStorage::type),
                Codec.list(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(SharedStorage::encodeItems),
                Codec.list(FluidStack.OPTIONAL_CODEC).fieldOf("fluids").forGetter(SharedStorage::encodeFluids),
                Codec.INT.fieldOf("energy").forGetter(SharedStorage::encodeEnergy),
                Codec.INT.fieldOf("memberCount").forGetter(SharedStorage::memberCount)).apply(instance, SharedStorage::decode));
    }

    private List<ItemStack> encodeItems() {
        if (type != ChannelType.ITEM) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>(ITEM_SLOTS);
        for (int i = 0; i < ITEM_SLOTS; i++) {
            stacks.add(itemHandler.getStackInSlot(i));
        }
        return stacks;
    }

    private List<FluidStack> encodeFluids() {
        if (type != ChannelType.FLUID) {
            return List.of();
        }
        List<FluidStack> stacks = new ArrayList<>(FLUID_TANKS);
        for (MachineFluidTank tank : tanks) {
            stacks.add(tank.getFluid());
        }
        return stacks;
    }

    private int encodeEnergy() {
        return type == ChannelType.ENERGY && energy != null ? energy.getEnergyStored() : 0;
    }

    private static SharedStorage decode(ChannelType type, List<ItemStack> items,
                                        List<FluidStack> fluids, int energyStored, int memberCount) {
        SharedStorage storage = switch (type) {
            case ITEM -> forItem();
            case FLUID -> forFluid();
            case ENERGY -> forEnergy();
        };
        storage.memberCount = memberCount;
        storage.restore(items, fluids, energyStored);
        storage.refreshCapacities();
        return storage;
    }

    private void restore(List<ItemStack> items, List<FluidStack> fluids, int energyStored) {
        switch (type) {
            case ITEM -> {
                for (int i = 0; i < Math.min(items.size(), ITEM_SLOTS); i++) {
                    ItemStack stack = items.get(i);
                    if (stack != null && !stack.isEmpty()) {
                        itemHandler.setStackInSlot(i, stack.copy());
                    }
                }
            }
            case FLUID -> {
                for (int i = 0; i < Math.min(fluids.size(), FLUID_TANKS); i++) {
                    FluidStack stack = fluids.get(i);
                    if (stack != null && !stack.isEmpty()) {
                        tanks.get(i).setFluid(stack.copy());
                    }
                }
            }
            case ENERGY -> energy.setEnergy(Math.max(0, energyStored));
            default -> throw new IllegalStateException("Unsupported channel type: " + type);
        }
    }
}
