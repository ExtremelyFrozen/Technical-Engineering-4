package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.common.block.SpringBlock;
import com.modularmc.ten.common.data.TENDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpringBlockEntity extends CmBlockEntity {

    private static final int PRODUCE_INTERVAL = 20;

    /**
     * 内部储罐：不参与 LDLib2 字段同步，持久化/同步全部手动。
     */
    private final MachineFluidTank tank = new MachineFluidTank(0);

    private int amount = -1;
    private int capacity = -1;
    private int produceTimer = 0;
    private float item1 = -1;
    private float item2 = -1;
    private float fluid1 = -1;
    private float fluid2 = -1;


    private IFluidHandler outputOnlyHandler(Direction side) {
        boolean canIn = side == Direction.UP;
    return new IFluidHandler() {
        @Override public int getTanks() {
            return 1;
        }
        @Override public @NotNull FluidStack getFluidInTank (int t){
            return tank.getFluid();
        }
        @Override public int getTankCapacity(int t){
            return tank.getCapacity();
        }
        @Override public boolean isFluidValid(int t, FluidStack s){
            return false;
        }
        @Override public int fill(FluidStack r, FluidAction a){
            return 0;
        }
        @Override public FluidStack drain(FluidStack r, FluidAction a){
            return canIn ? tank.drain(r, a) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int m, FluidAction a){
            return canIn ?  tank.drain(m, a) : FluidStack.EMPTY;
        }
    }

    ;
}

    public SpringBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 罐内容变化 → markDirty → sendBlockUpdated → 客户端收到 update tag
        tank.setChangeListener(this::markDirty);
    }

    /**
     * 首次服务端 tick 时调用（基类 initialised 标记保证只调一次），
     * 此时若已从存档读入，capacity 已 >= 0；新放置则仍为 -1。
     */
    @Override
    protected void onPlaced() {
        if (level == null || level.isClientSide()) return;
        if (capacity <= 0) {
            RandomSource random = level.getRandom();
            capacity = random.nextIntBetweenInclusive(1, 8) * 1000;
            amount = random.nextIntBetweenInclusive(1, 8) * 10;
            item1 = random.nextIntBetweenInclusive(1, 40) / 10f;
            item2 = random.nextIntBetweenInclusive(1, 40) / 10f;
            fluid1 = random.nextIntBetweenInclusive(1, 40) / 10f;
            fluid2 = random.nextIntBetweenInclusive(1, 40) / 10f;
            tank.setCapacity(capacity);
            // setCapacity 不触发 changeListener，手动推一次让客户端拿到初始容量
            markDirty();
        }
    }

    private SpringBlock getBlock() {
        return (SpringBlock) getBlockState().getBlock();
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) return;
        if (tank.getCapacity() <= 0) return; // 容量尚未就绪（理论不会发生）
        produceTimer++;
        if (produceTimer >= PRODUCE_INTERVAL) {
            produceTimer = 0;
            Fluid fluid0 = getBlock().getFluid();
            if (tank.getSpace() == 0){
                if (getBlockState().getValue(SpringBlock.FACING_AIR)){
                    FluidStack fluid = new FluidStack(fluid0, 1000);
                    fluid.set(TENDataComponents.QUALITY.get(), new TENDataComponents.FluidQuality(item1, item2, fluid1, fluid2));
                    tank.drain(fluid, IFluidHandler.FluidAction.EXECUTE);
                    level.setBlock(worldPosition.above(), getBlock().getFluid().defaultFluidState().createLegacyBlock(), 3);
                }
                return;
            }
            FluidStack fluid = new FluidStack(fluid0, amount);
            fluid.set(TENDataComponents.QUALITY.get(), new TENDataComponents.FluidQuality(item1, item2, fluid1, fluid2));
            tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** 供 RegisterCapabilitiesEvent 注册；只出不进。 */
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return outputOnlyHandler(side);
    }

    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        amount = tag.getInt("amount");
        capacity = tag.getInt("capacity");
        produceTimer = tag.getInt("produceTimer");
        item1 = tag.getFloat("item1");
        item2 = tag.getFloat("item2");
        fluid1 = tag.getFloat("fluid1");
        fluid2 = tag.getFloat("fluid2");
        // 空 tag 守卫：LDLib2 GUI 空同步 / 无关更新包可能带来空 tag，
        // 直接 readFromNBT 会把客户端罐清空
        tank.setCapacity(capacity);
        CompoundTag tankTag = tag.getCompound("tank");
        if (!tankTag.isEmpty()) {
            tank.readFromNBT(registries, tankTag);
        }
    }

    @Override
    protected void writeTileData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("amount", amount);
        tag.putInt("capacity", capacity);
        tag.putInt("produceTimer", produceTimer);
        tag.putFloat("item1", item1);
        tag.putFloat("item2", item2);
        tag.putFloat("fluid1", fluid1);
        tag.putFloat("fluid2", fluid2);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }
}
