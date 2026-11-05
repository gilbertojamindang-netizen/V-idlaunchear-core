/*
 * VøidLaunchear companion stage.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * This file is distributed under GPL-3.0-or-later, consistent with the
 * upstream Zalith Launcher 2 project.
 */
package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.petlib.ArmModel
import com.movtery.petlib.PetTapReaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * A small skin-based companion that wanders inside its stage and can be picked up.
 *
 * Dragging is constrained to the stage. Releasing the companion produces a
 * lightweight Compose spring/drop response; this is visual feedback, not a
 * physics-based skeletal ragdoll simulation.
 */
@Composable
fun VoidCompanionStage(
    modifier: Modifier = Modifier,
    skin: ByteArray? = null,
    cape: ByteArray? = null,
    arm: ArmModel? = null,
    yaw: Float = 0f
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF160D20)),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val modelWidth = 132.dp
        val modelHeight = 190.dp
        val maxX = with(density) { ((maxWidth - modelWidth) / 2).coerceAtLeast(0.dp).toPx() }
        val maxY = with(density) { ((maxHeight - modelHeight) / 2).coerceAtLeast(0.dp).toPx() }

        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }
        val rotation = remember { Animatable(0f) }
        val squash = remember { Animatable(1f) }
        var dragging by remember { mutableStateOf(false) }
        var dropping by remember { mutableStateOf(false) }

        LaunchedEffect(maxX, maxY, dragging, dropping) {
            if (!dragging && !dropping) {
                while (true) {
                    delay(Random.nextLong(900L, 2100L))
                    val targetX = if (maxX <= 0f) 0f else Random.nextFloat() * maxX * 2f - maxX
                    val targetY = if (maxY <= 0f) 0f else (Random.nextFloat() * 1.5f - 0.45f) * maxY
                    val duration = Random.nextInt(850, 1650)
                    launch { offsetX.animateTo(targetX, tween(duration)) }
                    launch { offsetY.animateTo(targetY.coerceIn(-maxY, maxY), tween(duration)) }
                    launch {
                        rotation.animateTo(
                            if (targetX >= offsetX.value) 5f else -5f,
                            tween(duration)
                        )
                    }
                    delay(duration.toLong())
                    rotation.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                }
            }
        }

        Box(
            modifier = Modifier
                .size(modelWidth, modelHeight)
                .offset {
                    IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt())
                }
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = squash.value
                    scaleY = 2f - squash.value
                }
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            launch {
                                offsetX.stop()
                                offsetY.stop()
                                rotation.stop()
                                squash.stop()
                            }
                        },
                        onDragEnd = {
                            dropping = true
                            dragging = false
                            launch {
                                val lean = if (offsetX.value >= 0f) 18f else -18f
                                offsetY.animateTo(maxY, tween(220))
                                launch { rotation.animateTo(lean, tween(130)) }
                                launch { squash.animateTo(0.84f, tween(100)) }
                                offsetY.animateTo(
                                    (maxY - 13f).coerceAtLeast(-maxY),
                                    spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMedium)
                                )
                                launch {
                                    rotation.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)
                                    )
                                }
                                squash.animateTo(
                                    1f,
                                    spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMedium)
                                )
                                dropping = false
                            }
                        },
                        onDragCancel = {
                            dropping = true
                            dragging = false
                            launch {
                                offsetY.animateTo(maxY, tween(220))
                                rotation.animateTo(0f, spring())
                                squash.animateTo(1f, spring())
                                dropping = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(-maxX, maxX))
                            offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-maxY, maxY))
                            rotation.snapTo((dragAmount.x / 9f).coerceIn(-18f, 18f))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            PetModelView(
                modifier = Modifier.fillMaxSize(),
                skin = skin,
                cape = cape,
                arm = arm,
                yaw = yaw,
                tapReaction = PetTapReaction.Interact,
                dragYaw = false,
                autonomousIdle = true
            )
        }
    }
}
