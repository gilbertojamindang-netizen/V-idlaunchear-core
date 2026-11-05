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
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.game.version.mod.isEnabled
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
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
        return "Selected: ${version.getVersionName()} • Minecraft: ${info?.minecraftVersion ?: "unknown"} • Loader: $loader • Version folder: ${if (valid) "valid" else "missing/invalid"} • Client JAR at direct version path: ${if (clientJar.isFile) "present" else "not present (may be inherited or incomplete)"} • Game data directory: ${if (gameDirectory.isDirectory) "present" else "not found"}. This checks installation files only; it does not launch or modify the game."
    }

    private data class DiagnosticLog(val file: File, val content: String)

    /** Read only the tail of the newest local crash/log file to keep memory bounded. */
    private fun latestDiagnosticLog(): DiagnosticLog? {
        val version = VersionsManager.currentVersion.value ?: return null
        val gameDirectory = version.getGameDir()
        val candidates = buildList {
            add(File(gameDirectory, "logs/latest.log"))
            File(gameDirectory, "crash-reports").listFiles()
                ?.filter { it.isFile && (it.extension.equals("txt", true) || it.extension.equals("log", true)) }
                ?.let(::addAll)
        }.filter { it.isFile }
        val latest = candidates.maxByOrNull { it.lastModified() } ?: return null
        return runCatching {
            RandomAccessFile(latest, "r").use { file ->
                val byteCount = minOf(file.length(), 512L * 1024L)
                val bytes = ByteArray(byteCount.toInt())
                file.seek((file.length() - byteCount).coerceAtLeast(0L))
                file.readFully(bytes)
                DiagnosticLog(latest, String(bytes, StandardCharsets.UTF_8))
            }
        }.getOrNull()
    }

    private fun classifyDiagnosticLog(log: DiagnosticLog?): String {
        if (log == null) return "No recent Minecraft latest.log or crash report was found for the selected installation."
        val lower = log.content.lowercase()
        val findings = mutableListOf<String>()
        if ("outofmemoryerror" in lower || "gc overhead limit exceeded" in lower) {
            findings += "Memory exhaustion detected; compare the selected Java allocation with Android RAM headroom and reduce render distance if the device is under pressure."
        }
        if ("unsupportedclassversionerror" in lower || "class file version" in lower) {
            findings += "Java bytecode/runtime mismatch detected; check the Minecraft version's required Java major version and the selected runtime."
        }
        if ("mixinapplyerror" in lower || "mixin apply failed" in lower || "modresolutionexception" in lower || "incompatible mod set" in lower) {
            findings += "A loader or mod dependency conflict is indicated by the log; compare each mod's Minecraft and loader versions and remove recently added mods one at a time."
        }
        if ("noclassdeffounderror" in lower || "nosuchmethoderror" in lower || "classnotfoundexception" in lower) {
            findings += "A missing or incompatible class/API is indicated; check mod dependencies and loader compatibility."
        }
        if ("glfwerror" in lower || "opengl error" in lower || "vulkan" in lower || "egl_bad_match" in lower || "lwjgl" in lower) {
            findings += "A graphics/renderer failure is indicated; test the alternate supported renderer and update or reinstall the matching graphics driver/plugin."
        }
        if ("sslhandshakeexception" in lower || "unknownhostexception" in lower || "failed to download" in lower) {
            findings += "A network/download failure is indicated; check the connection and retry the affected download."
        }
        if (findings.isEmpty()) {
            findings += "No common Java, mod-loader, or renderer signature was detected in the log tail. The full report may contain details outside the scanned tail."
        }
        val excerpt = log.content.lineSequence()
            .filter { line ->
                val value = line.lowercase()
                listOf("exception", "error", "failed", "mixin", "outofmemory", "unsupportedclass").any(value::contains)
            }
            .takeLast(3)
            .joinToString(" | ")
            .take(360)
        return "Report: ${log.file.name} • Updated: ${log.file.lastModified()} • ${findings.joinToString(" ")}" +
            if (excerpt.isNotBlank()) " Log clues: $excerpt" else ""
    }

    /** Read-only crash/log guidance for the selected Minecraft installation. */
    fun crashDiagnostics(): String = classifyDiagnosticLog(latestDiagnosticLog())

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
        return "Minecraft ${version.getVersionName()} • Mod files: ${candidates.size} • Enabled: $enabled • Disabled: $disabled. File counts do not prove version/loader compatibility; check each mod's declared Minecraft and loader support. Latest log scan: ${classifyDiagnosticLog(latestDiagnosticLog())}"
    }

    internal data class ModMetadataSnapshot(
        val fileName: String,
        val id: String,
        val loader: ModLoader,
        val enabled: Boolean,
        val recognized: Boolean
    )

    /**
     * Compare parsed local mod loader metadata with the selected profile.
     * This is an offline review, not a guarantee of Minecraft-version or dependency compatibility.
     */
    internal fun buildModCompatibilityReport(
        versionName: String,
        installedLoaders: Set<ModLoader>?,
        modFiles: List<ModMetadataSnapshot>
    ): String {
        val enabled = modFiles.filter { it.enabled && it.recognized }
        val disabled = modFiles.count { !it.enabled }
        val unrecognized = modFiles.count { it.enabled && !it.recognized }

        val mismatched = if (installedLoaders == null) {
            emptyList()
        } else {
            enabled.filter { mod ->
                mod.loader.isLoader &&
                    mod.loader !in installedLoaders &&
                    !(mod.loader == ModLoader.FABRIC && ModLoader.QUILT in installedLoaders)
            }
        }
        val duplicates = enabled
            .filter { it.id.isNotBlank() }
            .groupBy { it.id.lowercase(Locale.ROOT) }
            .filterValues { it.size > 1 }
            .map { (id, entries) ->
                "$id (${entries.joinToString { it.fileName }})"
            }

        val loaderLabel = when {
            installedLoaders == null -> "metadata unavailable"
            installedLoaders.isEmpty() -> "Vanilla"
            else -> installedLoaders.joinToString { it.displayName }
        }
        val mismatchLabel = if (mismatched.isEmpty()) {
            "No obvious loader mismatch found."
        } else {
            "Loader mismatch review: " + mismatched.joinToString { "${it.loader.displayName}: ${it.fileName}" } + "."
        }
        val duplicateLabel = if (duplicates.isEmpty()) {
            "No duplicate mod IDs found."
        } else {
            "Duplicate mod IDs: ${duplicates.joinToString()}."
        }
        return "Minecraft $versionName • Installed loader: $loaderLabel • Parsed enabled mods: ${enabled.size} • Disabled files: $disabled • Unrecognized JARs: $unrecognized. $mismatchLabel $duplicateLabel This offline scan checks declared loader metadata and duplicate IDs only; it cannot guarantee Minecraft-version or dependency compatibility."
    }

    /** Read mod JAR metadata locally and compare loader IDs against the selected install. */
    suspend fun modCompatibilityDiagnostics(): String {
        val version = VersionsManager.currentVersion.value
            ?: return "No Minecraft version is selected. Select an installed version before checking mod compatibility."
        val directory = File(version.getGameDir(), "mods")
        if (!directory.isDirectory) {
            return "No mods folder exists for ${version.getVersionName()}. There are no local mod files to inspect."
        }

        val parsed = runCatching { AllModReader(directory).readAllLocals() }
            .getOrElse { error ->
                return "Unable to read local mod metadata: ${error.message ?: "unknown error"}"
            }
        val snapshots = parsed.map { mod ->
            ModMetadataSnapshot(
                fileName = mod.file.name,
                id = mod.id,
                loader = mod.loader,
                enabled = mod.file.isEnabled(),
                recognized = !mod.notMod && mod.loader != ModLoader.UNKNOWN
            )
        }
        val installedLoaders = version.getVersionInfo()?.loaderInfos
            ?.map { it.loader }
            ?.filter { it.isLoader }
            ?.toSet()
        val report = buildModCompatibilityReport(
            versionName = version.getVersionName(),
            installedLoaders = installedLoaders,
            modFiles = snapshots
        )
        return "$report Crash log: ${classifyDiagnosticLog(latestDiagnosticLog())}".take(2400)
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
