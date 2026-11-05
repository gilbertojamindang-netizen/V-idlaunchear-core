/*
 * VØID FPS drop monitor tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later.
 */
package com.movtery.zalithlauncher.utils.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidFpsMonitorTest {
    @Test
    fun detectsLargeDropAgainstRecentBaseline() {
        assertTrue(VoidFpsMonitor.hasSevereRecentDrop(listOf(60, 61, 59, 62, 60, 35)))
    }

    @Test
    fun ignoresStableFrameRateSamples() {
        assertFalse(VoidFpsMonitor.hasSevereRecentDrop(listOf(60, 62, 61, 60, 59, 61)))
    }

    @Test
    fun ignoresLowBaselineAndInsufficientSamples() {
        assertFalse(VoidFpsMonitor.hasSevereRecentDrop(listOf(18, 19, 20, 8)))
        assertFalse(VoidFpsMonitor.hasSevereRecentDrop(listOf(60, 60, 20)))
    }

    @Test
    fun ignoresMissingOrNonPositivePriorSamples() {
        assertFalse(VoidFpsMonitor.hasSevereRecentDrop(listOf(0, 0, 0, 0, 35)))
    }
}
