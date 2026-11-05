/*
 * VØID CORE command router tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later.
 */
package com.movtery.zalithlauncher.utils.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoidCoreCommandRouterTest {
    @Test
    fun recognizesCommandsWithSlashAndExtraWhitespace() {
        val result = VoidCoreCommandRouter.route("  /CHECK   GPU ")
        assertEquals(VoidCoreCommandRouter.Action.OPEN_GPU_DIAGNOSTICS, result.action)
    }

    @Test
    fun destructiveOrRestorativeCommandsRequireConfirmation() {
        assertTrue(VoidCoreCommandRouter.route("/clean cache").requiresConfirmation)
        assertTrue(VoidCoreCommandRouter.route("/restore settings").requiresConfirmation)
    }

    @Test
    fun adviceCommandsDoNotClaimToApplyChanges() {
        val result = VoidCoreCommandRouter.route("/optimize")
        assertFalse(result.requiresConfirmation)
        assertTrue(result.message.contains("Nothing is changed automatically"))
    }

    @Test
    fun deviceCommandReturnsBasicDeviceAndAndroidSummary() {
        val result = VoidCoreCommandRouter.route("/device")
        assertEquals(VoidCoreCommandRouter.Action.SHOW_DEVICE_SUMMARY, result.action)
        assertTrue(result.message.contains("Android"))
        assertTrue(result.message.contains("API"))
    }

    @Test
    fun unknownCommandReturnsHelp() {
        assertEquals(
            VoidCoreCommandRouter.Action.HELP,
            VoidCoreCommandRouter.route("/do-everything").action
        )
    }
}
