/*
 * VØID Smart Lag Analyzer tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later.
 */
package com.movtery.zalithlauncher.utils.device

import java.nio.file.Files
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidSmartLagAnalyzerTest {
    @Test
    fun missingModsDirectoryReturnsUnknownWithoutThrowing() {
        val directory = Files.createTempDirectory("void-missing-mods").toFile()
        try {
            val report = VoidSmartLagAnalyzer.inspectModpack(directory.resolve("mods"))
            assertEquals(VoidSmartLagAnalyzer.ModpackLoad.UNKNOWN, report.load)
            assertEquals(0, report.enabledMods)
            assertTrue(report.recommendation.isNotBlank())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun ignoresNonJarFilesAndCountsEnabledJarFiles() {
        val directory = Files.createTempDirectory("void-mods").toFile()
        try {
            File(directory, "one.jar").writeBytes(ByteArray(1024))
            File(directory, "two.JAR").writeBytes(ByteArray(2048))
            File(directory, "readme.txt").writeText("not a mod")
            val report = VoidSmartLagAnalyzer.inspectModpack(directory)
            assertEquals(2, report.enabledMods)
            assertEquals(3072L, report.totalBytes)
            assertEquals(2048L, report.largestModBytes)
            assertEquals(VoidSmartLagAnalyzer.ModpackLoad.LIGHT, report.load)
        } finally {
            directory.deleteRecursively()
        }
    }
}
