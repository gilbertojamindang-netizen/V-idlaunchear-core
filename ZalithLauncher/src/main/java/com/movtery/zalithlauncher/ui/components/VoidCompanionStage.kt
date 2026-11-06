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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.movtery.petlib.ArmModel
import com.movtery.petlib.PetAnimation
import com.movtery.petlib.PetTapReaction
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
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
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var ragdollTrigger by remember { mutableStateOf(0) }
    var wasLifted by remember { mutableStateOf(false) }
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 1700),
        label = "voidCompanionRoaming"
    )

    LaunchedEffect(ragdollTrigger) {
        if (ragdollTrigger > 0) {
            // Keep the model at the release point while PetLib plays its fall animation.
            delay(900L)
            if (!isDragging) {
                dragX = 0f
                dragY = 0f
            }
        }
    }

    LaunchedEffect(autoPose) {
        if (autoPose) {
            sitting = true
            walking = false
            targetOffset = 0.dp
            while (true) {
                delay(Random.nextLong(3500L, 6000L))
                if (!isDragging) {
                    sitting = false
                    walking = false
                    delay(Random.nextLong(900L, 1800L))
                    if (!isDragging) {
                        walkingRight = !walkingRight
                        walking = true
                        targetOffset = if (walkingRight) 12.dp else (-12).dp
                    }
                    delay(1900L)
                    walking = false
                    delay(Random.nextLong(1200L, 2400L))
                    if (!isDragging && Random.nextInt(4) == 0) {
                        sitting = true
                        delay(Random.nextLong(2200L, 3800L))
                        sitting = false
                    }
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
            .background(Color.Transparent)
            .pointerInput(autoPose) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        wasLifted = false
                        sitting = false
                        walking = false
                        targetOffset = 0.dp
                    },
                    onDragEnd = {
                        // Keep an airborne release at its drop point until the fall animation finishes.
                        if (wasLifted || dragY < -12f) {
                            ragdollTrigger++
                        } else {
                            dragX = 0f
                            dragY = 0f
                        }
                        wasLifted = false
                        isDragging = false
                    },
                    onDragCancel = {
                        // A gesture can be cancelled by navigation or pointer loss.
                        // Treat an already-lifted companion like a normal airborne release
                        // so it does not snap back without playing its fall animation.
                        if (wasLifted || dragY < -12f) {
                            ragdollTrigger++
                        } else {
                            dragX = 0f
                            dragY = 0f
                        }
                        wasLifted = false
                        isDragging = false
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragX = (dragX + amount.x).coerceIn(-96f, 96f)
                        dragY = (dragY + amount.y).coerceIn(-120f, 48f)
                        if (dragY < -12f) wasLifted = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        PetModelView(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        (dragX + if (isDragging) 0f else with(this) { animatedOffset.toPx() }).roundToInt(),
                        dragY.roundToInt()
                    )
                },
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
            ragdollTrigger = ragdollTrigger
        )
    }
}
