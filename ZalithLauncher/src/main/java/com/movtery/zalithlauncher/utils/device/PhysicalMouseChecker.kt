/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.utils.device

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import com.movtery.zalithlauncher.utils.logging.Logger

private const val TAG = "PhysicalMouseChecker"

object PhysicalMouseChecker {
    /**
     * 当前是否有实体鼠标连接
     */
    @Volatile
    var physicalMouseConnected = false
        private set

    private var registeredInputManager: InputManager? = null
    private var registeredListener: InputManager.InputDeviceListener? = null

    @Synchronized
    fun initChecker(activity: Activity) {
        val inputManager = activity.getSystemService(Context.INPUT_SERVICE) as InputManager

        // Avoid retaining an old Activity's listener after recreation.
        registeredListener?.let { oldListener ->
            registeredInputManager?.unregisterInputDeviceListener(oldListener)
        }

        fun refreshConnectionState() {
            physicalMouseConnected = isPhysicalMouseConnected()
            Logger.info(TAG, "Physical mouse connection status: $physicalMouseConnected")
        }

        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                refreshConnectionState()
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                refreshConnectionState()
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                refreshConnectionState()
            }
        }

        registeredInputManager = inputManager
        registeredListener = listener
        refreshConnectionState()
        inputManager.registerInputDeviceListener(listener, null)
        Logger.info(TAG, "Input device listener registered.")
    }
}

/**
 * 确认这个deviceId是否为实体鼠标
 */
private fun Int.isMouseId(): Boolean {
    return InputDevice.getDevice(this)?.let { device ->
        device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
    } ?: false
}

/**
 * 粗略检查是否接入实体鼠标
 */
private fun isPhysicalMouseConnected(): Boolean {
    return InputDevice.getDeviceIds()
        .takeIf { it.isNotEmpty() }
        ?.any { it.isMouseId() }
        ?: false
}