/*
 * VøidLaunchear companion stage.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * This file is distributed under GPL-3.0-or-later, consistent with the
 * upstream Zalith Launcher 2 project.
 */
package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.movtery.petlib.ArmModel
import com.movtery.petlib.PetAnimation
import com.movtery.petlib.PetTapReaction
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Seated/standing home-screen companion.
 *
 * The companion stays in its stage, alternates between sitting and standing,
 * uses the native idle pose animation, follows the Activity mouse gaze router,
 * and reacts to taps. It deliberately has no wandering, dragging or ragdoll.
 */
@Composable
fun VoidCompanionStage(
    modifier: Modifier = Modifier,
    skin: ByteArray? = null,
    cape: ByteArray? = null,
    arm: ArmModel? = null,
    yaw: Float = 0f,
    autoPose: Boolean = true
) {
    var sitting by remember { mutableStateOf(true) }

    LaunchedEffect(autoPose) {
        if (autoPose) {
            sitting = true
            while (true) {
                delay(Random.nextLong(6500L, 9500L))
                sitting = false
                delay(Random.nextLong(5000L, 8000L))
                sitting = true
            }
        } else {
            sitting = true
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        PetModelView(
            modifier = Modifier.fillMaxSize(),
            skin = skin,
            cape = cape,
            arm = arm,
            yaw = yaw,
            animation = if (sitting) PetAnimation.Sitting else PetAnimation.Idle,
            tapReaction = PetTapReaction.Interact,
            dragYaw = false,
            gazeEnabled = true,
            autonomousIdle = false,
            ragdollTrigger = 0
        )
    }
}
