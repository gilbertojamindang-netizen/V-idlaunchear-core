/*
 * VøidLaunchear performance settings applier.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.utils.device

import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

/**
 * Applies a reviewed VØID recommendation to the currently selected installation.
 * Existing options.txt and version.config files are copied before any modification.
 */
object VoidPerformanceApplier {
    data class ApplyResult(
        val minecraftVersion: String,
        val memoryMb: Int,
        val renderDistance: Int,
        val simulationDistance: Int,
        val entityDistancePercent: Int,
        val backupDirectory: File,
        val backupId: String,
        val backedUpFiles: List<String>
    )

    data class RestoreResult(
        val minecraftVersion: String,
        val restoredFiles: List<String>,
        val safetyBackupDirectory: File
    )

    suspend fun apply(recommendation: VoidPerformanceAdvisor.Recommendation): ApplyResult =
        withContext(Dispatchers.IO) {
            val version = VersionsManager.currentVersion.value
                ?: error("No Minecraft version is selected.")
            if (!version.isValid()) error("The selected Minecraft installation is not valid.")

            val gameDirectory = version.getGameDir()
            if (!gameDirectory.exists() && !gameDirectory.mkdirs()) {
                error("Unable to access the selected Minecraft game directory.")
            }
            if (!gameDirectory.isDirectory) error("The selected game path is not a directory.")

            val optionsFile = File(gameDirectory, "options.txt")
            val configFile = File(version.getZalithVersionPath(), "version.config")
            val backupDirectory = File(gameDirectory, ".void-backups")
            if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
                error("Unable to create the VØID backup directory.")
            }

            val backupId = "void-settings-${System.currentTimeMillis()}"
            val optionsExisted = optionsFile.isFile
            val configExisted = configFile.isFile
            val backedUp = mutableListOf<String>()
            if (optionsFile.isFile) {
                optionsFile.copyTo(File(backupDirectory, "$backupId-options.txt"), overwrite = false)
                backedUp.add("options.txt")
            }
            if (configFile.isFile) {
                configFile.copyTo(File(backupDirectory, "$backupId-version.config"), overwrite = false)
                backedUp.add("version.config")
            }

            File(backupDirectory, "$backupId.meta").writeText(
                listOf(
                    "version=${version.getVersionName()}",
                    "optionsExisted=$optionsExisted",
                    "configExisted=$configExisted"
                ).joinToString(System.lineSeparator())
            )

            val safeMemory = recommendation.maxMemoryMb.coerceIn(512, 4096)
            val renderDistance = recommendation.renderDistance.coerceIn(2, 24)
            val simulationDistance = recommendation.simulationDistance.coerceIn(2, 16)
            val entityDistance = recommendation.entityDistancePercent.coerceIn(25, 150)
            val previousMemory = version.getVersionConfig().ramAllocation

            val lines = if (optionsFile.isFile) {
                optionsFile.readLines().toMutableList()
            } else {
                mutableListOf()
            }

            fun putOption(key: String, value: String) {
                val index = lines.indexOfFirst { line ->
                    val separator = line.indexOfFirst { it == ':' || it == '=' }
                    separator > 0 && line.substring(0, separator).trim() == key
                }
                if (index >= 0) {
                    val line = lines[index]
                    val separatorIndex = line.indexOfFirst { it == ':' || it == '=' }
                    val separator = line[separatorIndex]
                    lines[index] = "$key$separator$value"
                } else {
                    // Minecraft options.txt uses key:value lines.
                    lines.add("$key:$value")
                }
            }

            putOption("renderDistance", renderDistance.toString())
            putOption("simulationDistance", simulationDistance.toString())
            putOption("entityDistanceScaling", String.format(Locale.US, "%.2f", entityDistance / 100f))

