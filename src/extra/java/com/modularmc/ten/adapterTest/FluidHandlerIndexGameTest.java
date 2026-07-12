package com.modularmc.ten.adapterTest;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;
import net.neoforged.testframework.gametest.ExtendedGameTestHelper;
import com.modularmc.ten.api.capability.FluidHandlerResourceAdapter;

/**
 * GameTest for FluidHandlerResourceAdapter tank-index behavior.
 *
 * <p>This test verifies that {@code extract(1, resource, amount, ctx)} actually drains
 * from tank 1, not from any other tank, using a fake two-tank handler.
 */
@net.neoforged.testframework.annotation.TestHolder(value = "kenergyengineering:fluid_handler_index_test")
public class FluidHandlerIndexGameTest {

    private int passed = 0;
    private int failed = 0;

    /**
     * Core test: extract(index=1) must drain from tank 1, not tank 0.
     *
     * Setup:
     *   Tank 0: 1000mB Water
     *   Tank 1:  500mB Water
     *
     * Action: extract(1, WATER_RES, 300, ...)
     *
     * Expected (correct behavior):
     *   Tank 0: 1000mB (unchanged)
     *   Tank 1:  200mB (500 - 300)
     *
     * Current bug:
     *   handler-wide drain takes from tank 0 first
     */
    @net.neoforged.testframework.gametest.GameTest(required = true)
    public void extractDrainsCorrectTank(ExtendedGameTestHelper helper) {
        // Setup: two independent tanks
        FakeTwoTankHandler handler = new FakeTwoTankHandler();
        handler.tank0.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
        handler.tank0.getFluid().setAmount(1000);
        handler.tank1.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 500));
        handler.tank1.getFluid().setAmount(500);

        FluidHandlerResourceAdapter adapter = new FluidHandlerResourceAdapter(handler);

        System.out.println("[FLUID_HANDLER_INDEX_TEST] Before extract: tank0="
            + handler.tank0.getFluid().getAmount() + "mB, tank1=" + handler.tank1.getFluid().getAmount() + "mB");

        try (Transaction tx = Transaction.openRoot()) {
            int drained = adapter.extract(1, FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 300, tx);
            tx.commit();

            System.out.println("[FLUID_HANDLER_INDEX_TEST] After extract: drained=" + drained
                + ", tank0=" + handler.tank0.getFluid().getAmount()
                + "mB, tank1=" + handler.tank1.getFluid().getAmount() + "mB");

            // Check results
            if (drained != 300) {
                helper.fail("extract returned " + drained + " (expected 300)");
                return;
            }
            if (handler.tank1.getFluid().getAmount() != 200) {
                helper.fail("Tank 1 has " + handler.tank1.getFluid().getAmount()
                    + "mB (expected 200mB - wrong tank was drained!)");
                return;
            }
            if (handler.tank0.getFluid().getAmount() != 1000) {
                helper.fail("Tank 0 has " + handler.tank0.getFluid().getAmount()
                    + "mB (expected 1000mB - tank 0 was incorrectly drained!)");
                return;
            }

            helper.succeed();
        }
    }

    // ─── Fake two-tank handler ───

    @SuppressWarnings("deprecation")
    static class FakeTwoTankHandler implements IFluidHandler {
        final FluidTank tank0 = new FluidTank(1000);
        final FluidTank tank1 = new FluidTank(1000);

        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (tank == 0) return tank0.getFluid();
            if (tank == 1) return tank1.getFluid();
            return FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) {
            if (tank == 0) return tank0.getCapacity();
            if (tank == 1) return tank1.getCapacity();
            return 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return true; }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack r = tank0.drain(resource, action);
            if (!r.isEmpty()) return r;
            return tank1.drain(resource, action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack r = tank0.drain(maxDrain, action);
            if (!r.isEmpty()) return r;
            return tank1.drain(maxDrain, action);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            int f0 = tank0.fill(resource, action);
            if (f0 >= resource.getAmount()) return f0;
            FluidStack rem = resource.copy();
            rem.setAmount(resource.getAmount() - f0);
            return f0 + tank1.fill(rem, action);
        }
    }
}
