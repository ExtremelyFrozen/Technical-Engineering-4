package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.List;

public class MobRipBlockEntity extends RadiusMachineBlockEntity {

    public MobRipBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
        initialRadius = 4; // 9x9 基础（同种植站），radius 升级在此基础上扩展（26.1.2 对齐）
        radius = 4;
    }

    @Override
    public int machineType() {
        return MachineType.MOB_RIPPER;
    }

    @Override
    public int inventorySize() {
        // 26.1.2 对齐：槽 0 武器输入、槽 1..12 掉落输出（与 createUI 13 槽布局配套）
        return 13;
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot == 0 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        // 槽 0 为武器输入：任何带 TOOL 组件的物品（26.1.2 对齐，原 TieredItem/SwordItem 判定过窄）
        if (slot == 0) {
            return stack.has(DataComponents.TOOL);
        }
        return true;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        // 布局与 Quarry 逐坐标对齐（用户确认两者基本一致）：
        // 输入槽 (43,34)、12 输出槽 3×4 网格 (79/97/115/133 × 16/34/52)。
        // 输入槽中心 y=43 与输出网格第二行中心对齐，x 间距 18 不重叠。
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 79, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 133, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 5, 79, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 6, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 7, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 8, 133, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 9, 79, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 11, 115, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 12, 133, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // 输出网格底 y=70，进度条 y=74（间距 4）避免贴边；中心 x=88 与输出列中心对齐
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 74, true));
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this));
            root.addChild(TENMachineBlockUIFactory.useEnchantmentsToggleButton(this, 48, 48));
        });
    }

    /**
     * 生效范围：同农场背面方形（宽轴 ±radius、深轴 2*radius+1，
     * 机器位于工作区边缘中点、不含机器本身），差异点：Y = 机器层向上额外 2 格
     * （[my, my+3)，3 层，用户设定）。
     * 与 applyEffect 查询框 mobRipRangeAABB 一致。
     */
    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        // 预览用 previewRadius（服务端权威镜像 @DescSynced，旧存档 initialRadius 毒化由
        // 去 @Persisted + 每 tick 重置自愈解决）——见 RadiusMachineBlockEntity.displayRadius
        return List.of(mobRipRangeAABB(previewRadius()));
    }

    /**
     * 生物啃噬者工作范围 AABB：同农场背面方形（宽轴 ±radius、深轴 2*radius+1，
     * 机器位于边缘中点），Y = 机器层向上额外 2 格（[my, my+3)，共 3 层）——
     * 覆盖作物层与地表生物身高（用户设定，非 26.1.2 的 7 层语义）。
     */
    /** @param radiusValue 半径数据源：服务端实际伤害用 {@code radius}，预览线框用 {@code previewRadius()} */
    private AABB mobRipRangeAABB(int radiusValue) {
        Direction facing = getFacing();
        int mx = worldPosition.getX(), my = worldPosition.getY(), mz = worldPosition.getZ();
        int r = Math.max(0, radiusValue);
        int depth = 2 * r + 1;
        int minX, maxX, minZ, maxZ;
        switch (facing) {
            case NORTH -> {
                minX = mx - r;
                maxX = mx + r + 1;
                minZ = mz + 1;
                maxZ = mz + 1 + depth;
            }
            case SOUTH -> {
                minX = mx - r;
                maxX = mx + r + 1;
                minZ = mz - depth;
                maxZ = mz;
            }
            case EAST -> {
                minX = mx - depth;
                maxX = mx;
                minZ = mz - r;
                maxZ = mz + r + 1;
            }
            default -> {
                minX = mx + 1;
                maxX = mx + 1 + depth;
                minZ = mz - r;
                maxZ = mz + r + 1;
            }
        }
        return new AABB(minX, my, minZ, maxX, my + 3, maxZ);
    }

    @Override
    public void applyEffect() {
        if (level == null) return;
        int B = getLockedBatchSize();

        // 记录周期开始时武器是否存在：若原本有而中途损坏/耗尽则停止剩余批次，
        // 不回退到徒手伤害；从未有武器则保留原语义（允许无武器运行）。
        boolean weaponWasPresentAtStart = hasValidWeapon();

        for (int i = 0; i < B; i++) {
            if (weaponWasPresentAtStart && !hasValidWeapon()) {
                break;
            }
            // 无有效目标时继续循环——实体可被重复选中
            tryHurtOneEntity();
        }
    }

    /** 武器槽是否仍有可用武器：非空且带 TOOL 组件。 */
    private boolean hasValidWeapon() {
        ItemStack weapon = itemHandler.getStackInSlot(0);
        return !weapon.isEmpty() && weapon.has(DataComponents.TOOL);
    }

    /**
     * 选择并伤害范围内一个实体。
     *
     * @return true = 已造成伤害；false = 无有效目标
     */
    private boolean tryHurtOneEntity() {
        if (level == null) return false;
        AABB box = mobRipRangeAABB(radius); // 服务端实际伤害范围：真实 radius（含 Rg 加成）
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        // 过滤死亡实体与创造模式玩家
        entities.removeIf(e -> !e.isAlive() || (e instanceof Player p && p.isCreative()));
        if (entities.isEmpty()) return false;

        LivingEntity target = SafeOperationHelper.randomInCollection(entities);
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player p && p.isCreative()) return false;

        ItemStack weapon = itemHandler.getStackInSlot(0);
        float damage = 0.5f;
        if (!weapon.isEmpty() && weapon.has(DataComponents.TOOL)) {
            damage = 1.0f;
        }
        // 应用武器附魔：开关开启时锋利（SHARPNESS）加成伤害；关闭时不应用。
        if (useEnchantments && !weapon.isEmpty()) {
            var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(weapon);
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)) {
                    int lvl = entry.getIntValue();
                    // 锋利每级 +0.5 * level + 0.5（近似原版锋利加成）
                    damage += 0.5f * lvl + 0.5f;
                    break;
                }
            }
        }
        target.hurt(target.damageSources().cactus(), damage);
        // 仅在武器实际存在时消耗耐久
        if (!weapon.isEmpty()) {
            ItemNBTHelper.damage(weapon, level, 1);
        }
        return true;
    }

    @Override
    public double effectInterval() {
        return 3;
    }
}
