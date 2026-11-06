/*
 * VøidLaunchear companion stage.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * This file is distributed under GPL-3.0-or-later, consistent with the
 * upstream Zalith Launcher 2 project.
 */
package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import kotlin.math.PI
import kotlin.random.Random

/**
 * Home-screen Minecraft companion with a lightweight roaming loop.
 *
 * It alternates between sitting, idle pauses and short walking steps. The
 * movement is deliberately kept inside a small stage-safe range to avoid
 * covering launcher controls or clipping the model on narrow screens.
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
    var walking by remember { mutableStateOf(false) }
    var walkingRight by remember { mutableStateOf(true) }
    var targetOffset by remember { mutableStateOf(0.dp) }
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 1700),
        label = "voidCompanionRoaming"
    )

    LaunchedEffect(autoPose) {
        if (autoPose) {
            sitting = true
            walking = false
            targetOffset = 0.dp
            while (true) {
                delay(Random.nextLong(3500L, 6000L))
                sitting = false
                walking = false
                delay(Random.nextLong(900L, 1800L))

                walkingRight = !walkingRight
                walking = true
                targetOffset = if (walkingRight) 12.dp else (-12).dp
                delay(1900L)

                walking = false
                delay(Random.nextLong(1200L, 2400L))
                if (Random.nextInt(4) == 0) {
                    sitting = true
                    delay(Random.nextLong(2200L, 3800L))
                    sitting = false
                }
            }
        } else {
            sitting = true
            walking = false
            targetOffset = 0.dp
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        PetModelView(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedOffset),
            skin = skin,
            cape = cape,
            arm = arm,
            yaw = yaw + if (walkingRight) 0f else PI.toFloat(),
            animation = when {
                sitting -> PetAnimation.Sitting
                walking -> PetAnimation.Walking
                else -> PetAnimation.Idle
            },
            tapReaction = PetTapReaction.Interact,
            dragYaw = false,
            gazeEnabled = true,
            autonomousIdle = false,
            ragdollTrigger = 0
        )
    }
}
