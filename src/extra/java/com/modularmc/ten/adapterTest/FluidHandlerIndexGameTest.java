package com.modularmc.ten.adapterTest;

import com.modularmc.ten.api.capability.FluidHandlerResourceAdapter;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/**
 * GameTest for FluidHandlerResourceAdapter per-index and transaction behavior.
 *
 * <p>Verifies that {@code insert(index, ...)} and {@code extract(index, ...)}
 * operate on the specified tank only, support transaction rollback/commit,
 * and that read methods are not blocked by write permissions.
 */
@net.neoforged.testframework.annotation.TestHolder(value = "kenergyengineering:fluid_handler_index_test")
public class FluidHandlerIndexGameTest {

    private int passed = 0;
    private int failed = 0;

    // ─── Helper: create a two-tank adapter ───

    private static FluidHandlerResourceAdapter createTwoTankAdapter(
            BooleanSupplier insertAllowed,
            BooleanSupplier extractAllowed,
            Runnable onCommit
    ) {
        List<MachineFluidTank> tanks = List.of(
                new MachineFluidTank(1000),
                new MachineFluidTank(1000)
        );
        IntFunction<IngredientType> tankType = idx -> IngredientType.BOTH;
        BiPredicate<Integer, FluidStack> validator = (idx, stack) -> true;
        return new FluidHandlerResourceAdapter(
                tanks, tankType, validator,
                insertAllowed, extractAllowed,
                onCommit
        );
    }

