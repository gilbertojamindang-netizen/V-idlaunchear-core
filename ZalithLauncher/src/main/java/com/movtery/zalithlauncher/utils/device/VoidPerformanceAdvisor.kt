/*
 * VøidLaunchear performance advisor
 * Copyright (C) 2026 Gilbert Jamindang and contributors
 *
 * This file is distributed under the same GPL-3.0-or-later license as the
 * upstream Zalith Launcher 2 project.
 */

package com.movtery.zalithlauncher.utils.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlin.math.min

/**
 * Conservative, advisory-only device profile estimates.
 *
 * This does not change Android process limits or Minecraft settings. The UI
 * must ask the player before applying any suggested values.
 */
object VoidPerformanceAdvisor {
    enum class Profile {
        BATTERY_SAVER,
        BALANCED,
        PERFORMANCE,
        EXTREME
    }

    data class Recommendation(
        val profile: Profile,
        val maxMemoryMb: Int,
        val renderDistance: Int,
        val simulationDistance: Int,
        val entityDistancePercent: Int,
        val explanation: String
    )

    fun recommend(context: Context): Recommendation {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = if (memoryInfo.totalMem > 0L) {
            (memoryInfo.totalMem / (1024L * 1024L)).toInt()
        } else {
            2048
        }

        // Keep a large reserve for Android, the launcher, graphics drivers and
        // native allocations. This is a suggestion, not a promise of free RAM.
        val safeRamMb = min((totalRamMb * 0.35f).toInt(), 4096).coerceAtLeast(512)
        val lowRam = activityManager?.isLowRamDevice == true || totalRamMb <= 4096

        return when {
            lowRam -> Recommendation(
                profile = Profile.BALANCED,
                maxMemoryMb = min(safeRamMb, 2048),
                renderDistance = 6,
                simulationDistance = 4,
                entityDistancePercent = 75,
                explanation = "Conservative settings help leave memory and CPU time for Android and chunk loading."
            )
            totalRamMb <= 6144 -> Recommendation(
                profile = Profile.BALANCED,
                maxMemoryMb = min(safeRamMb, 3072),
                renderDistance = 8,
                simulationDistance = 5,
                entityDistancePercent = 100,
                explanation = "Balanced settings aim for steadier frame times without using most available memory."
            )
            else -> Recommendation(
                profile = Profile.PERFORMANCE,
                maxMemoryMb = safeRamMb,
                renderDistance = 10,
                simulationDistance = 6,
                entityDistancePercent = 100,
                explanation = "Your device reports more memory headroom; increase settings gradually and monitor frame times."
            )
        }
    }

    fun deviceSummary(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android device" }
}
