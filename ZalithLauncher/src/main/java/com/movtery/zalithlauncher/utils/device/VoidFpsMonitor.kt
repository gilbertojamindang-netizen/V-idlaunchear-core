/*
 * VØID coarse FPS-drop monitor.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.utils.device

/**
 * Detect a large one-second FPS drop against the recent rolling baseline.
 *
 * This intentionally does not claim frame-time or 1% low measurement. The
 * launcher samples the game FPS once per second, so this is a coarse warning.
 */
object VoidFpsMonitor {
    fun hasSevereRecentDrop(history: List<Int>): Boolean {
        if (history.size < 4) return false

        val current = history.last().coerceAtLeast(0)
        val baselineSamples = history.dropLast(1).takeLast(5).filter { it > 0 }
        if (baselineSamples.size < 3) return false

        val baseline = baselineSamples.average()
        if (baseline < 30.0) return false

        return current <= 45 && current <= baseline * 0.65
    }
}
