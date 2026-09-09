package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * 范围型机器基类：维护 radius（随 LevelupRg 升级扩展）与客户端预览半径，
 * 配合 RangeDisplayBER 渲染工作范围线框。
 */
public abstract class RadiusMachineBlockEntity extends EffectMachineBlockEntity {

    @Persisted
    @DescSynced
    public int radius;

    /**
     * 基础半径（构造期常量）。不持久化（@Persisted 已移除）：旧版本存档携带的
     * initialRadius（如啃噬者旧值 8）会覆盖构造值毒化范围（radius 每周期被
     * {@link #resetUpgradeEffects} 重置回 initialRadius），导致新代码 9×9 设计失效；
     * radius 自身在每周期重置时自愈，无需迁移。
     */
    @DescSynced
    public int initialRadius;

    /**
     * 服务端权威半径镜像（预览线框单一数据源）。当前客户端 radius 经 @DescSynced
     * 值比较同步（含 Rg 加成），本可直接作预览数据源；收敛到服务端单侧写入镜像
     * 属防御性设计——若未来 ticker 侧别或重算路径变化（客户端出现 radius 回算），
     * 预览数据源仍保证与服务端实际范围一致。
     */
    @DescSynced
    private int displayRadius = -1;

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
     * doBaseData 收尾：仅服务端把最终 radius 镜像到 displayRadius（值变更触发
     *
     * @DescSynced 推送）；客户端不回写，避免客户端重算值污染预览。
     */
    @Override
    public void doBaseData() {
        super.doBaseData();
        if (level != null && !level.isClientSide() && displayRadius != radius) {
            displayRadius = radius;
        }
    }
}
