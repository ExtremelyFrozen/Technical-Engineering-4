package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 主动输入面（IN）的拉取契约：机器自拉只依赖相邻方块暴露的 capability，
 * 不按方块类型特判——能量靠 {@code IEnergyStorage}，流体靠 {@code IFluidHandler}。
 */
@GameTestHolder(TEN.MOD_ID)
public class MachineActivePullGameTest {

    private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);
    private static final Direction SOURCE_SIDE = Direction.EAST;

    /**
     * 能量 IN 面从相邻「被动输出」的创造能量单元抽电。
     * 旧实现只有自推、没有自拉，普通机器的能量主动输入面完全不动作。
     */
    @TestHolder(value = TEN.MOD_ID + ":in_face_pulls_energy_from_passive_cell", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void inFacePullsEnergyFromPassiveCell(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeMachine(helper, MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        CmMachineBlockEntity cell = placeMachine(helper, MACHINE_POS.relative(SOURCE_SIDE), TENBlocks.CREATIVE_CELL.get());
        if (machine == null || cell == null) return;

        machine.energyStorage.setEnergy(0);
        cell.energyStorage.setEnergy(50_000);
        // 单元朝机器的一面设为「被动输出」，机器朝单元的一面设为「主动输入」
        cell.energyFaceMode.put(SOURCE_SIDE.getOpposite(), FaceOption.BE_OUT);
        machine.energyFaceMode.put(SOURCE_SIDE, FaceOption.IN);

        IEnergyStorage source = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(MACHINE_POS.relative(SOURCE_SIDE)), SOURCE_SIDE.getOpposite());
        helper.assertTrue(source != null, "adjacent creative cell must expose IEnergyStorage");
        helper.assertTrue(source.canExtract(), "passive-output cell face must be extractable");
        helper.assertTrue(source.extractEnergy(1_000, true) > 0, "passive-output cell must offer energy");

        machine.doBaseData();

        int stored = machine.energyStorage.getEnergyStored();
        helper.assertTrue(stored > 0,
                "IN energy face must pull from the adjacent passive-output cell, got " + stored);
        helper.succeed();
    }

    /**
     * 流体 IN 面从相邻机器的输出罐抽液。
     * 旧实现同样只有自推、没有自拉，相邻容器里的流体永远进不来。
     */
    @TestHolder(value = TEN.MOD_ID + ":in_face_pulls_fluid_from_neighbour", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void inFacePullsFluidFromNeighbour(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeMachine(helper, MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        CmMachineBlockEntity source = placeMachine(helper, MACHINE_POS.relative(SOURCE_SIDE), TENBlocks.MACHINE_REFINER.get());
        if (machine == null || source == null) return;

        // 源机器的输出罐（精炼机 tank 1）装水；朝机器的一面被动输出
        source.tanks.get(1).setFluid(new FluidStack(Fluids.WATER, 1_000));
        source.fluidFaceMode.put(SOURCE_SIDE.getOpposite(), FaceOption.BE_OUT);
        // 目标机器的输入罐（tank 0）留空，朝源的一面主动输入
        machine.tanks.get(0).setFluid(FluidStack.EMPTY);
        machine.fluidFaceMode.put(SOURCE_SIDE, FaceOption.IN);

        machine.doBaseData();

        int pulled = machine.tanks.get(0).getFluidAmount();
        helper.assertTrue(pulled > 0,
                "IN fluid face must pull from the adjacent machine tank, got " + pulled);
        helper.assertTrue(source.tanks.get(1).getFluidAmount() + pulled == 1_000,
                "fluid must be conserved while pulling, source=" + source.tanks.get(1).getFluidAmount() + " pulled=" + pulled);
        helper.succeed();
    }

    /** 放置一台机器并初始化；失败时 helper.fail 并返回 null。 */
    private static CmMachineBlockEntity placeMachine(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        CmMachineBlockEntity machine = helper.getBlockEntity(pos);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + pos);
            return null;
        }
        machine.initMachine();
        // 初始化后 energyStorage 才存在；能力缓存若在 setBlock 阶段记过 null，必须此时失效，
        // 否则同一 tick 内的 capability 查询仍返回 null（GameTest 的 setBlock 不走常规更新链）。
        // 注意：GameTest 的 pos 是结构内相对坐标，level 的世界坐标要经 absolutePos 转换。
        helper.getLevel().invalidateCapabilities(helper.absolutePos(pos));
        if (machine.energyStorage == null) {
            helper.fail("Expected the machine to expose energy storage at " + pos);
            return null;
        }
        return machine;
    }
}
