package com.modularmc.ten.common.blockentity.machine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract: pins BASE_GENERATION_RATE for all four engine block entities.
 * Solar self-consistency: 600 fuel / 10 FE/t = 60 ticks per cycle.
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

    @Test
    void solarBaseGenerationRate_shouldBe10() {
        assertEquals(10, SolarBlockEntity.BASE_GENERATION_RATE);
    }
}