            val tempOptions = File(gameDirectory, ".options.txt.void-tmp")
            val updatedText = lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator())
            try {
                tempOptions.writeText(updatedText)
                version.getVersionConfig().ramAllocation = safeMemory
                version.getVersionConfig().saveWithThrowable()

                try {
                    Files.move(
                        tempOptions.toPath(),
                        optionsFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(
                        tempOptions.toPath(),
                        optionsFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } catch (error: Throwable) {
                tempOptions.delete()
                // The original values remain available in the timestamped backup copies.
                version.getVersionConfig().ramAllocation = previousMemory
                runCatching { version.getVersionConfig().saveWithThrowable() }
                throw error
            }

            ApplyResult(
                minecraftVersion = version.getVersionName(),
                memoryMb = safeMemory,
                renderDistance = renderDistance,
                simulationDistance = simulationDistance,
                entityDistancePercent = entityDistance,
                backupDirectory = backupDirectory,
                backupId = backupId,
                backedUpFiles = backedUp
            )
        }

    /**
     * Restore a previously saved profile snapshot. Current files are copied first,
     * and the backup identifier is restricted to the generated timestamp format.
     */
    suspend fun restore(backupId: String): RestoreResult = withContext(Dispatchers.IO) {
        require(backupId.matches(Regex("^void-settings-[0-9]+$"))) {
            "Invalid VØID backup identifier."
        }
        val version = VersionsManager.currentVersion.value
            ?: error("No Minecraft version is selected.")
        if (!version.isValid()) error("The selected Minecraft installation is not valid.")

        val gameDirectory = version.getGameDir()
        val backupDirectory = File(gameDirectory, ".void-backups")
        if (!backupDirectory.isDirectory) error("No VØID backup directory exists for this installation.")

        val optionsBackup = File(backupDirectory, "$backupId-options.txt")
        val configBackup = File(backupDirectory, "$backupId-version.config")
        val manifestFile = File(backupDirectory, "$backupId.meta")
        if (!manifestFile.isFile && !optionsBackup.isFile && !configBackup.isFile) {
            error("That backup was not found for the selected Minecraft installation.")
        }
        val manifest = if (manifestFile.isFile) manifestFile.readLines().associate {
            it.substringBefore("=") to it.substringAfter("=", "")
        } else emptyMap()
        val manifestVersion = manifest["version"]
        if (manifestVersion != null && manifestVersion != version.getVersionName()) {
            error("This backup belongs to Minecraft version $manifestVersion, not ${version.getVersionName()}.")
        }
        val optionsExisted = manifest["optionsExisted"]?.toBooleanStrictOrNull()
        val configExisted = manifest["configExisted"]?.toBooleanStrictOrNull()

        val optionsFile = File(gameDirectory, "options.txt")
        val configFile = File(version.getZalithVersionPath(), "version.config")
        val safetyId = "void-before-restore-${System.currentTimeMillis()}"
        if (optionsFile.isFile) {
            optionsFile.copyTo(File(backupDirectory, "$safetyId-options.txt"), overwrite = false)
        }
        if (configFile.isFile) {
            configFile.copyTo(File(backupDirectory, "$safetyId-version.config"), overwrite = false)
        }

        val restored = mutableListOf<String>()
        when {
            optionsBackup.isFile -> {
                optionsBackup.copyTo(optionsFile, overwrite = true)
                restored.add("options.txt")
            }
            optionsExisted == false && optionsFile.exists() -> {
                if (!optionsFile.delete()) error("Could not restore the original missing options.txt state.")
                restored.add("options.txt (removed because it did not exist at backup time)")
            }
        }
        when {
            configBackup.isFile -> {
                configBackup.copyTo(configFile, overwrite = true)
                val restoredConfig = com.movtery.zalithlauncher.game.version.installed.VersionConfig
                    .parseConfig(version.getVersionPath())
                version.getVersionConfig().ramAllocation = restoredConfig.ramAllocation
                restored.add("version.config")
            }
            configExisted == false && configFile.exists() -> {
                if (!configFile.delete()) error("Could not restore the original missing version.config state.")
                version.getVersionConfig().ramAllocation = -1
                restored.add("version.config (removed because it did not exist at backup time)")
            }
        }

        RestoreResult(
            minecraftVersion = version.getVersionName(),
            restoredFiles = restored,
            safetyBackupDirectory = backupDirectory
        )
    }
}
