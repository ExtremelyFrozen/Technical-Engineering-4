package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;
import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupKnow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.Objects;
import java.util.Optional;

/**
 * 熔炼机：三条互相独立的并行流水线（lane）。
 *
 * <p>
 * 槽位约定：{@code 0..2} 为输入槽，{@code 3..5} 为输出槽；lane i 独占输入槽 i、输出槽
 * {@code i + LANES} 与一条进度箭头。输入槽的索引顺序就是输入优先级：slot 0 画在中间，
 * 因此快捷移动、主动拉取与外部 capability 的按序插入都会优先进中间输入槽。
 * 视觉上三条 lane 上下堆叠、间隔 0px，行 y 见 {@link #LANE_SLOT_Y}。
 *
 * <p>
 * 每条 lane 自持进度、配方身份与批处理锁，逐 tick 各自推进；能量与 XP 罐为三路共享，
 * 按 lane 顺序扣减。基类 {@code progress}/{@code maxProgress} 只作单进度镜像，
 * 供冷却器等横切逻辑读取，不再驱动本机加工。
 */
public class FurnaceBlockEntity extends ProcessingMachineBlockEntity {

    /** 并行流水线数量：每条 lane 独占一个输入槽、一个输出槽与一条进度箭头。 */
    public static final int LANES = 3;

    /** 三条 lane 的槽位 y：lane 0 居中（沿用旧输入槽位置），1 在上、2 在下。 */
    public static final int[] LANE_SLOT_Y = { 32, 14, 50 };

    /** XP 流体输出罐容量（恒存在）。 */
    private static final int XP_TANK_CAPACITY = 4000;

    /** XP 流体换算：每 mB 对应 tick 数（0.1 mB/tick）。 */
    private static final int XP_FLUID_TICKS_PER_MB = 10;

    // ───── 三条 lane 的进度镜像（GUI 三条箭头分别读其中一个，必须 @DescSynced）─────
    @Persisted
    @DescSynced
    public int laneProgress0 = 0;
    @Persisted
    @DescSynced
    public int laneProgress1 = 0;
    @Persisted
    @DescSynced
    public int laneProgress2 = 0;
    @Persisted
    @DescSynced
    public int laneMaxProgress0 = 0;
    @Persisted
    @DescSynced
    public int laneMaxProgress1 = 0;
    @Persisted
    @DescSynced
    public int laneMaxProgress2 = 0;

    /** 各 lane 的批处理锁与配方身份：服务端权威、不持久化，读档后由 conditionStart 重建。 */
    private final int[] laneB = new int[LANES];
    private final int[] laneLockedMaxProgress = new int[LANES];
    private final ResourceLocation[] lastRecipeId = new ResourceLocation[LANES];

