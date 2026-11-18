package com.modularmc.ten.test.logic;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.testframework.annotation.TestHolder;

/**
 * 面语义与主动输出记账契约。
 *
 * <p>
 * 语义定义：「主动」= 机器自己搬；「被动」= 机器不搬，只等外部经 capability 读写。
 * {@code BE_OUT} 仅可被输出、{@code BOTH} 既可被输入也可被输出。两类面机器本 tick 都不动手。
 *
 * <p>
 * 记账反例来源：超堆叠输出槽（并行运行升级，单槽上限 64×B）配合主动推出。父类
 * {@code ItemStackHandler#extractItem} 单次提取被物品自身堆叠上限（64）封顶，旧实现按
 * 「意欲推送量」记账（目标收 256、槽只扣 64），每 tick 净增 192 个物品。
 */
@GameTestHolder(TEN.MOD_ID)
public class MachineActivePushGameTest {

    private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);
    private static final Direction OUT_SIDE = Direction.EAST;
    private static final int OVERSTACK = 256;

    /**
     * handler 层契约：一次调用即可交出整个超堆叠槽，模拟提取不改状态。
     * 这是主动推出/扳手拆卸/管道抽取共同依赖的提取语义。
     */
    @TestHolder(value = TEN.MOD_ID + ":machine_item_handler_extracts_whole_overstack", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void handlerExtractsWholeOverstack(GameTestHelper helper) {
        MachineItemHandler handler = new MachineItemHandler(1);
        handler.setDynamicSlotLimit((slot, stack) -> 64 * 4);
        handler.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, OVERSTACK));

        ItemStack simulated = handler.extractItem(0, Integer.MAX_VALUE, true);
        helper.assertTrue(simulated.getCount() == OVERSTACK,
                "simulated extract must offer the whole overstacked slot, got " + simulated.getCount());
        helper.assertTrue(handler.getStackInSlot(0).getCount() == OVERSTACK,
                "simulated extract must not mutate the slot");

        ItemStack extracted = handler.extractItem(0, Integer.MAX_VALUE, false);
        helper.assertTrue(extracted.getCount() == OVERSTACK,
                "real extract must hand out the whole overstacked slot, got " + extracted.getCount());
        helper.assertTrue(handler.getStackInSlot(0).isEmpty(),
                "slot must be empty after extracting the whole overstack");
        helper.succeed();
    }

    /**
     * 端到端契约：输出面设为主动输出，输出槽塞入超堆叠后，机器与相邻箱子内的物品总数守恒。
     * 旧实现会在此用例中凭空多出 192 个铁锭。
     */
    @TestHolder(value = TEN.MOD_ID + ":active_push_overstack_conserves_items", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void activePushOverstackConservesItems(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefinerWithChest(helper);
        if (machine == null) return;

        machine.itemFaceMode.put(OUT_SIDE, FaceOption.OUT);
        // 并行运行升级后的单槽上限：64 × 4
        machine.itemHandler.setDynamicSlotLimit((slot, stack) -> 64 * 4);
        machine.itemHandler.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, OVERSTACK));

        machine.doBaseData();

        int inMachine = machine.itemHandler.getStackInSlot(1).getCount();
        int inChest = countChest(helper, MACHINE_POS.relative(OUT_SIDE));
        helper.assertTrue(inChest > 0,
                "OUT face must push items into the adjacent chest, got " + inChest);
        helper.assertTrue(inMachine + inChest == OVERSTACK,
                "items must be conserved: machine=" + inMachine + " + chest=" + inChest + " != " + OVERSTACK + " (duplication or voiding)");
        helper.succeed();
    }

    /**
     * 被动输出面（BE_OUT）是「仅可被输出」：机器不主动推，外部可抽，外部不可塞。
     */
    @TestHolder(value = TEN.MOD_ID + ":be_out_face_is_drain_only", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void beOutFaceIsDrainOnly(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefinerWithChest(helper);
        if (machine == null) return;

        machine.itemFaceMode.put(OUT_SIDE, FaceOption.BE_OUT);
        machine.itemHandler.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 32));

        machine.doBaseData();

        helper.assertTrue(countChest(helper, MACHINE_POS.relative(OUT_SIDE)) == 0,
                "BE_OUT face must not push items into the adjacent chest");

        IItemHandler exposed = machine.getItemHandler(OUT_SIDE);
        helper.assertTrue(exposed != null, "expected a side-facing item handler");

        ItemStack drained = exposed.extractItem(1, 16, false);
        helper.assertTrue(drained.getCount() == 16,
                "BE_OUT face must be drainable by external devices, got " + drained.getCount());

        ItemStack leftover = exposed.insertItem(0, new ItemStack(Items.COBBLESTONE, 4), true);
        helper.assertTrue(leftover.getCount() == 4,
                "BE_OUT face must reject external inserts, leftover=" + leftover.getCount());
        helper.succeed();
    }

    /**
     * 被动双向面（BOTH）是「既可被输入也可被输出」：机器不主动推，外部塞/抽都通。
     */
    @TestHolder(value = TEN.MOD_ID + ":both_face_is_open_but_passive", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void bothFaceIsOpenButPassive(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefinerWithChest(helper);
        if (machine == null) return;

        machine.itemFaceMode.put(OUT_SIDE, FaceOption.BOTH);
        machine.itemHandler.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 32));

        machine.doBaseData();

        helper.assertTrue(countChest(helper, MACHINE_POS.relative(OUT_SIDE)) == 0,
                "BOTH face must not push items — passive means the machine itself stays still");

        IItemHandler exposed = machine.getItemHandler(OUT_SIDE);
        helper.assertTrue(exposed != null, "expected a side-facing item handler");

        ItemStack drained = exposed.extractItem(1, 16, false);
        helper.assertTrue(drained.getCount() == 16,
                "BOTH face must be drainable, got " + drained.getCount());

        ItemStack leftover = exposed.insertItem(0, new ItemStack(Items.COBBLESTONE, 4), false);
        helper.assertTrue(leftover.isEmpty(),
                "BOTH face must accept external inserts, leftover=" + leftover.getCount());
        helper.succeed();
    }

    /**
     * 主动输入面（IN）由机器自拉：相邻容器里的物品被搬进输入槽，且总量守恒。
     */
    @TestHolder(value = TEN.MOD_ID + ":in_face_pulls_from_neighbour", enabledByDefault = true)
    @GameTest(template = "empty_5x5")
    @PrefixGameTestTemplate(false)
    public static void inFacePullsFromNeighbour(GameTestHelper helper) {
        CmMachineBlockEntity machine = placeRefinerWithChest(helper);
        if (machine == null) return;

        ChestBlockEntity chest = helper.getBlockEntity(MACHINE_POS.relative(OUT_SIDE));
        if (chest == null) {
            helper.fail("Expected a chest block entity next to the machine");
            return;
        }
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
        machine.itemFaceMode.put(OUT_SIDE, FaceOption.IN);

        machine.doBaseData();

        int pulled = machine.itemHandler.getStackInSlot(0).getCount();
        helper.assertTrue(pulled > 0,
                "IN face must pull items from the adjacent chest, got " + pulled);
        helper.assertTrue(countChest(helper, MACHINE_POS.relative(OUT_SIDE)) + pulled == 32,
                "items must be conserved while pulling");
        helper.succeed();
    }

    /** 放置一台精炼机与其东侧的箱子，返回已初始化的机器（失败时返回 null 并已 helper.fail）。 */
    private static CmMachineBlockEntity placeRefinerWithChest(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, TENBlocks.MACHINE_REFINER.get());
        helper.setBlock(MACHINE_POS.relative(OUT_SIDE), Blocks.CHEST);

        CmMachineBlockEntity machine = helper.getBlockEntity(MACHINE_POS);
        if (machine == null) {
            helper.fail("Expected a machine block entity at " + MACHINE_POS);
            return null;
        }
        machine.initMachine();
        if (machine.itemHandler == null) {
            helper.fail("Expected the machine to have an item handler after initMachine()");
            return null;
        }
        if (machine.itemHandler.getSlots() < 2) {
            helper.fail("Expected the refiner to expose an output slot at index 1");
            return null;
        }
        return machine;
    }

    private static int countChest(GameTestHelper helper, BlockPos pos) {
        ChestBlockEntity chest = helper.getBlockEntity(pos);
        if (chest == null) {
            helper.fail("Expected a chest block entity at " + pos);
            return 0;
        }
        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            total += chest.getItem(i).getCount();
        }
        return total;
    }
}
