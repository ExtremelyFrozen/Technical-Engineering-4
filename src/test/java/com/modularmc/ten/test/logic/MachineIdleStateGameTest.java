package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 待机/唤醒契约：空闲机器进入 {@link CmMachineBlockEntity#STATE_IDLE} 后，
 * 任何输入侧变化都必须把状态同步推回 AWAKE（0 tick 延迟，不等下一次 tick）。
 */
@GameTestHolder(TEN.MOD_ID)
public class MachineIdleStateGameTest {

    private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);

    @TestHolder(value = TEN.MOD_ID + ":idle_after_empty_ticks", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void idleAfterEmptyTicks(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeMachine(helper, MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        if (machine == null) return;

        for (int i = 0; i < 3; i++) {
            machine.serverTick();
        }
        helper.assertTrue(machine.isIdle(), "empty machine must enter idle after ticking");
        helper.succeed();
    }

    @TestHolder(value = TEN.MOD_ID + ":idle_wake_on_item_change", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void idleWakesOnItemChange(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeMachine(helper, MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        if (machine == null) return;

        for (int i = 0; i < 3; i++) {
            machine.serverTick();
        }
        helper.assertTrue(machine.isIdle(), "precondition: empty machine must be idle");

        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 1));

        helper.assertFalse(machine.isIdle(), "item change must wake the machine synchronously (0 tick)");
        helper.succeed();
    }

    /** 输入指纹必须同时感知物品数量与数据组件的变化。 */
    @TestHolder(value = TEN.MOD_ID + ":idle_fingerprint_tracks_stack_identity", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void idleFingerprintTracksStackIdentity(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeMachine(helper, MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        if (machine == null) return;

        for (int i = 0; i < 3; i++) {
            machine.serverTick();
        }
        helper.assertTrue(machine.isIdle(), "precondition: empty machine must be idle");

        long empty = machine.inputFingerprint();
        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 1));
        long one = machine.inputFingerprint();
        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.IRON_ORE, 2));
        long two = machine.inputFingerprint();
        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.GOLD_ORE, 2));
        long other = machine.inputFingerprint();

        helper.assertTrue(empty != one, "empty vs one item must differ");
        helper.assertTrue(one != two, "count change must be visible in the fingerprint");
        helper.assertTrue(two != other, "item swap must be visible in the fingerprint");
        helper.succeed();
    }

    /**
     * 有料无电的机器会被 conditionStart 的 B_byEnergy==0 判死并进入待机（走的是 !start 分支，
     * 不是 energyAllowRun 的 BLOCKED 路径），所以储能跨过「至少能跑一份」门槛时必须自己唤醒。
     */
    @TestHolder(value = TEN.MOD_ID + ":idle_wakes_on_energy_restore", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void idleWakesOnEnergyRestore(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeMachine(helper, MACHINE_POS, TENBlocks.MACHINE_PULVERIZER.get());
        if (machine == null) return;

        machine.itemHandler.setStackInSlot(0, new ItemStack(Items.ANDESITE, 1));
        machine.energyStorage.setEnergy(0);
        for (int i = 0; i < 3; i++) {
            machine.serverTick();
        }
        helper.assertTrue(machine.isIdle(), "precondition: machine with no energy must be idle");

        machine.energyStorage.setEnergy(10_000);
        machine.serverTick();

        helper.assertFalse(machine.isIdle(), "energy restore must wake the machine within one tick");
        helper.assertTrue(machine.progress > 0, "machine must start progressing after energy restore");
        helper.succeed();
    }

    private static CmMachineBlockEntity placeMachine(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
        CmMachineBlockEntity machine = helper.getBlockEntity(pos);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + pos);
            return null;
        }
        machine.initMachine();
        if (machine.energyStorage == null) {
            helper.fail("Expected the machine to expose energy storage at " + pos);
            return null;
        }
        return machine;
    }
}