    public FurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
        tanks.add(new MachineFluidTank(XP_TANK_CAPACITY)); // XP 输出罐（恒存在，三路共享）
    }

    public static int inputSlot(int lane) {
        return lane;
    }

    public static int outputSlot(int lane) {
        return lane + LANES;
    }

    @Override
    public int machineType() {
        return MachineType.FURNACE;
    }

    @Override
    public int inventorySize() {
        return 2 * LANES;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot >= 0 && slot < LANES) return IngredientType.INPUT;
        if (slot >= LANES && slot < 2 * LANES) return IngredientType.OUTPUT;
        return IngredientType.IGNORE;
    }

    /**
     * 三条并行 lane 允许同款原料分占多个输入槽；熔炼机走原版单物品配方，
     * 不存在 FormsCombinedRecipe 那种「占用槽数决定配方」的匹配歧义。
     */
    @Override
    protected boolean enforcesUniqueItemSlots() {
        return false;
    }

    /** XP 输出罐：可经桶/管道取出（canOut），禁注入（canIn）。 */
    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    /** 基础周期 tick：配方 cookingTime 全值（旧版误除 2 导致处理速度快一倍，勿回退）。 */
    @Override
    public int baseTickTime() {
        return getCurrentRecipe(0).map(r -> r.value().getCookingTime()).orElse(200);
    }

    /** 指定 lane 的进度比例，供 GUI 的对应箭头绑定。 */
    public double laneProgressPercent(int lane) {
        int max = laneMaxProgress(lane);
        return max <= 0 ? 0 : Math.min(1.0, (double) laneProgress(lane) / max);
    }

    private int laneProgress(int lane) {
        return switch (lane) {
            case 0 -> laneProgress0;
            case 1 -> laneProgress1;
            default -> laneProgress2;
        };
    }

    private void setLaneProgress(int lane, int value) {
        switch (lane) {
            case 0 -> laneProgress0 = value;
            case 1 -> laneProgress1 = value;
            default -> laneProgress2 = value;
        }
    }

    private int laneMaxProgress(int lane) {
        return switch (lane) {
            case 0 -> laneMaxProgress0;
            case 1 -> laneMaxProgress1;
            default -> laneMaxProgress2;
        };
    }

    private void setLaneMaxProgress(int lane, int value) {
        switch (lane) {
            case 0 -> laneMaxProgress0 = value;
            case 1 -> laneMaxProgress1 = value;
            default -> laneMaxProgress2 = value;
        }
    }

    private static String laneProgressField(int lane) {
        return switch (lane) {
            case 0 -> "laneProgress0";
            case 1 -> "laneProgress1";
            default -> "laneProgress2";
        };
    }

    private static String laneMaxProgressField(int lane) {
        return switch (lane) {
            case 0 -> "laneMaxProgress0";
            case 1 -> "laneMaxProgress1";
            default -> "laneMaxProgress2";
        };
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 三输入槽：18x18 小槽上下堆叠、间隔 0px；slot 0 在中间，注册顺序即快捷移动优先级。
            // 三输出槽：同样的小槽与输入槽逐行对齐（lane i 的输入/输出在同一行）。
            for (int lane = 0; lane < LANES; lane++) {
                root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, inputSlot(lane), 39, LANE_SLOT_Y[lane]));
            }
            for (int lane = 0; lane < LANES; lane++) {
                root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, outputSlot(lane), 113, LANE_SLOT_Y[lane]));
            }
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // 三条进度箭头上下堆叠，与各自 lane 的输入槽同高（22x16 在 18px 行内上下各留 1px）。
            // 每条只读对应 lane 的进度，不受其他两条 lane 影响。
            for (int lane = 0; lane < LANES; lane++) {
                final int index = lane;
                root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 74, LANE_SLOT_Y[lane] + 1, true,
                        m -> ((FurnaceBlockEntity) m).laneProgressPercent(index)));
            }
            // XP 槽仍在最右侧：输出小槽右缘 131，XP 槽左缘 143（间隔 12），与主区垂直对齐 y=17。
            root.addChild(TENMachineBlockUIFactory.createXpFluidSlot(this, 143, 17, 18, 50));
        });
    }

    /**
     * 按当前配方模式取指定 lane 输入槽的配方：
     * recipeMode 由 LevelupBlast/LevelupSmoke 升级决定（First-wins 语义，见 setRecipeMode）：
     * SMELTING（默认）/ BLASTING / SMOKING。
     */
    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<AbstractCookingRecipe>> getCurrentRecipe(int lane) {
        if (level == null || itemHandler == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(inputSlot(lane));
        if (input.isEmpty()) return Optional.empty();
        var recipeInput = new SingleRecipeInput(input);

        return switch (recipeMode) {
            case IUpgradableMachine.RECIPE_MODE_BLASTING -> (Optional) level.getRecipeManager()
                    .getRecipeFor(RecipeType.BLASTING, recipeInput, level);
            case IUpgradableMachine.RECIPE_MODE_SMOKING -> (Optional) level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMOKING, recipeInput, level);
            default -> (Optional) level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        };
    }

    @Override
    public void doBaseData() {
        super.doBaseData();
        // 三条 lane 同时运行的峰值能耗是单路的 LANES 倍，信息栏按真实峰值显示。
        energyUseMax = safeMultiply(energyUseMax, LANES);
        markDirty("energyUseMax");
    }

    /** 不复用基类的单流水线 process()，改为逐 lane 独立推进。 */
    @Override
    public void tick() {
        doBaseData();
        processLanes();
    }

    // 无参钩子保留中间 lane 的语义，供仍按单流水线读取的外部代码使用。
    @Override
    public boolean conditionStart() {
        return conditionStart(0);
    }

    @Override
    public boolean cooking() {
        return cooking(0);
    }

    @Override
    public void onCookFinish() {
        onCookFinish(0);
    }

    private void processLanes() {
        if (level != null && level.isClientSide()) return;

        // Syn 光合注能在所有门禁之前执行一次（补的是同一份储能，与 lane 数无关）。
        tryInjectPhotosynEnergy();

        boolean signal = signalAllowRun();
        boolean anyRunning = false;
        for (int lane = 0; lane < LANES; lane++) {
            boolean start = conditionStart(lane);
            if (!start || !signal) {
                // 原料消失 / 配方失配：作废本 lane 的进度；缺信号属停滞，保留进度等待恢复。
                if (!start && laneProgress(lane) > 0) {
                    setLaneProgress(lane, 0);
                }
                continue;
            }

            long expected = Math.round((double) getActualEfficiency() * Math.max(1, laneB[lane]));
            if (expected <= 0 || expected > Integer.MAX_VALUE) continue;
            int fePerTick = (int) expected;

            // 缺能、输出满：保留进度，等待恢复（与基类停滞语义一致）。
            if (energyStorage == null || energyStorage.getEnergyStored() < fePerTick) continue;
            if (cooking(lane)) continue;

            if (laneLockedMaxProgress[lane] <= 0) {
                int mp = Math.max(1, laneMaxProgress(lane));
                setLaneMaxProgress(lane, mp);
                laneLockedMaxProgress[lane] = mp;
            }

            if (energyStorage.extractEnergy(fePerTick, true) != fePerTick) continue;
            if (energyStorage.extractEnergy(fePerTick, false) != fePerTick) {
                throw new IllegalStateException(
                        "Energy under-extraction on furnace lane " + lane + ": expected " + fePerTick + " FE.");
            }
            recordEnergyUse(fePerTick);

            anyRunning = true;
            setLaneProgress(lane, laneProgress(lane) + 1);
            if (laneProgress(lane) >= laneMaxProgress(lane)) {
                onCookFinish(lane);
                setLaneProgress(lane, 0);
                laneB[lane] = 0;
                laneLockedMaxProgress[lane] = 0;
            }
        }
        setActive(anyRunning);
        syncLaneData();
    }

    /**
     * 每 tick 把三条 lane 的进度写入客户端镜像，并维护基类的单进度镜像：
     * 取第一条在跑的 lane，供冷却器的 conditionStart / maxProgress 判定使用。
     */
    private void syncLaneData() {
        for (int lane = 0; lane < LANES; lane++) {
            markDirty(laneProgressField(lane));
            markDirty(laneMaxProgressField(lane));
        }
        int mirror = -1;
        for (int lane = 0; lane < LANES; lane++) {
            if (laneMaxProgress(lane) > 0 && laneB[lane] > 0) {
                mirror = lane;
                break;
            }
        }
        progress = mirror < 0 ? 0 : laneProgress(mirror);
        maxProgress = mirror < 0 ? 0 : laneMaxProgress(mirror);
        markDirty("progress");
        markDirty("maxProgress");
    }

    /** 外部推进进度（冷却器）：本机三路同时受益，不落在单一 lane 上。 */
    @Override
    public void advanceProgress(int ticks) {
        if (ticks <= 0) return;
        for (int lane = 0; lane < LANES; lane++) {
            int max = laneMaxProgress(lane);
            if (max <= 0 || laneB[lane] <= 0) continue;
            setLaneProgress(lane, Math.min(max, laneProgress(lane) + ticks));
        }
        syncLaneData();
        setChanged();
    }

    private boolean conditionStart(int lane) {
        var recipeOpt = getCurrentRecipe(lane);
        if (recipeOpt.isEmpty()) {
            lastRecipeId[lane] = null;
            setLaneProgress(lane, 0);
            setLaneMaxProgress(lane, 0);
            laneB[lane] = 0;
            laneLockedMaxProgress[lane] = 0;
            return false;
        }

        AbstractCookingRecipe recipe = recipeOpt.get().value();
        ResourceLocation newId = recipeOpt.get().id();
        boolean identityChanged = !Objects.equals(lastRecipeId[lane], newId);
        if (identityChanged) {
            setLaneProgress(lane, 0);
            laneB[lane] = 0;
            laneLockedMaxProgress[lane] = 0;
        }
        lastRecipeId[lane] = newId;

        if (identityChanged || laneB[lane] <= 0) {
            int mp = Math.max(1, (int) Math.ceil(recipe.getCookingTime() * durationMultiplier));
            setLaneMaxProgress(lane, mp);
            laneLockedMaxProgress[lane] = mp;

            int B_theory = 1 + batch;
            ItemStack input = itemHandler.getStackInSlot(inputSlot(lane));
            var registry = level.registryAccess();
            ItemStack result = recipe.getResultItem(registry);
            int B_byInput = input.getCount();

            // 输出容量按「物品上限 × 理论 B」的动态槽位上限算，不按原版 64 封顶
            // （旧写法在非空槽上用 getMaxStackSize()，会让输出槽永远停在 64）。
            int outSlot = outputSlot(lane);
            ItemStack existingOutput = itemHandler.getStackInSlot(outSlot);
            int availOutputSpace;
            if (existingOutput.isEmpty()) {
                availOutputSpace = itemHandler.getEffectiveSlotLimit(outSlot, result);
            } else if (ItemStack.isSameItem(existingOutput, result)) {
                availOutputSpace = itemHandler.getEffectiveSlotLimit(outSlot, existingOutput) - existingOutput.getCount();
            } else {
                availOutputSpace = 0;
            }
            int resultCount = result.getCount();
            int B_byOutput = resultCount > 0 ? availOutputSpace / resultCount : 0;

            int baseFe = Math.max(1, getActualEfficiency());
            int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFe : 0;

            // Knowledge 门控 XP 产出：安装时 B 受 XP 罐剩余空间约束（罐三路共享）。
            int B_byXpFluid = Integer.MAX_VALUE;
            if (hasUpgrade(LevelupKnow.class)) {
                int xpPerUnit = calculateXpFluidPerUnit(recipe.getCookingTime());
                if (xpPerUnit > 0 && !tanks.isEmpty()) {
                    int availXp = tanks.get(0).getCapacity() - tanks.get(0).getFluidAmount();
                    B_byXpFluid = availXp / xpPerUnit;
                } else if (xpPerUnit > 0) {
                    B_byXpFluid = 0;
                }
            }

            int B = calculateBActual(B_theory, B_byInput, B_byXpFluid, B_byOutput, B_byEnergy);
            if (B <= 0) {
                laneB[lane] = 0;
                return false;
            }
            laneB[lane] = Math.min(B, 19);
        }
        return true;
    }

    /** 返回 true 表示本 lane 的输出侧阻塞，应保留进度停滞。 */
    private boolean cooking(int lane) {
        var recipeOpt = getCurrentRecipe(lane);
        if (recipeOpt.isEmpty()) return false;
        var registry = level != null ? level.registryAccess() : null;
        if (registry == null) return false;

        int B = Math.max(1, laneB[lane]);
        AbstractCookingRecipe recipe = recipeOpt.get().value();
        ItemStack result = recipe.getResultItem(registry);
        int outSlot = outputSlot(lane);
        ItemStack output = itemHandler.getStackInSlot(outSlot);

        if (output.isEmpty()) return false;
        if (!ItemStack.isSameItem(output, result)) return true;
        if ((long) output.getCount() + (long) result.getCount() * B > itemHandler.getEffectiveSlotLimit(outSlot, output)) return true;

        // Knowledge 门控 XP 罐空间检查（仅生产路径）
        if (hasUpgrade(LevelupKnow.class)) {
            int xpPerUnit = calculateXpFluidPerUnit(recipe.getCookingTime());
            if (xpPerUnit > 0 && !tanks.isEmpty()) {
                FluidStack xpFluid = xpFluidStack(xpPerUnit);
                int filled = tanks.get(0).fill(xpFluid, IFluidHandler.FluidAction.SIMULATE);
                if (filled < xpPerUnit) return true;
            }
        }
        return false;
    }

    private void onCookFinish(int lane) {
        var recipeOpt = getCurrentRecipe(lane);
        if (recipeOpt.isEmpty() || level == null) return;

        if (!Objects.equals(lastRecipeId[lane], recipeOpt.get().id())) {
            laneB[lane] = 0;
            return;
        }

        int B = Math.max(1, laneB[lane]);
        AbstractCookingRecipe recipe = recipeOpt.get().value();
        var registry = level.registryAccess();
        ItemStack result = recipe.getResultItem(registry);
        int inSlot = inputSlot(lane);
        int outSlot = outputSlot(lane);
        ItemStack input = itemHandler.getStackInSlot(inSlot);
        ItemStack output = itemHandler.getStackInSlot(outSlot);

        if (input.getCount() < B) {
            laneB[lane] = 0;
            return;
        }

        long totalResult = (long) result.getCount() * B;
        if (totalResult > Integer.MAX_VALUE) {
            laneB[lane] = 0;
            return;
        }
        int resultCount = (int) totalResult;

        if (!output.isEmpty()) {
            if (!ItemStack.isSameItem(output, result)) {
                laneB[lane] = 0;
                return;
            }
            int outputLimit = itemHandler.getEffectiveSlotLimit(outSlot, output);
            if ((long) output.getCount() + resultCount > outputLimit) {
                return;
            }
        }

        // Knowledge 门控 XP 产出：执行前预检 XP 罐空间（不足则本周期停滞不消耗输入）
        boolean hasKnowledge = hasUpgrade(LevelupKnow.class);
        int xpFluidTotal = 0;
        if (hasKnowledge) {
            int xpPerUnit = calculateXpFluidPerUnit(recipe.getCookingTime());
            if (xpPerUnit > 0) {
                long totalXpLong = (long) xpPerUnit * B;
                if (totalXpLong > Integer.MAX_VALUE) {
                    laneB[lane] = 0;
                    return;
                }
                xpFluidTotal = (int) totalXpLong;
                if (!tanks.isEmpty()) {
                    FluidStack totalXpFluid = xpFluidStack(xpFluidTotal);
                    int filled = tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.SIMULATE);
                    if (filled < xpFluidTotal) {
                        return;
                    }
                } else {
                    laneB[lane] = 0;
                    return;
                }
            }
        }

        // ── 原子执行 + 回滚（只碰本 lane 的两个槽，XP 罐三路共享）──
        ItemStack inputSnapshot = itemHandler.getStackInSlot(inSlot).copy();
        ItemStack outputSnapshot = itemHandler.getStackInSlot(outSlot).copy();
        FluidStack tankSnapshot = (hasKnowledge && !tanks.isEmpty()) ? tanks.get(0).getFluid().copy() : FluidStack.EMPTY;

        try {
            itemHandler.extractItem(inSlot, B, false);

            if (output.isEmpty()) {
                ItemStack batchResult = result.copy();
                batchResult.setCount(resultCount);
                itemHandler.setStackInSlot(outSlot, batchResult);
            } else {
                output.grow(resultCount);
            }

            if (hasKnowledge && xpFluidTotal > 0 && !tanks.isEmpty()) {
                tanks.get(0).fill(xpFluidStack(xpFluidTotal), IFluidHandler.FluidAction.EXECUTE);
            }
        } catch (Exception e) {
            itemHandler.setStackInSlot(inSlot, inputSnapshot);
            itemHandler.setStackInSlot(outSlot, outputSnapshot);
            if (hasKnowledge && !tanks.isEmpty()) {
                tanks.get(0).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                if (!tankSnapshot.isEmpty()) {
                    tanks.get(0).fill(tankSnapshot, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            throw new RuntimeException("Furnace lane " + lane + " onCookFinish failed and rolled back", e);
        }
    }

    /** 每单位产物产出的 XP mB：max(1, round(cookingTime / 10))。 */
    private static int calculateXpFluidPerUnit(int cookingTime) {
        if (cookingTime <= 0) return 0;
        return Math.max(1, (int) Math.round((double) cookingTime / XP_FLUID_TICKS_PER_MB));
    }

    private static FluidStack xpFluidStack(int amount) {
        return new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_XP.getSource(), amount);
    }

    @Override
    public boolean hasFaceCapabilityFluid(Direction side) {
        // 罐与 capability 恒存在——Knowledge 仅门控生产逻辑。
        // 保证卸下 Knowledge 后残留 XP 流体仍可取出。
        return true;
    }

    /**
     * 旧档 2 槽 → 6 槽迁移：旧输入槽 0 落在新的中间输入槽（slot 0），
     * 旧输出槽 1 落在新的中间输出槽（slot 3），避免旧产物被塞进上排输入槽。
     * 其余字段沿用基类读档逻辑（进度与运行锁一并作废，下周期重建）。
     */
    @Override
    protected void readTileData(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag patched = tag;
        CompoundTag invTag = tag.getCompound("inventory");
        int savedSize = invTag.contains("Size") ? invTag.getInt("Size") : 0;
        if (!invTag.isEmpty() && savedSize == 2 && itemHandler != null && itemHandler.getSlots() == 2 * LANES) {
            patched = tag.copy();
            CompoundTag remapped = invTag.copy();
            remapped.putInt("Size", 2 * LANES);
            ListTag items = remapped.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag item = items.getCompound(i);
                int oldSlot = item.getInt("Slot");
                // 无效槽写成 -1，deserializeNBT 会跳过；不改动原 tag。
                item.putInt("Slot", oldSlot == 0 ? 0 : oldSlot == 1 ? outputSlot(0) : -1);
            }
            patched.put("inventory", remapped);
        }
        super.readTileData(patched, registries);
    }
}
