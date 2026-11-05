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
    fun customProfileStartsWithDeviceSafeMemoryLimit() {
        val result = VoidPerformanceAdvisor.recommend(
            8192,
            VoidPerformanceAdvisor.Profile.CUSTOM
        )
        assertEquals(VoidPerformanceAdvisor.Profile.CUSTOM, result.profile)
        assertEquals(2867, result.maxMemoryMb)
        assertTrue(result.renderDistance in 2..24)
        assertTrue(result.simulationDistance in 2..16)
    }

    @Test
    fun modScanFlagsLoaderMismatchAgainstSelectedLoader() {
        val report = VoidPerformanceAdvisor.buildModCompatibilityReport(
            versionName = "1.21.1",
            installedLoaders = setOf(com.movtery.zalithlauncher.game.addons.modloader.ModLoader.FORGE),
            modFiles = listOf(
                VoidPerformanceAdvisor.ModMetadataSnapshot(
                    fileName = "fabric-example.jar",
                    id = "example",
                    loader = com.movtery.zalithlauncher.game.addons.modloader.ModLoader.FABRIC,
                    enabled = true,
                    recognized = true
                )
            )
        )
        assertTrue(report.contains("Loader mismatch review"))
        assertTrue(report.contains("Fabric: fabric-example.jar"))
    }

    @Test
    fun modScanFlagsDuplicateIdsWithoutAssumingVersionCompatibility() {
        val loader = com.movtery.zalithlauncher.game.addons.modloader.ModLoader.FABRIC
        val report = VoidPerformanceAdvisor.buildModCompatibilityReport(
            versionName = "1.21.1",
            installedLoaders = setOf(loader),
            modFiles = listOf(
                VoidPerformanceAdvisor.ModMetadataSnapshot("first.jar", "Example", loader, true, true),
                VoidPerformanceAdvisor.ModMetadataSnapshot("second.jar", "example", loader, true, true),
                VoidPerformanceAdvisor.ModMetadataSnapshot("old.jar", "disabled", loader, false, true),
                VoidPerformanceAdvisor.ModMetadataSnapshot("notes.jar", "", com.movtery.zalithlauncher.game.addons.modloader.ModLoader.UNKNOWN, true, false)
            )
        )
        assertTrue(report.contains("Duplicate mod IDs: example"))
        assertTrue(report.contains("Disabled files: 1"))
        assertTrue(report.contains("Unrecognized JARs: 1"))
        assertTrue(report.contains("cannot guarantee Minecraft-version or dependency compatibility"))
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
