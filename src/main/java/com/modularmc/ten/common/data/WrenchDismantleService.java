package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 扳手拆解服务（1.21.1 适配版：CompoundTag 替代 26.1.2 的 TagValueOutput）。
 * <p>
 * 事务顺序：快照 → fireBlockBreak → 编码 NBT → extractItem → destroy → 掉落物。
 * 任何关键步骤失败则恢复原方块。
 */
public final class WrenchDismantleService {

    private WrenchDismantleService() {}

    private record MachineSnapshot(
            BlockState state, List<ItemStack> inventorySnap, List<ItemStack> upgradeSnap,
            int energy, List<FluidStack> tanks,
            int[] energyFaceData, int[] itemFaceData, int[] fluidFaceData,
            int redstoneMode, int facingVal, boolean active, int upgradeSize) {}

    public static boolean dismantle(Level level, BlockPos pos, ServerPlayer player) {
        if (level.isClientSide()) return false;

        BlockState state = level.getBlockState(pos);
        if (player.distanceToSqr(pos.getCenter()) > 81.0) return false;
        if (!player.getAbilities().mayBuild && !player.isCreative()) return false;

        BlockEntity be = level.getBlockEntity(pos);
        Block block = state.getBlock();

        // 1. 快照（只读）
        MachineSnapshot snapshot = null;
        boolean extracted = false;

        if (be instanceof CmMachineBlockEntity machine) {
            machine.initMachine();
            List<ItemStack> invSnap = new ArrayList<>();
            if (machine.itemHandler != null)
                for (int i = 0; i < machine.itemHandler.getSlots(); i++)
                    invSnap.add(machine.itemHandler.getStackInSlot(i).copy());

            List<ItemStack> upgSnap = new ArrayList<>();
            if (machine.upgradeHandler != null)
                for (int i = 0; i < machine.upgradeHandler.getSlots(); i++)
                    upgSnap.add(machine.upgradeHandler.getStackInSlot(i).copy());

            int energy = machine.energyStorage != null ? machine.energyStorage.getEnergyStored() : 0;
            List<FluidStack> tankSnap = new ArrayList<>();
            for (var tank : machine.tanks) tankSnap.add(tank.getFluid().copy());

            int[] energyFace = new int[6], itemFace = new int[6], fluidFace = new int[6];
            for (Direction dir : Direction.values()) {
                int idx = dir.get3DDataValue();
                energyFace[idx] = machine.energyFaceMode.getOrDefault(dir, machine.initialFaceModeEnergy());
                itemFace[idx] = machine.itemFaceMode.getOrDefault(dir, machine.initialFaceModeItem());
                fluidFace[idx] = machine.fluidFaceMode.getOrDefault(dir, machine.initialFaceModeFluid());
            }
            snapshot = new MachineSnapshot(state, invSnap, upgSnap, energy, tankSnap,
                    energyFace, itemFace, fluidFace, machine.redstoneMode, machine.facingVal, machine.active, machine.upgradeSize);
        }

        // 2. fireBlockBreak（未改变任何状态）
        if (CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled())
            return false;

        // 3. 编码配置数据到 NBT（1.21.1 用 CompoundTag，无 TagValueOutput）
        CompoundTag preEncoded = null;
        if (be instanceof CmMachineBlockEntity machine && snapshot != null) {
            HolderLookup.Provider registries = level.registryAccess();
            preEncoded = new CompoundTag();
            preEncoded.putInt("energy", snapshot.energy);
            for (int i = 0; i < snapshot.tanks.size(); i++) {
                // 与 CmMachineBlockEntity.saveSerializedHandlers 的 tank 格式一致（readFromNBT 可解析）
                CompoundTag tankTag = new CompoundTag();
                FluidStack fs = snapshot.tanks.get(i);
                if (fs != null && !fs.isEmpty()) {
                    tankTag.put("Fluid", fs.save(registries));
                }
                preEncoded.put("tank" + i, tankTag);
            }
            for (Direction dir : Direction.values()) {
                int idx = dir.get3DDataValue();
                preEncoded.putInt("direEnergy" + idx, snapshot.energyFaceData[idx]);
                preEncoded.putInt("direItem" + idx, snapshot.itemFaceData[idx]);
                preEncoded.putInt("direFluid" + idx, snapshot.fluidFaceData[idx]);
            }
            preEncoded.putInt("upgrade_size", snapshot.upgradeSize);
            // 1.21.1 readTileData 不读 redstoneMode/facingVal/active，但存着无妨
            preEncoded.putInt("redstoneMode", snapshot.redstoneMode);
            preEncoded.putInt("facingVal", snapshot.facingVal);
            preEncoded.putBoolean("active", snapshot.active);
        }

        // 4. playerWillDestroy
        block.playerWillDestroy(level, pos, state, player);

        // 5. 提取物品/升级（实际移动）
        List<ItemStack> inventoryDrops = new ArrayList<>();
        List<ItemStack> upgradeDrops = new ArrayList<>();
        if (be instanceof CmMachineBlockEntity machine) {
            if (machine.itemHandler != null)
                for (int i = 0; i < machine.itemHandler.getSlots(); i++) {
                    ItemStack s = machine.itemHandler.extractItem(i, Integer.MAX_VALUE, false);
                    if (!s.isEmpty()) inventoryDrops.add(s);
                }
            if (machine.upgradeHandler != null)
                for (int i = 0; i < machine.upgradeHandler.getSlots(); i++) {
                    ItemStack s = machine.upgradeHandler.extractItem(i, Integer.MAX_VALUE, false);
                    if (!s.isEmpty()) upgradeDrops.add(s);
                }
            extracted = true;
        }

        // 6. onDestroyedByPlayer（1.21.1 无 6 参重载，用 5 参：Level, BlockPos, Player, canHarvest, FluidState）
        boolean removed = state.onDestroyedByPlayer(level, pos, player,
                state.canHarvestBlock(level, pos, player), level.getFluidState(pos));
        if (!removed) {
            if (be instanceof CmMachineBlockEntity m && snapshot != null)
                restoreMachine(m, snapshot, extracted);
            return false;
        }

        // 7. block.destroy
        block.destroy(level, pos, state);

        // 8. 构建掉落物
        List<ItemStack> drops = new ArrayList<>();
        if (be instanceof CmMachineBlockEntity machine && snapshot != null && preEncoded != null) {
            ItemStack blockStack = new ItemStack(block);
            BlockItem.setBlockEntityData(blockStack, machine.getType(), preEncoded);
            drops.add(blockStack);
            drops.addAll(inventoryDrops);
            drops.addAll(upgradeDrops);
        } else {
            drops.add(new ItemStack(block));
        }

        // 9. 合并后交付
        for (ItemStack stack : consolidateDrops(drops)) {
            if (stack.isEmpty()) continue;
            if (!player.addItem(stack)) {
                var drop = stack.copy();
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), drop, 0, 0, 0));
            }
        }
        return true;
    }

    private static List<ItemStack> consolidateDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            ItemStack source = stack.copy();
            if (!source.isStackable()) {
                for (int i = 0; i < source.getCount(); i++) {
                    ItemStack single = source.copy(); single.setCount(1); result.add(single);
                }
                continue;
            }
            while (!source.isEmpty()) {
                for (ItemStack existing : result) {
                    if (existing.isEmpty()) continue;
                    if (!ItemStack.isSameItemSameComponents(existing, source)) continue;
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space <= 0) continue;
                    int toTransfer = Math.min(space, source.getCount());
                    existing.setCount(existing.getCount() + toTransfer);
                    source.setCount(source.getCount() - toTransfer);
                    if (source.isEmpty()) break;
                }
                if (source.isEmpty()) break;
                if (source.getCount() > source.getMaxStackSize()) {
                    ItemStack full = source.copy(); full.setCount(source.getMaxStackSize());
                    result.add(full); source.setCount(source.getCount() - source.getMaxStackSize());
                } else {
                    result.add(source); break;
                }
            }
        }
        return result;
    }

    private static void restoreMachine(CmMachineBlockEntity machine, MachineSnapshot snap, boolean wasExtracted) {
        try {
            machine.initMachine();
            if (wasExtracted && machine.itemHandler != null)
                for (int i = 0; i < Math.min(snap.inventorySnap.size(), machine.itemHandler.getSlots()); i++)
                    machine.itemHandler.setStackInSlot(i, snap.inventorySnap.get(i).copy());
            if (wasExtracted && machine.upgradeHandler != null)
                for (int i = 0; i < Math.min(snap.upgradeSnap.size(), machine.upgradeHandler.getSlots()); i++)
                    machine.upgradeHandler.setStackInSlot(i, snap.upgradeSnap.get(i).copy());
            if (machine.energyStorage != null) machine.energyStorage.setEnergy(snap.energy);
            for (int i = 0; i < Math.min(snap.tanks.size(), machine.tanks.size()); i++)
                machine.tanks.get(i).setFluid(snap.tanks.get(i).copy());
            for (Direction dir : Direction.values()) {
                int idx = dir.get3DDataValue();
                machine.energyFaceMode.put(dir, snap.energyFaceData[idx]);
                machine.itemFaceMode.put(dir, snap.itemFaceData[idx]);
                machine.fluidFaceMode.put(dir, snap.fluidFaceData[idx]);
            }
            machine.redstoneMode = snap.redstoneMode;
            machine.facingVal = snap.facingVal;
            machine.active = snap.active;
            machine.upgradeSize = snap.upgradeSize;
            machine.setChanged();
        } catch (Exception e) {
            TEN.LOGGER.error("Failed to restore machine at {} after failed dismantle", machine.getBlockPos(), e);
        }
    }
}