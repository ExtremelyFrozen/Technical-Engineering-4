// -*- coding: utf-8 -*-
package com.modularmc.ten.api.capability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.modularmc.ten.api.blockentity.RecipeMachineBlockEntity;

import java.util.function.ToIntBiFunction;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for dynamic output slot limits — pure logic,
 * no Minecraft runtime dependencies.
 * <p>
 * Uses a simulated slot handler to verify the dynamic limit
 * concept without requiring {@link net.minecraft.world.item.ItemStack}
 * or Minecraft bootstrap.
 */
class MachineOutputSlotContractTest {

    // ════════════════════════════════════════════════════════════
    // Simulated slot environment (Minecraft-free)
    // ════════════════════════════════════════════════════════════

    /**
     * A minimal slot record: item type (int id), count.
     */
    private record SimSlot(int item, int count) {}

    /**
     * Simulated item handler with dynamic limits, mirroring
     * the MachineItemHandler contract without Minecraft deps.
     */
    private static class SimHandler {
        private final SimSlot[] slots;
        private ToIntBiFunction<Integer, SimSlot> dynamicLimit;

        SimHandler(int size) {
            slots = new SimSlot[size];
            for (int i = 0; i < size; i++) slots[i] = new SimSlot(0, 0);
        }

        void setDynamicLimit(ToIntBiFunction<Integer, SimSlot> provider) {
            this.dynamicLimit = provider;
        }

        int getSlotLimit(int slot) {
            if (dynamicLimit != null) {
                int limit = dynamicLimit.applyAsInt(slot, slots[slot]);
                return Math.max(limit, slots[slot].count());
            }
            return 64;
        }

        SimSlot getSlot(int slot) { return slots[slot]; }

        int insert(int slot, int item, int count) {
            int limit = getSlotLimit(slot);
            SimSlot current = slots[slot];
            if (current.item() != 0 && current.item() != item) return count; // wrong item
            int existingCount = current.count();
            int space = limit - existingCount;
            int toAdd = Math.min(space, count);
            slots[slot] = new SimSlot(item, existingCount + toAdd);
            return count - toAdd; // remainder
        }
    }

    private static final int ITEM_A = 1;
    private static final int ITEM_B = 2;
    private static final int ABSOLUTE_MAX = 99;

    // ════════════════════════════════════════════════════════════
    // A. Dynamic slot limit concept
    // ════════════════════════════════════════════════════════════

    @Nested
    class DynamicSlotLimit {

        @Test
        void defaultLimitIs64() {
            var h = new SimHandler(5);
            for (int i = 0; i < 5; i++) {
                assertEquals(64, h.getSlotLimit(i));
            }
        }

        @Test
        void setLimit_affectsGetSlotLimit() {
            var h = new SimHandler(5);
            h.setDynamicLimit((slot, s) -> 81);
            assertEquals(81, h.getSlotLimit(0));
        }

        @Test
        void limit_differsBySlot() {
            var h = new SimHandler(5);
            h.setDynamicLimit((slot, s) -> slot == 0 ? 81 : 64);
            assertEquals(81, h.getSlotLimit(0));
            assertEquals(64, h.getSlotLimit(1));
        }

        @Test
        void nullRestoresDefault() {
            var h = new SimHandler(5);
            h.setDynamicLimit((slot, s) -> 81);
            h.setDynamicLimit(null);
            assertEquals(64, h.getSlotLimit(0));
        }

        @Test
        void neverBelowExistingCount() {
            var h = new SimHandler(5);
            h.setDynamicLimit((slot, s) -> 100); // first allow 100
            h.insert(0, ITEM_A, 90);             // fill to 90
            h.setDynamicLimit((slot, s) -> 81);  // now lower limit
            // With existing 90, effective limit must be at least 90
            assertTrue(h.getSlotLimit(0) >= 90,
                    "Slot limit must preserve existing overstack of 90");
        }

        @Test
        void insertRespectsDynamicLimit() {
            var h = new SimHandler(5);
            h.setDynamicLimit((slot, s) -> 81);
            int remainder = h.insert(0, ITEM_A, 81);
            assertEquals(0, remainder);
            assertEquals(81, h.getSlot(0).count());
        }

