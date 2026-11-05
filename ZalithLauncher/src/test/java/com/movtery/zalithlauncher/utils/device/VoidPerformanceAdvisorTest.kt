/*
 * VØID performance recommendation tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later.
 */
package com.movtery.zalithlauncher.utils.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidPerformanceAdvisorTest {
    @Test
    fun batterySaverUsesConservativeDistancesAndMemory() {
        val result = VoidPerformanceAdvisor.recommend(
            4096,
            VoidPerformanceAdvisor.Profile.BATTERY_SAVER
        )
        assertEquals(4, result.renderDistance)
        assertEquals(3, result.simulationDistance)
        assertEquals(50, result.entityDistancePercent)
        assertTrue(result.maxMemoryMb <= 1536)
    }

    @Test
    fun lowMemoryExtremeProfileIsCappedToConservativeDistances() {
        val result = VoidPerformanceAdvisor.recommend(
            3072,
            VoidPerformanceAdvisor.Profile.EXTREME
        )
        assertEquals(8, result.renderDistance)
        assertEquals(5, result.simulationDistance)
        assertTrue(result.maxMemoryMb <= 1075)
    }

    @Test
    fun memoryRecommendationNeverExceedsSafeFractionOrHardCap() {
        val result = VoidPerformanceAdvisor.recommend(
            32768,
            VoidPerformanceAdvisor.Profile.PERFORMANCE
        )
        assertEquals(4096, result.maxMemoryMb)
        assertTrue(result.maxMemoryMb < 32768)
    }

    @Test
    fun invalidRamEstimateIsClampedToSafeMinimum() {
        val result = VoidPerformanceAdvisor.recommend(
            0,
            VoidPerformanceAdvisor.Profile.BALANCED
        )
        assertTrue(result.maxMemoryMb >= 512)
        assertTrue(result.maxMemoryMb <= 512)
    }
}
