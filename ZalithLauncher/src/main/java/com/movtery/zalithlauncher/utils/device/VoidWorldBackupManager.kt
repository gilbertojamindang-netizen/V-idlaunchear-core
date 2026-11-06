/*
 * VøidLaunchear world backup manager.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.utils.device

import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local world backups for the currently selected Minecraft installation.
 *
 * Restore always creates a fresh safety archive before replacing an existing
 * world. ZIP paths are validated before extraction to prevent path traversal.
 */
object VoidWorldBackupManager {
    private const val META_ENTRY = ".void-world-backup.properties"
    private const val MAX_RESTORE_ENTRIES = 250_000
    private const val MAX_METADATA_BYTES = 16 * 1024
    private const val MAX_RESTORE_UNCOMPRESSED_BYTES = 16L * 1024L * 1024L * 1024L

    data class WorldInfo(
        val name: String,
        val sizeBytes: Long,
        val lastModified: Long
    )

    data class BackupInfo(
        val id: String,
        val worldName: String,
        val createdAt: Long,
        val sizeBytes: Long
    )

    data class RestoreResult(
        val worldName: String,
        val safetyBackupId: String?
    )

    suspend fun listWorlds(): List<WorldInfo> = withContext(Dispatchers.IO) {
        val saves = savesDirectory()
        saves.listFiles()
            ?.filter { it.isDirectory && isSafeWorldName(it.name) && File(it, "level.dat").isFile }
            ?.map { directory ->
                WorldInfo(
                    name = directory.name,
                    sizeBytes = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() },
                    lastModified = directory.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            .orEmpty()
    }

    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val directory = backupDirectory()
        directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("zip", ignoreCase = true) }
            ?.mapNotNull { file ->
                runCatching { readBackupInfo(file) }.getOrNull()
            }
            ?.sortedByDescending { it.createdAt }
            .orEmpty()
    }

    suspend fun createBackup(worldName: String): BackupInfo = withContext(Dispatchers.IO) {
        val saves = savesDirectory()
        val world = safeChild(saves, worldName)
        require(world.isDirectory && File(world, "level.dat").isFile) {
            "The selected world folder is missing or is not a valid Minecraft save."
        }
        createBackupInternal(world)
    }

    suspend fun restoreBackup(backupId: String): RestoreResult = withContext(Dispatchers.IO) {
        require(isSafeBackupId(backupId)) {
            "Invalid VØID world backup identifier."
        }

        val backupDir = backupDirectory()
        val archive = File(backupDir, backupId).canonicalFile
        require(archive.parentFile == backupDir.canonicalFile && archive.isFile) {
            "That VØID backup could not be found."
        }

        val metadata = readBackupInfo(archive)
        require(isSafeWorldName(metadata.worldName)) {
            "The backup contains an invalid world name."
        }

        val saves = savesDirectory()
        val destination = safeChild(saves, metadata.worldName)
        require(!destination.exists() || destination.isDirectory) {
            "The target world path exists but is not a folder. Nothing was changed."
        }
        val safetyBackup = if (destination.isDirectory) {
            createBackupInternal(destination)
        } else {
            null
        }

        val timestamp = System.currentTimeMillis()
        val temporary = File(saves, ".void-restore-$timestamp")
        val replaced = File(saves, ".void-replaced-$timestamp")
        deleteTree(temporary)
        deleteTree(replaced)
        if (!temporary.mkdirs()) error("Unable to prepare a temporary restore folder.")

        try {
            extractWorld(archive, temporary)
            require(File(temporary, "level.dat").isFile) {
                "The backup is incomplete: level.dat is missing."
            }

            if (destination.exists() && !destination.renameTo(replaced)) {
                error("Unable to move the current world aside. Nothing was replaced.")
            }

            if (!temporary.renameTo(destination)) {
                if (replaced.exists()) replaced.renameTo(destination)
                error("Unable to install the restored world. The previous world was kept.")
            }

            // The safety archive was written before replacement. Remove the
            // temporary old directory only after the restored folder is in place.
            deleteTree(replaced)
            RestoreResult(metadata.worldName, safetyBackup?.id)
        } catch (error: Throwable) {
            deleteTree(temporary)
            if (!destination.exists() && replaced.exists()) {
                replaced.renameTo(destination)
            }
            throw error
        }
    }

    private fun savesDirectory(): File {
        val version = VersionsManager.currentVersion.value
            ?: error("Select an installed Minecraft version first.")
        if (!version.isValid()) error("The selected Minecraft installation is not valid.")
        val gameDir = version.getGameDir().canonicalFile
        val saves = File(gameDir, "saves")
        if (!saves.exists() && !saves.mkdirs()) {
            error("Unable to open the selected installation's saves folder.")
        }
        if (!saves.isDirectory) error("The saves path is not a folder.")
        val canonicalSaves = saves.canonicalFile
        require(canonicalSaves.parentFile == gameDir) {
            "The saves folder resolves outside the selected Minecraft directory."
        }
        return canonicalSaves
    }

    private fun backupDirectory(): File {
        val version = VersionsManager.currentVersion.value
            ?: error("Select an installed Minecraft version first.")
        if (!version.isValid()) error("The selected Minecraft installation is not valid.")
        val gameDir = version.getGameDir().canonicalFile
        val directory = File(gameDir, ".void-world-backups")
        if (!directory.exists() && !directory.mkdirs()) {
            error("Unable to create the VØID world backup folder.")
        }
        if (!directory.isDirectory) error("The VØID backup path is not a folder.")
        val canonicalDirectory = directory.canonicalFile
        require(canonicalDirectory.parentFile == gameDir) {
            "The VØID backup folder resolves outside the selected Minecraft directory."
        }
        return canonicalDirectory
    }

    internal fun isSafeBackupId(id: String): Boolean {
        if (!id.matches(Regex("^void-world-[A-Za-z0-9_-]+-[0-9]+\\.zip$"))) return false
        val timestamp = id.substringAfterLast('-').removeSuffix(".zip").toLongOrNull()
        return timestamp != null && timestamp > 0L
    }

    internal fun isSafeWorldName(name: String): Boolean =
        name.isNotBlank() &&
            name != "." &&
            name != ".." &&
            '/' !in name &&
            '\\' !in name &&
            '\u0000' !in name

    internal fun isSafeArchiveEntryName(name: String): Boolean =
        name.isNotBlank() &&
            !name.startsWith('/') &&
            '\\' !in name &&
            '\u0000' !in name &&
            name.split('/').none { it == ".." || it == "." }

    private fun safeChild(parent: File, childName: String): File {
        require(isSafeWorldName(childName)) { "Invalid world folder name." }
        val root = parent.canonicalFile
        val child = File(root, childName).canonicalFile
        require(child.parentFile == root) { "The requested world path is outside the saves folder." }
        return child
    }

    private fun createBackupInternal(world: File): BackupInfo {
        val backupDir = backupDirectory()
        val timestamp = System.currentTimeMillis()
        val slug = world.name
            .map { character ->
                if (character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' || character == '_' || character == '-') {
                    character
                } else {
                    '_'
                }
            }
            .joinToString("")
            .trim('_')
            .ifBlank { "world" }
        val id = "void-world-$slug-$timestamp.zip"
        val output = File(backupDir, id)
        val temporary = File(backupDir, "$id.tmp")

        val properties = Properties().apply {
            setProperty("worldName", world.name)
            setProperty("createdAt", timestamp.toString())
        }
        val metadataBytes = ByteArrayOutputStream().use { buffer ->
            properties.store(buffer, "VØID world backup")
            buffer.toByteArray()
        }

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { zip ->
                zip.putNextEntry(ZipEntry(META_ENTRY))
                zip.write(metadataBytes)
                zip.closeEntry()

                val root = world.canonicalFile
                world.walkTopDown().forEach { file ->
                    if (!file.isFile) return@forEach
                    val canonical = file.canonicalFile
                    if (!canonical.path.startsWith(root.path + File.separator)) return@forEach
                    val relative = root.toPath().relativize(canonical.toPath())
                        .toString()
                        .replace(File.separatorChar, '/')
                    if (relative.isBlank() || relative == META_ENTRY) return@forEach
                    zip.putNextEntry(ZipEntry(relative))
                    BufferedInputStream(FileInputStream(canonical)).use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                }
            }

            try {
                Files.move(
                    temporary.toPath(),
                    output.toPath(),
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                if (!temporary.renameTo(output)) {
                    Files.move(temporary.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }

        return BackupInfo(id, world.name, timestamp, output.length())
    }

    private fun readBackupInfo(file: File): BackupInfo {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(META_ENTRY) ?: error("Backup metadata is missing.")
            require(entry.size in 0..MAX_METADATA_BYTES.toLong()) {
                "Backup metadata is larger than the allowed safety limit."
            }
            val metadataBytes = zip.getInputStream(entry).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_METADATA_BYTES) {
                        "Backup metadata is larger than the allowed safety limit."
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val properties = Properties().apply {
                ByteArrayInputStream(metadataBytes).use { load(it) }
            }
            val worldName = properties.getProperty("worldName")
                ?: error("Backup world name is missing.")
            require(isSafeWorldName(worldName)) {
                "Backup metadata contains an invalid world name."
            }
            val createdAt = properties.getProperty("createdAt")?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?: error("Backup timestamp is invalid.")
            return BackupInfo(file.name, worldName, createdAt, file.length())
        }
    }

    private fun extractWorld(archive: File, destination: File) {
        val root = destination.canonicalFile
        var extractedEntries = 0
        var extractedBytes = 0L
        val seenEntries = HashSet<String>()
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                try {
                    if (entry.name == META_ENTRY) continue
                    extractedEntries++
                    require(extractedEntries <= MAX_RESTORE_ENTRIES) {
                        "The backup contains too many files to restore safely."
                    }
                    require(isSafeArchiveEntryName(entry.name)) {
                        "The backup contains an unsafe file path."
                    }
                    require(seenEntries.add(entry.name)) {
                        "The backup contains duplicate file entries."
                    }

                    val target = File(root, entry.name).canonicalFile
                    require(target.path.startsWith(root.path + File.separator)) {
                        "The backup contains a file outside the world folder."
                    }
                    if (entry.isDirectory) {
                        if (!target.exists() && !target.mkdirs()) {
                            error("Unable to create a world folder during restore.")
                        }
                    } else {
                        val parent = target.parentFile
                            ?: error("Invalid file path in backup.")
                        if (!parent.exists() && !parent.mkdirs()) {
                            error("Unable to create a folder during restore.")
                        }
                        BufferedOutputStream(FileOutputStream(target)).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                extractedBytes += count
                                require(extractedBytes <= MAX_RESTORE_UNCOMPRESSED_BYTES) {
                                    "The backup expands beyond the VØID restore safety limit."
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                } finally {
                    zip.closeEntry()
                }
            }
        }
    }

    private fun deleteTree(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) file.listFiles()?.forEach(::deleteTree)
        file.delete()
    }
}
