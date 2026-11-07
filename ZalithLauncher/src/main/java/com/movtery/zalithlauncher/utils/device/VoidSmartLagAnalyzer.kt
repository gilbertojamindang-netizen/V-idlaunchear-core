/*
 * VØID Smart Lag Analyzer
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.utils.device

import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.io.File

/**
 * Conservative, read-only signals for the Smart Lag Fix UI.
 * Mod-pack weight is a file-size/count heuristic, not a benchmark or compatibility verdict.
 */
object VoidSmartLagAnalyzer {
    enum class ModpackLoad { LIGHT, MODERATE, HEAVY, VERY_HEAVY, UNKNOWN }
    enum class HeatLevel { NORMAL, MODERATE, SEVERE, CRITICAL, EMERGENCY, UNKNOWN }

    data class ModpackReport(
        val load: ModpackLoad,
        val enabledMods: Int,
        val totalBytes: Long,
        val largestModBytes: Long,
        val recommendation: String
    )

    fun inspectModpack(modsDirectory: File): ModpackReport {
        if (!modsDirectory.isDirectory || !modsDirectory.canRead()) {
            return ModpackReport(ModpackLoad.UNKNOWN, 0, 0L, 0L,
                "Mods folder is unavailable; no modpack estimate was made.")
        }
        val jars = modsDirectory.listFiles()
            ?.filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
            ?: return ModpackReport(ModpackLoad.UNKNOWN, 0, 0L, 0L,
                "Mods folder could not be read; no modpack estimate was made.")
        val total = jars.sumOf { it.length().coerceAtLeast(0L) }
        val largest = jars.maxOfOrNull { it.length().coerceAtLeast(0L) } ?: 0L
        val count = jars.size
        val totalMb = total / (1024L * 1024L)
        val load = when {
            count == 0 -> ModpackLoad.LIGHT
            count >= 180 || totalMb >= 1200 -> ModpackLoad.VERY_HEAVY
            count >= 90 || totalMb >= 600 -> ModpackLoad.HEAVY
            count >= 35 || totalMb >= 250 -> ModpackLoad.MODERATE
            else -> ModpackLoad.LIGHT
        }
        val advice = when (load) {
            ModpackLoad.LIGHT -> "Mod file weight looks light. Check renderer, heat and view distance if lag continues."
            ModpackLoad.MODERATE -> "Try a lower render and simulation distance; check large content mods and resource packs."
            ModpackLoad.HEAVY -> "Try the Battery Saver or Balanced profile and review mod dependencies and background apps."
            ModpackLoad.VERY_HEAVY -> "This mod folder is very large. Test a copy with optional content mods disabled; keep a backup."
            ModpackLoad.UNKNOWN -> "Modpack weight could not be estimated."
        }
        return ModpackReport(load, count, total, largest, advice)
    }

    /** Android thermal state is available from API 29. This reads status only. */
    fun thermalStatus(context: Context): HeatLevel {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return HeatLevel.UNKNOWN
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return HeatLevel.UNKNOWN
        return when (manager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> HeatLevel.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> HeatLevel.MODERATE
            PowerManager.THERMAL_STATUS_MODERATE -> HeatLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> HeatLevel.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> HeatLevel.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> HeatLevel.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> HeatLevel.EMERGENCY
            else -> HeatLevel.UNKNOWN
        }
    }

    fun thermalAdvice(level: HeatLevel): String = when (level) {
        HeatLevel.NORMAL -> "Device thermal status is normal."
        HeatLevel.MODERATE -> "Device is warming up. Lower render distance or take a short break if performance drops."
        HeatLevel.SEVERE -> "High thermal pressure detected. Stop the game and let the device cool before continuing."
        HeatLevel.CRITICAL, HeatLevel.EMERGENCY ->
            "Critical thermal pressure detected. Stop gameplay and allow the device to cool."
        HeatLevel.UNKNOWN -> "Thermal status is unavailable on this Android version or device."
    }
}
