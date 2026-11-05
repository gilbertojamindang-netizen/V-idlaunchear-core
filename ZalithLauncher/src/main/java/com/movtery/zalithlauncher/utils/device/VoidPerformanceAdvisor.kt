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
        val detectedProfile = if (activityManager?.isLowRamDevice == true || totalRamMb <= 6144) {
            Profile.BALANCED
        } else {
            Profile.PERFORMANCE
        }
        return recommend(totalRamMb, detectedProfile)
    }

    /** Recalculate a selected profile using the device's reported physical RAM. */
    fun recommend(context: Context, profile: Profile): Recommendation {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamMb = if (memoryInfo.totalMem > 0L) {
            (memoryInfo.totalMem / (1024L * 1024L)).toInt()
        } else {
            2048
        }
        return recommend(totalRamMb, profile)
    }


    /**
     * Build an advisory profile from a RAM estimate. This overload is pure and
     * testable; it never applies values to Minecraft or changes Android limits.
     */
    fun recommend(totalRamMb: Int, profile: Profile): Recommendation {
        val ramMb = totalRamMb.coerceAtLeast(512)
        // Leave at least 65% of reported RAM for Android, graphics/native
        // allocations, the launcher, and other processes.
        val safeRamMb = min((ramMb * 0.35f).toInt(), 4096).coerceAtLeast(512)

        return when (profile) {
            Profile.BATTERY_SAVER -> Recommendation(
                profile = profile,
                maxMemoryMb = min(safeRamMb, 1536),
                renderDistance = 4,
                simulationDistance = 3,
                entityDistancePercent = 50,
                explanation = "Battery Saver favors lower background load and shorter view distance. These are suggestions only."
            )
            Profile.BALANCED -> {
                val lowRam = ramMb <= 4096
                Recommendation(
                    profile = profile,
                    maxMemoryMb = min(safeRamMb, if (lowRam) 2048 else 3072),
                    renderDistance = if (lowRam) 6 else 8,
                    simulationDistance = if (lowRam) 4 else 5,
                    entityDistancePercent = if (lowRam) 75 else 100,
                    explanation = "Balanced settings aim for steadier frame times while preserving memory headroom."
                )
            }
            Profile.PERFORMANCE -> Recommendation(
                profile = profile,
                maxMemoryMb = safeRamMb,
                renderDistance = if (ramMb <= 4096) 6 else 10,
                simulationDistance = if (ramMb <= 4096) 4 else 6,
                entityDistancePercent = 100,
                explanation = "Performance favors higher view distance when memory headroom allows. Monitor frame times and heat."
            )
            Profile.EXTREME -> Recommendation(
                profile = profile,
                maxMemoryMb = safeRamMb,
                renderDistance = if (ramMb < 6144) 8 else 12,
                simulationDistance = if (ramMb < 6144) 5 else 8,
                entityDistancePercent = 100,
                explanation = "Extreme is an experimental recommendation, not a guaranteed FPS boost. Reduce settings if frame times or heat worsen."
            )
        }
    }

    fun deviceSummary(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android device" }
}
