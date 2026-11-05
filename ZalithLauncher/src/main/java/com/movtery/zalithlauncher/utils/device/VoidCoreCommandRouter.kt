/*
 * VØID CORE local command router.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.utils.device

import android.content.Context
import android.os.Build

/**
 * Offline command interpretation for the VØID CORE assistant.
 *
 * This class deliberately routes requests instead of pretending to execute
 * device or game changes. Callers must run diagnostics through the relevant
 * subsystem and ask for confirmation before any mutating operation.
 */
object VoidCoreCommandRouter {
    enum class Action {
        SHOW_OPTIMIZATION_ADVICE,
        SHOW_LOW_END_ADVICE,
        OPEN_GPU_DIAGNOSTICS,
        OPEN_JAVA_DIAGNOSTICS,
        REVIEW_GAME_SETTINGS,
        RESTORE_BACKUP,
        REVIEW_MODS,
        SHOW_FPS_GUIDANCE,
        REVIEW_MINECRAFT_INSTALLATION,
        SHOW_DEVICE_SUMMARY,
        CLEAN_CACHE,
        HELP
    }

    data class Result(
        val action: Action,
        val title: String,
        val message: String,
        val requiresConfirmation: Boolean = false
    )

    fun route(rawCommand: String, context: Context? = null): Result {
        val command = rawCommand.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .removePrefix("/")

        return when (command) {
            "optimize", "optimization" -> Result(
                Action.SHOW_OPTIMIZATION_ADVICE,
                "Optimization advice",
                "I can review a conservative profile and explain suggested memory, render-distance, and simulation-distance values. Nothing is changed automatically."
            )
            "low-end", "low end" -> Result(
                Action.SHOW_LOW_END_ADVICE,
                "Low-end profile",
                "Use a conservative profile, keep memory headroom for Android, and lower render and simulation distance before testing again."
            )
            "check gpu", "gpu" -> Result(
                Action.OPEN_GPU_DIAGNOSTICS,
                "Graphics diagnostics",
                context?.let { VoidPerformanceAdvisor.graphicsDiagnostics(it) }
                    ?: "Open Renderer settings to inspect the available graphics APIs and renderer driver. A live report requires an Android context."
            )
            "check java", "java" -> Result(
                Action.OPEN_JAVA_DIAGNOSTICS,
                "Java diagnostics",
                VoidPerformanceAdvisor.javaDiagnostics()
            )
            "fix settings", "review settings" -> Result(
                Action.REVIEW_GAME_SETTINGS,
                "Settings review",
                "I can compare the current settings with conservative recommendations. Review each proposed change before applying it."
            )
            "restore settings", "restore" -> Result(
                Action.RESTORE_BACKUP,
                "Restore settings",
                "Choose an existing backup and review its contents before restoring. No backup is overwritten automatically.",
                requiresConfirmation = true
            )
            "check mods", "mods" -> Result(
                Action.REVIEW_MODS,
                "Installed mod inventory",
                VoidPerformanceAdvisor.modDiagnostics()
            )
            "fps", "check fps" -> Result(
                Action.SHOW_FPS_GUIDANCE,
                "FPS and frame pacing",
                "Compare average FPS with frame-time stability. If FPS drops while moving, test a lower render distance and allow chunk loading to settle."
            )
            "device", "check device", "device info" -> Result(
                Action.SHOW_DEVICE_SUMMARY,
                "Device summary",
                context?.let { VoidPerformanceAdvisor.deviceDiagnostics(it) }
                    ?: "${VoidPerformanceAdvisor.deviceSummary()} • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}). Basic device identification only; live memory details require an Android context."
            )
            "check minecraft", "minecraft" -> Result(
                Action.REVIEW_MINECRAFT_INSTALLATION,
                "Minecraft installation",
                VoidPerformanceAdvisor.minecraftDiagnostics()
            )
            "clean cache", "clear cache" -> Result(
                Action.CLEAN_CACHE,
                "Cache cleanup",
                "Review cache categories and sizes first. Only disposable cache files should be removed; worlds, accounts, saves, and game data must be preserved.",
                requiresConfirmation = true
            )
            "help", "commands", "" -> Result(
                Action.HELP,
                "VØID CORE commands",
                "Try /device, /optimize, /low-end, /check gpu, /check java, /fix settings, /restore settings, /check mods, /fps, /check minecraft, or /clean cache."
            )
            else -> Result(
                Action.HELP,
                "Command not recognized",
                "I don't recognize that offline command. Type /help to see the supported commands."
            )
        }
    }
}