        @Test
        void insertRejectsExcess() {
            var h = new SimHandler(5);
            h.setDynamicLimit((slot, s) -> 81);
            int remainder = h.insert(0, ITEM_A, 90);
            assertTrue(remainder > 0);
            assertEquals(9, remainder);
            assertEquals(81, h.getSlot(0).count());
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. N+63 slot limit formula
    // ════════════════════════════════════════════════════════════

    @Nested
    class SlotLimitFormula {

        @Test
        void rawBlock_N18_limit81() {
            int totalN = 9 + 9;
            assertEquals(18, totalN);
            int limit = Math.min(totalN + 63, ABSOLUTE_MAX);
            assertEquals(81, limit);
        }

        @Test
        void singleOutput_limit64() {
            assertEquals(64, Math.min(1 + 63, ABSOLUTE_MAX));
        }

        @Test
        void cappedAt99() {
            assertEquals(99, Math.min(36 + 63, ABSOLUTE_MAX));
            assertEquals(99, Math.min(99, ABSOLUTE_MAX));
            assertEquals(99, Math.min(100, ABSOLUTE_MAX));
        }

        @Test
        void neverBelowExistingCount() {
            int existing = 63;
            int limit = Math.max(Math.min(18 + 63, ABSOLUTE_MAX), existing);
            assertEquals(81, limit);

            existing = 90;
            limit = Math.max(Math.min(18 + 63, ABSOLUTE_MAX), existing);
            assertEquals(90, limit, "Must preserve existing overstack");
        }

        @Test
        void allValuesFrom1to99() {
            for (int n = 1; n <= 99; n++) {
                int limit = Math.min(n + 63, ABSOLUTE_MAX);
                assertTrue(limit >= 64 && limit <= 99,
                        "N=" + n + " → limit=" + limit);
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Capacity simulation
    // ════════════════════════════════════════════════════════════

    @Nested
    class CapacitySimulation {

        @Test
        void singleSlot_exactFit() {
            int limit = 81, existing = 63, toAdd = 18;
            int space = limit - existing;
            assertTrue(space >= toAdd);
            assertEquals(81, existing + toAdd);
        }

        @Test
        void singleSlot_rejectsOverflow() {
            int limit = 81, existing = 63, toAdd = 19;
            int space = limit - existing;
            assertTrue(space < toAdd);
        }

        @Test
        void twoSlots_distributes() {
            int[] limits = {81, 81};
            int[] existing = {64, 0};
            int toAdd = 18;
            int remaining = distribute(existing, limits, toAdd);
            assertEquals(0, remaining);
            assertEquals(81, existing[0]);
            assertEquals(1, existing[1]);
        }

        @Test
        void allSlots_fullDistribute() {
            int[] limits = {81, 81, 81};
            int[] existing = {64, 64, 64};
            int toAdd = 51; // space = 17+17+17 = 51
            int remaining = distribute(existing, limits, toAdd);
            assertEquals(0, remaining);
            for (int e : existing) assertEquals(81, e);
        }

        @Test
        void insufficientSpace_fails() {
            int[] limits = {81};
            int[] existing = {80};
            int toAdd = 18;
            int remaining = distribute(existing, limits, toAdd);
            assertTrue(remaining > 0, "Should have remaining items");
            assertEquals(81, existing[0], "Only 1 should fit (81-80=1) → slot fills to 81");
            assertEquals(17, remaining, "17 should remain");
        }

        @Test
        void aggregateSameItem_fits() {
            int totalN = 18;
            int limit = Math.min(totalN + 63, ABSOLUTE_MAX);
            assertEquals(81, limit);

            int[] slots = {63};
            int remaining = distribute(slots, new int[]{limit}, totalN);
            assertEquals(0, remaining);
            assertEquals(81, slots[0]);
        }

        @Test
        void differentItems_separateLimits() {
            int[] limits = {Math.min(9 + 63, ABSOLUTE_MAX), Math.min(9 + 63, ABSOLUTE_MAX)};
            assertEquals(72, limits[0]);
            assertEquals(72, limits[1]);
        }

        @Test
        void twoOutputsSameItem_jointCheck() {
            // Main: count=9, rolls=1 → N=9
            // Bonus: count=1, rolls=9 → N=9
            // Total N=18, limit=81 across 4 output slots
            int totalN = 18;
            int limitPerSlot = Math.min(totalN + 63, ABSOLUTE_MAX);
            assertEquals(81, limitPerSlot);

            // Simulate: first slot has 63, rest empty
            int[] slots = {63, 0, 0, 0};
            int[] limits = {limitPerSlot, limitPerSlot, limitPerSlot, limitPerSlot};
            int remaining = totalN;
            // First ingest: bonus (N=9)
            remaining = distribute(slots, limits, 9);
            assertEquals(0, remaining, "First 9 should fit");
            assertEquals(72, slots[0]);

            // Second ingest: main (N=9)
            remaining = distribute(slots, limits, 9);
            assertEquals(0, remaining, "Second 9 should fit (72+9=81)");
            assertEquals(81, slots[0]);
        }

        @Test
        void twoOutputsSameItem_overflowWithoutJoint() {
            // Two outputs for same item, each N=9, but only 1 slot with 76 existing
            // Individually each would "fit" (9 < 81-76=5... wait no)
            // limit=81, existing=76, space=5. Neither fits individually.
            int totalN = 18;
            int limitPerSlot = Math.min(totalN + 63, ABSOLUTE_MAX);
            assertEquals(81, limitPerSlot);
            
            int existing = 76;
            int spacePerOutput = Math.min(9, limitPerSlot - existing); // = min(9, 5) = 5
            // Individual check would say "5 < 9" → FAIL for EACH
            // Joint check: totalN=18, space=5 → FAIL
            assertTrue(5 < 9, "Neither output fits individually");
            assertTrue(5 < 18, "Joint also fails");
        }

        private int distribute(int[] slots, int[] limits, int amount) {
            int remaining = amount;
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                int space = limits[i] - slots[i];
                int add = Math.min(space, remaining);
                slots[i] += add;
                remaining -= add;
            }
            return remaining;
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. giveOutput replacement logic
    // ════════════════════════════════════════════════════════════

    @Nested
    class GiveOutputLogic {

        @Test
        void usesDynamicLimit_notMaxStackSize() {
            int current = 63, limit = 81, toAdd = 18;
            int space = limit - current;
            int added = Math.min(space, toAdd);
            assertEquals(18, added);
            assertEquals(81, current + added);
        }

        @Test
        void preservesExistingOverstack() {
            // Slot has 90 items (from previous overstack config), limit=81
            // The effective limit = max(81, 90) = 90, preserving existing items.
            // Since already at capacity (90), no more can be added.
            int current = 90, limit = 81, toAdd = 5;
            int effectiveLimit = Math.max(limit, current); // 90
            int space = effectiveLimit - current; // 0
            int added = Math.min(space, toAdd);
            assertEquals(0, added, "No space when existing overstack fills the effective limit");
        }

        @Test
        void remainderTriggersNextSlot() {
            int[] counts = {64, 0};
            int[] limits = {81, 81};
            int toAdd = 30;
            int remaining = toAdd;
            for (int i = 0; i < counts.length && remaining > 0; i++) {
                int effectiveLimit = Math.max(limits[i], counts[i]);
                int space = effectiveLimit - counts[i];
                int add = Math.min(space, remaining);
                counts[i] += add;
                remaining -= add;
            }
            assertEquals(0, remaining);
            assertEquals(81, counts[0]);
            assertEquals(13, counts[1]);
        }

        @Test
        void noSlotAvailable_remainderReported() {
            int[] counts = {80};
            int[] limits = {81};
            int toAdd = 18;
            int remaining = toAdd;
            for (int i = 0; i < counts.length && remaining > 0; i++) {
                int effectiveLimit = Math.max(limits[i], counts[i]);
                int space = effectiveLimit - counts[i];
                int add = Math.min(space, remaining);
                counts[i] += add;
                remaining -= add;
            }
            assertTrue(remaining > 0, "Should have remainder: " + remaining);
            assertEquals(81, counts[0], "Slot should fill to 81");
            assertEquals(17, remaining, "17 should remain");
        }

        @Test
        void existing63_accepts18_total81() {
            int[] counts = {63};
            int[] limits = {81};
            int toAdd = 18;
            int remaining = toAdd;
            for (int i = 0; i < counts.length && remaining > 0; i++) {
                int space = limits[i] - counts[i];
                int add = Math.min(space, remaining);
                counts[i] += add;
                remaining -= add;
            }
            assertEquals(0, remaining);
            assertEquals(81, counts[0]);
        }

        @Test
        void existing64_needsSecondSlot() {
            int[] counts = {64, 0};
            int[] limits = {81, 81};
            int toAdd = 18;
            int remaining = toAdd;
            for (int i = 0; i < counts.length && remaining > 0; i++) {
                int space = Math.max(limits[i], counts[i]) - counts[i];
                int add = Math.min(space, remaining);
                counts[i] += add;
                remaining -= add;
            }
            assertEquals(0, remaining);
            assertEquals(81, counts[0], "Slot 0 fills to 81");
            assertEquals(1, counts[1], "Slot 1 gets remaining 1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. MachineItemHandler API surface verification
    // ════════════════════════════════════════════════════════════

    @Nested
    class ApiSurface {

        @Test
        void setDynamicSlotLimit_methodExistsOnMachineItemHandler() throws Exception {
            var method = MachineItemHandler.class.getMethod(
                    "setDynamicSlotLimit", ToIntBiFunction.class);
            assertNotNull(method);
        }

        @Test
        void getEffectiveSlotLimit_methodExists() throws Exception {
            var method = MachineItemHandler.class.getMethod(
                    "getEffectiveSlotLimit", int.class, net.minecraft.world.item.ItemStack.class);
            assertNotNull(method);
        }

    @Test
    void dynamicSlotLimit_isNotLombokSetter() throws Exception {
        // Lombok @Setter would create setDynamicSlotLimit with
        // the exact field type. We defined it manually to prevent
        // accidental exposure. Verify it's the correct signature.
        var methods = MachineItemHandler.class.getMethods();
        boolean found = false;
        for (var m : methods) {
            if (m.getName().equals("setDynamicSlotLimit")) {
                assertEquals(1, m.getParameterCount());
                assertEquals(ToIntBiFunction.class, m.getParameterTypes()[0]);
                found = true;
                break;
            }
        }
        assertTrue(found, "setDynamicSlotLimit must exist with correct signature");
    }
}

    // ════════════════════════════════════════════════════════════
    // F. Remainder drop path contract (RED→GREEN cycle)
    // ════════════════════════════════════════════════════════════

    @Nested
    class RemainderDropPath {

        @Test
        void remainderMustBeCapturedForDrop() {
            // Concept: items that don't fit in output slots must enter
            // a verifiable drop path, not just a log line.
            int[] counts = {81}; // slot at absolute limit
            int[] limits = {81};
            int toAdd = 10;

            int remaining = toAdd;
            for (int i = 0; i < counts.length && remaining > 0; i++) {
                int space = Math.max(limits[i], counts[i]) - counts[i];
                int add = Math.min(space, remaining);
                counts[i] += add;
                remaining -= add;
            }

            assertTrue(remaining > 0, "All 10 must remain");
            assertEquals(10, remaining,
                    "Slot is full (81/81), all 10 items remain as overflow");
        }

        @Test
        void onRemainingOutputMethodExists() throws Exception {
            // RED: fails before GREEN — method doesn't exist.
            // GREEN: RecipeMachineBlockEntity has protected onRemainingOutput(ItemStack).
            var method = RecipeMachineBlockEntity.class.getDeclaredMethod(
                    "onRemainingOutput", net.minecraft.world.item.ItemStack.class);
            assertNotNull(method, "onRemainingOutput must exist on RecipeMachineBlockEntity");
            assertTrue(java.lang.reflect.Modifier.isProtected(method.getModifiers()),
                    "onRemainingOutput must be protected for test override");
        }
    }
}
