/*
 * VØID World Vault path validation tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later.
 */
package com.movtery.zalithlauncher.utils.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidWorldBackupManagerTest {
    @Test
    fun acceptsOnlyGeneratedBackupIdentifiers() {
        assertTrue(VoidWorldBackupManager.isSafeBackupId("void-world-Survival-1762380000000.zip"))
        assertTrue(VoidWorldBackupManager.isSafeBackupId("void-world-My_World-1762380000000.zip"))
        assertFalse(VoidWorldBackupManager.isSafeBackupId(""))
        assertFalse(VoidWorldBackupManager.isSafeBackupId("../void-world-Survival-1.zip"))
        assertFalse(VoidWorldBackupManager.isSafeBackupId("void-world-Survival.zip"))
        assertFalse(VoidWorldBackupManager.isSafeBackupId("void-world-Survival-0.zip"))
        assertFalse(VoidWorldBackupManager.isSafeBackupId("void-world-Survival-1.zip/../outside"))
    }

    @Test
    fun acceptsNormalWorldFolderNames() {
        assertTrue(VoidWorldBackupManager.isSafeWorldName("Survival"))
        assertTrue(VoidWorldBackupManager.isSafeWorldName("My World 2"))
        assertTrue(VoidWorldBackupManager.isSafeWorldName("world-2026_backup"))
    }

    @Test
    fun rejectsTraversalAndInvalidWorldFolderNames() {
        assertFalse(VoidWorldBackupManager.isSafeWorldName(""))
        assertFalse(VoidWorldBackupManager.isSafeWorldName("."))
        assertFalse(VoidWorldBackupManager.isSafeWorldName(".."))
        assertFalse(VoidWorldBackupManager.isSafeWorldName("../outside"))
        assertFalse(VoidWorldBackupManager.isSafeWorldName("folder/world"))
        assertFalse(VoidWorldBackupManager.isSafeWorldName("folder\\world"))
        assertFalse(VoidWorldBackupManager.isSafeWorldName("bad\u0000name"))
    }

    @Test
    fun acceptsNormalRelativeArchiveEntries() {
        assertTrue(VoidWorldBackupManager.isSafeArchiveEntryName("level.dat"))
        assertTrue(VoidWorldBackupManager.isSafeArchiveEntryName("region/r.0.0.mca"))
        assertTrue(VoidWorldBackupManager.isSafeArchiveEntryName("data/map_0.dat"))
    }

    @Test
    fun rejectsUnsafeArchiveEntryPaths() {
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName(""))
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName("/level.dat"))
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName("../outside.txt"))
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName("region/../../outside.txt"))
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName("region/./level.dat"))
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName("region\\outside.txt"))
        assertFalse(VoidWorldBackupManager.isSafeArchiveEntryName("region/\u0000bad.dat"))
    }
}
