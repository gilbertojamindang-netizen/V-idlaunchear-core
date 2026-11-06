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
    fun cpuParserPrefersHardwareOrModelName() {
        val hardware = VoidPerformanceAdvisor.parseCpuModel(
            "processor : 0\nHardware : MediaTek MT6769V\n"
        )
        assertEquals("MediaTek MT6769V", hardware)

        val modelName = VoidPerformanceAdvisor.parseCpuModel(
            "model name : Example CPU 8-Core\n"
        )
        assertEquals("Example CPU 8-Core", modelName)
    }

    @Test
    fun cpuParserReturnsNullWhenNoModelFieldsExist() {
        assertEquals(null, VoidPerformanceAdvisor.parseCpuModel("flags : aes\n"))
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

    @Test
    fun everyProfileStaysWithinMemoryAndDistanceSafetyBoundsAcrossDeviceSizes() {
        val ramSizes = listOf(512, 1024, 2048, 3072, 4096, 6144, 8192, 16384, 32768)
        val profiles = VoidPerformanceAdvisor.Profile.values()

        ramSizes.forEach { ram ->
            profiles.forEach { profile ->
                val result = VoidPerformanceAdvisor.recommend(ram, profile)
                val safeLimit = minOf((ram.coerceAtLeast(512) * 0.35f).toInt(), 4096)
                    .coerceAtLeast(512)

                assertEquals(profile, result.profile)
                assertTrue("RAM recommendation exceeded safe cap: RAM=$ram profile=$profile", result.maxMemoryMb in 512..safeLimit)
                assertTrue("Render distance outside supported range: $result", result.renderDistance in 2..24)
                assertTrue("Simulation distance outside supported range: $result", result.simulationDistance in 2..16)
                assertTrue("Entity distance outside percentage range: $result", result.entityDistancePercent in 0..200)
                assertTrue("Missing recommendation explanation: $result", result.explanation.isNotBlank())
            }
        }
    }

    @Test
    fun balancedProfileDoesNotAllocateMostOfLowEndDeviceRam() {
        listOf(1024, 2048, 3072, 4096).forEach { ram ->
            val result = VoidPerformanceAdvisor.recommend(
                ram,
                VoidPerformanceAdvisor.Profile.BALANCED
            )
            assertTrue("Balanced profile should leave substantial native/Android headroom", result.maxMemoryMb <= maxOf(512f, ram * 0.35f))
        }
    }

}
