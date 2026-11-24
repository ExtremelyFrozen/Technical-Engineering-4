package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * 范围型机器基类。工作区由范围调节面板的 XYZ 盒
 * （{@link CmMachineBlockEntity#adjustedBounds()}）驱动，{@code radius} 退化为
 * 旧存档兼容字段与「范围升级是否可装」的门禁；{@link #displayRadius} 镜像只给
 * RangeDisplayBER 兜底预览用，子类线框一律读面板盒。
 */
public abstract class RadiusMachineBlockEntity extends EffectMachineBlockEntity {

    @Persisted
    @DescSynced
    public int radius;

    /**
     * 基础半径（构造期常量）。**不可加 @Persisted**：旧存档里的 initialRadius 会覆盖构造值，
     * 而每轮 {@link #resetUpgradeEffects} 又把 radius 重置回 initialRadius，旧值会永久毒化范围。
     */
    @DescSynced
    public int initialRadius;

    /** 服务端权威半径镜像（预览线框单一数据源）：客户端不回写，避免客户端重算值污染预览。 */
    @DescSynced
    private int displayRadius = -1;

    /**
     * 面板默认跨度（半单位）：构造器经 {@link #setDefaultRange(int, int, int)} 写入。
     * 旧存档的 rangeX/Y/Z 是接入面板前的占位值 1，首次加载要覆盖回去。
     */
    private int defaultRangeX = 1;
    private int defaultRangeY = 1;
    private int defaultRangeZ = 1;

    /** 旧档迁移哨兵：false = 还没把面板默认值写回（新机器也是 false，写一次即等价）。 */
    @Persisted
    private boolean rangeMigrated;

    /** 构造器写入设备的初始范围：同时作为面板默认值与旧档迁移源。 */
    protected void setDefaultRange(int x, int y, int z) {
        this.defaultRangeX = x;
        this.defaultRangeY = y;
        this.defaultRangeZ = z;
        this.rangeX = x;
        this.rangeY = y;
        this.rangeZ = z;
    }

    /** 预览半径（客户端线框用）：displayRadius 未同步前回退 radius。 */
    protected int previewRadius() {
        return displayRadius >= 0 ? displayRadius : radius;
    }

    public RadiusMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isType(String type) {
        if ("MACHINE_EFFECT".equals(type)) {
            return true;
        }
        return super.isType(type);
    }

    @Override
    public int getCurrentRadius() {
        return radius;
    }

    @Override
    public void setCurrentRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public int getInitialRadius() {
        return initialRadius;
    }

    /** 工作区由面板 XYZ 盒驱动，{@code radius} 只剩旧档兼容与升级卡门禁两处读取。 */
    @Override
    public boolean supportsRangeAdjust() {
        return true;
    }

    /**
     * 每轮升级 apply 前重置 radius 到 initialRadius，
     * 防止 {@link com.modularmc.ten.common.item.upgrades.LevelupRg#effect}
     * 的累加语义在每 tick doBaseData 中无界增长。
     * 由 {@link CmMachineBlockEntity#doBaseData()} 在单次
     * {@link CmMachineBlockEntity#applyUpgradeEffects()} 前调用。
     */
    @Override
    protected void resetUpgradeEffects() {
        super.resetUpgradeEffects();
        this.radius = this.initialRadius;
    }

    /**
     * doBaseData 收尾：旧档先把面板默认范围写回，再由基类钳位；随后仅服务端把最终
     * radius 镜像到 displayRadius（值变更触发同步）。
     */
    @Override
    public void doBaseData() {
        if (level != null && !level.isClientSide() && !rangeMigrated) {
            rangeMigrated = true;
            rangeX = defaultRangeX;
            rangeY = defaultRangeY;
            rangeZ = defaultRangeZ;
            markDirty("rangeX");
            markDirty("rangeY");
            markDirty("rangeZ");
        }
        super.doBaseData();
        if (level != null && !level.isClientSide() && displayRadius != radius) {
            displayRadius = radius;
        }
    }
}
