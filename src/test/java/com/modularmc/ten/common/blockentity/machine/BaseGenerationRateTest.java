package com.modularmc.ten.common.blockentity.machine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED: BASE_GENERATION_RATE constants do not exist yet in the three engine BEs.
 */
class BaseGenerationRateTest {

    @Test
    void extractorBaseGenerationRate_shouldBe30() {
        assertEquals(30, ExtractorBlockEntity.BASE_GENERATION_RATE);
    }

    @Test
    void metalizerBaseGenerationRate_shouldBe80() {
        assertEquals(80, MetalizerBlockEntity.BASE_GENERATION_RATE);
    }

    @Test
    void biomassBaseGenerationRate_shouldBe80() {
        assertEquals(80, BiomassBlockEntity.BASE_GENERATION_RATE);
    }
}