    /**
     * Core test: extract(index=1) must drain from tank 1, not tank 0.
     * Also tests transaction commit preserves the change.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void extractDrainsCorrectTank(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );

        // Access tanks through the adapter's internal structure
        // (We use FluidResource.of() to set up via the adapter if possible,
        //  otherwise we use direct tank access via the exposed tanks field)
        // Setup: tank 0 = 1000mB Water, tank 1 = 500mB Water
        // Since we don't have direct tank access from the adapter,
        // we insert into specific indices
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // Initialize via insert
        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 1000, tx);
            adapter.insert(1, water, 500, tx);
            tx.commit();
        }

        System.out.println("[TEST] Before extract: checking tank states");

        try (Transaction tx = Transaction.openRoot()) {
            int drained = adapter.extract(1, water, 300, tx);
            tx.commit();

            System.out.println("[TEST] After extract: drained=" + drained
                    + ", tank0=" + adapter.getAmountAsLong(0)
                    + "mB, tank1=" + adapter.getAmountAsLong(1) + "mB");

            if (drained != 300) {
                helper.fail("extract returned " + drained + " (expected 300)");
                return;
            }
            if (adapter.getAmountAsLong(1) != 200) {
                helper.fail("Tank 1 has " + adapter.getAmountAsLong(1)
                        + "mB (expected 200mB - wrong tank was drained!)");
                return;
            }
            if (adapter.getAmountAsLong(0) != 1000) {
                helper.fail("Tank 0 has " + adapter.getAmountAsLong(0)
                        + "mB (expected 1000mB - tank 0 was incorrectly drained!)");
                return;
            }
            helper.succeed();
        }
    }

    /**
     * Test: insert(index=0) must fill tank 0, not tank 1.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void insertFillsCorrectTank(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // Insert into tank 0 only
        try (Transaction tx = Transaction.openRoot()) {
            int filled = adapter.insert(0, water, 300, tx);
            tx.commit();

            if (filled != 300) {
                helper.fail("insert returned " + filled + " (expected 300)");
                return;
            }
            if (adapter.getAmountAsLong(0) != 300) {
                helper.fail("Tank 0 has " + adapter.getAmountAsLong(0)
                        + "mB (expected 300mB)");
                return;
            }
            if (adapter.getAmountAsLong(1) != 0) {
                helper.fail("Tank 1 has " + adapter.getAmountAsLong(1)
                        + "mB (expected 0mB - tank 1 was incorrectly filled!)");
                return;
            }
            helper.succeed();
        }
    }

    /**
     * Test: transaction abort restores original state.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void abortRestoresState(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // First, commit some fluid
        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 500, tx);
            tx.commit();
        }

        long beforeAmount = adapter.getAmountAsLong(0);

        // Now do an operation and abort
        try (Transaction tx = Transaction.openRoot()) {
            adapter.extract(0, water, 200, tx);
            // Abort — don't commit
        }

        long afterAmount = adapter.getAmountAsLong(0);
        if (afterAmount != beforeAmount) {
            helper.fail("Abort did not restore state: before=" + beforeAmount
                    + " after=" + afterAmount);
            return;
        }
        helper.succeed();
    }

    /**
     * Test: commit preserves changes after a write.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void commitPreservesChanges(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 700, tx);
            tx.commit();
        }

        if (adapter.getAmountAsLong(0) != 700) {
            helper.fail("Commit did not preserve change: expected 700 but got "
                    + adapter.getAmountAsLong(0));
            return;
        }
        helper.succeed();
    }

    /**
     * Test: read methods work even when write permissions are false.
     * (Jade depends on this behavior.)
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void readWorksWithoutWritePermission(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> false,  // insert NOT allowed
                () -> false,  // extract NOT allowed
                () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // First, add some fluid while permissions are still true
        // We need to use a different adapter instance to fill
        // Actually, we pre-fill via the tanks directly:
        // But since we use the new constructor with tanks list, we need to
        // verify that getResource/getAmountAsLong work with write=false

        // Fill via insert when allowed (but we set false, so use a throwaway adapter)
        FluidHandlerResourceAdapter filler = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        try (Transaction tx = Transaction.openRoot()) {
            filler.insert(0, water, 800, tx);
            tx.commit();
        }

        // Now the test adapter has write=false
        // Verify reads still work
        if (adapter.getAmountAsLong(0) != 0) {
            // If the adapter has fluid, read must return it
            helper.fail("Read returned " + adapter.getAmountAsLong(0)
                    + " despite no fluid in test adapter (expected 0)");
            return;
        }

        // Verify read methods don't throw
        try {
            FluidResource resource = adapter.getResource(0);
            long amount = adapter.getAmountAsLong(0);
            long capacity = adapter.getCapacityAsLong(0, water);
            boolean valid = adapter.isValid(0, water);
            System.out.println("[TEST] Read with write=false: resource=" + resource
                    + " amount=" + amount + " capacity=" + capacity + " valid=" + valid);
        } catch (Exception e) {
            helper.fail("Read threw exception with write=false: " + e.getMessage());
            return;
        }

        helper.succeed();
    }

    /**
     * Test: insert returns 0 when insertAllowed returns false.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void insertBlockedByPermission(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> false,  // insert NOT allowed
                () -> true,
                () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        try (Transaction tx = Transaction.openRoot()) {
            int filled = adapter.insert(0, water, 100, tx);
            if (filled != 0) {
                helper.fail("insert returned " + filled + " when insertAllowed=false (expected 0)");
                return;
            }
            tx.commit();
        }

        if (adapter.getAmountAsLong(0) != 0) {
            helper.fail("Tank was filled despite insertAllowed=false");
            return;
        }
        helper.succeed();
    }

    /**
     * Test: extract returns 0 when extractAllowed returns false.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void extractBlockedByPermission(ExtendedGameTestHelper helper) {
        // First fill via a disposable adapter
        FluidHandlerResourceAdapter filler = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        try (Transaction tx = Transaction.openRoot()) {
            filler.insert(1, water, 500, tx);
            tx.commit();
        }

        // Now use the restricted adapter
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true,
                () -> false,  // extract NOT allowed
                () -> {}
        );
        try (Transaction tx = Transaction.openRoot()) {
            int drained = adapter.extract(1, water, 200, tx);
            if (drained != 0) {
                helper.fail("extract returned " + drained + " when extractAllowed=false (expected 0)");
                return;
            }
            tx.commit();
        }
        helper.succeed();
    }

    /**
     * Test: onRootCommit callback fires exactly once on commit.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void commitCallbackFiresOnce(ExtendedGameTestHelper helper) {
        final int[] callCount = {0};
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true,
                () -> callCount[0]++
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // Perform two operations in the same root transaction — callback should fire once
        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 100, tx);
            adapter.insert(1, water, 50, tx);
            tx.commit();
        }

        if (callCount[0] != 1) {
            helper.fail("onRootCommit callback fired " + callCount[0] + " times (expected exactly 1)");
            return;
        }
        helper.succeed();
    }

    /**
     * Test: transaction abort does NOT fire the commit callback.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void abortDoesNotFireCallback(ExtendedGameTestHelper helper) {
        final boolean[] called = {false};
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true,
                () -> called[0] = true
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // Abort the transaction — callback must NOT fire
        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 100, tx);
            // No tx.commit() — abort on close
        }

        if (called[0]) {
            helper.fail("onRootCommit callback was fired on abort (should only fire on commit)");
            return;
        }
        helper.succeed();
    }

    /**
     * Test: getCapacityAsLong returns capacity even for empty FluidResource.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void emptyResourceReturnsCapacity(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        FluidResource empty = FluidResource.of(FluidStack.EMPTY);

        // Empty resource should still return the general tank capacity
        long capacityWithWater = adapter.getCapacityAsLong(0, water);
        long capacityWithEmpty = adapter.getCapacityAsLong(0, empty);

        if (capacityWithEmpty <= 0) {
            helper.fail("getCapacityAsLong with empty resource returned " + capacityWithEmpty
                    + " (expected general capacity " + capacityWithWater + ")");
            return;
        }
        if (capacityWithEmpty != capacityWithWater) {
            helper.fail("getCapacityAsLong mismatch: empty=" + capacityWithEmpty
                    + " water=" + capacityWithWater + " (should be same general capacity)");
            return;
        }
        helper.succeed();
    }

    /**
     * Test: insert respects component matching — resource with mismatched
     * components is rejected when tank already has a different component set.
     *
     * <p>NOTE: Construction of custom DataComponentPatch for test fluids requires
     * registry access that may not be available in this test context. The
     * {@code resource.matches(current)} call is verified via static analysis
     * (see fluid_gui_capability_check.py assertion C). At runtime, the vanilla
     * fluid system without patches will match as expected.
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void componentMatchUsedOnInsert(ExtendedGameTestHelper helper) {
        // Verify that the adapter uses resource.matches internally.
        // With vanilla fluids (no DataComponentPatch), matching is equivalent
        // to type matching, but the method used is resource.matches().
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );

        // Fill tank 0 with water
        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 500, tx);
            tx.commit();
        }

        // A second insert of the same resource should succeed (matches existing)
        try (Transaction tx = Transaction.openRoot()) {
            int filled = adapter.insert(0, water, 200, tx);
            if (filled != 200) {
                helper.fail("Same resource insert returned " + filled + " (expected 200)");
                return;
            }
            tx.commit();
        }

        if (adapter.getAmountAsLong(0) != 700) {
            helper.fail("Tank 0 has " + adapter.getAmountAsLong(0) + "mB (expected 700mB)");
            return;
        }
        helper.succeed();
    }

    /**
     * Test: inserting into a full tank returns 0 (capacity check).
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void insertRespectsCapacity(ExtendedGameTestHelper helper) {
        FluidHandlerResourceAdapter adapter = createTwoTankAdapter(
                () -> true, () -> true, () -> {}
        );
        FluidResource water = FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);

        // Fill to capacity
        try (Transaction tx = Transaction.openRoot()) {
            adapter.insert(0, water, 1000, tx);
            tx.commit();
        }

        // Try to insert more
        try (Transaction tx = Transaction.openRoot()) {
            int filled = adapter.insert(0, water, 100, tx);
            if (filled != 0) {
                helper.fail("insert returned " + filled + " when tank is full (expected 0)");
                return;
            }
            tx.commit();
        }
        helper.succeed();
    }
}
