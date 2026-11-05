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
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File
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
        EXTREME,
        CUSTOM
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
            Profile.CUSTOM -> Recommendation(
                profile = profile,
                maxMemoryMb = safeRamMb,
                renderDistance = if (ramMb <= 4096) 6 else 8,
                simulationDistance = 4,
                entityDistancePercent = 100,
                explanation = "Custom starts from a conservative device-safe limit. Adjust the values below to fit your device and monitor stability."
            )
        }
    }

    fun deviceSummary(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android device" }



    /** Inspect installed Java runtimes and the active launcher's Java selection. */
    fun javaDiagnostics(): String {
        val currentVersion = VersionsManager.currentVersion.value
        val installed = runCatching { RuntimesManager.getRuntimes(forceLoad = true) }
            .getOrElse { error -> 
                return "Unable to read installed Java runtimes: ${error.message ?: "unknown error"}"
            }
        val versionOverride = currentVersion?.getJavaRuntime()?.takeIf { it.isNotBlank() }
        val globalRuntime = AllSettings.javaRuntime.state.takeIf { it.isNotBlank() }
        val selectedName = versionOverride ?: globalRuntime
        val selected = installed.firstOrNull { it.name == selectedName }
        val compatible = installed.filter { it.isCompatible() }
        val selectedLabel = when {
            selectedName == null -> "automatic runtime selection"
            selected == null -> "selected runtime '$selectedName' is not installed"
            !selected.isCompatible() -> "selected runtime '$selectedName' is incompatible with this device architecture"
            else -> "Java ${selected.javaVersion} (${selected.name}, ${selected.arch})"
        }
        val autoPick = if (AllSettings.autoPickJavaRuntime.state) "enabled" else "disabled"
        val versionLabel = currentVersion?.getVersionName() ?: "no Minecraft version selected"
        val availableLabel = compatible
            .joinToString { "Java ${it.javaVersion}" }
            .ifBlank { "none detected" }
        return "Minecraft: $versionLabel • Selection: $selectedLabel • Auto-pick: $autoPick • Compatible installed runtimes: $availableLabel. Runtime compatibility also depends on the Minecraft version's Java requirement."
    }

    /** Inspect the selected installation and its client jar without changing files. */
    fun minecraftDiagnostics(): String {
        val version = VersionsManager.currentVersion.value
            ?: return "No Minecraft version is currently selected. Choose an installed version before launching."
        val info = version.getVersionInfo()
        val loader = info?.loaderInfos
            ?.joinToString { "${it.loader.displayName} ${it.version}" }
            ?.takeIf { it.isNotBlank() }
            ?: if (info == null) "metadata unavailable" else "Vanilla"
        val valid = version.isValid()
        val clientJar = version.getClientJar()
        val gameDirectory = version.getGameDir()
        return "Selected: ${version.getVersionName()} • Minecraft: ${info?.minecraftVersion ?: "unknown"} • Loader: $loader • Version folder: ${if (valid) "valid" else "missing/invalid"} • Client JAR: ${if (clientJar.isFile) "present" else "missing"} • Game data directory: ${if (gameDirectory.isDirectory) "present" else "not found"}. This checks installation files only; it does not launch or modify the game."
    }

    /** Count local mod files; compatibility still needs each mod's declared metadata. */
    fun modDiagnostics(): String {
        val version = VersionsManager.currentVersion.value
            ?: return "No Minecraft version is selected. Select a version before checking its mods."
        val modsDirectory = File(version.getGameDir(), "mods")
        if (!modsDirectory.isDirectory) {
            return "No mods folder exists for ${version.getVersionName()}. The selected installation currently has no local mods directory."
        }
        val candidates = modsDirectory.listFiles()
            ?.filter { file ->
                file.isFile && (
                    file.extension.equals("jar", ignoreCase = true) ||
                        file.name.endsWith(".jar.disabled", ignoreCase = true)
                    )
            }
            ?: return "The mods folder exists, but its contents could not be read."
        val disabled = candidates.count { it.name.endsWith(".disabled", ignoreCase = true) }
        val enabled = candidates.size - disabled
        return "Minecraft ${version.getVersionName()} • Mod files: ${candidates.size} • Enabled: $enabled • Disabled: $disabled. File counts do not prove version/loader compatibility; check each mod's declared Minecraft and loader support."
    }

    /** Query Android's declared graphics capabilities without guessing a GPU model. */
    fun graphicsDiagnostics(context: Context): String {
        val vulkan = runCatching {
            checkVulkanSupport(context.packageManager)
        }.getOrDefault(false)
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val glVersion = activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
        val glLabel = if (glVersion > 0) {
            "${glVersion shr 16}.${glVersion and 0xFFFF}"
        } else {
            "not reported"
        }
        val hardware = Build.HARDWARE.takeIf { it.isNotBlank() } ?: "not reported"
        return "OpenGL ES: $glLabel • Vulkan system feature flags: ${if (vulkan) "present" else "not both present"} • Android hardware identifier: $hardware. Exact GPU/driver capabilities depend on the selected renderer and driver; check Renderer settings for the live renderer."
    }

    /** Read basic Android memory and ABI details for a real local device report. */
    fun deviceDiagnostics(context: Context): String {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = (memoryInfo.totalMem / (1024L * 1024L)).toInt().coerceAtLeast(0)
        val availableRamMb = (memoryInfo.availMem / (1024L * 1024L)).toInt().coerceAtLeast(0)
        val ramLabel = if (totalRamMb > 0) {
            "$totalRamMb MB RAM • $availableRamMb MB available"
        } else {
            "RAM information unavailable"
        }
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown ABI"
        val hardware = Build.HARDWARE.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
        val lowRam = if (activityManager?.isLowRamDevice == true) "yes" else "no"

        return "${deviceSummary()}$hardware • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • $ramLabel • $abi • Android low-RAM device: $lowRam"
    }
}
