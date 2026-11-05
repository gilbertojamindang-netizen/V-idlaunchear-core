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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.movtery.petlib.PetAnimation
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
        val dragScope = rememberCoroutineScope()
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
        var ragdollTrigger by remember { mutableIntStateOf(0) }
        var sitting by remember { mutableStateOf(false) }
        var dragX by remember { mutableStateOf(0f) }
        var dragY by remember { mutableStateOf(0f) }

        LaunchedEffect(maxX, maxY, dragging, dropping, sitting) {
            if (!dragging && !dropping && !sitting) {
                while (true) {
                    delay(Random.nextLong(900L, 2100L))
                    val targetX = if (maxX <= 0f) 0f else Random.nextFloat() * maxX * 2f - maxX
                    val targetY = if (maxY <= 0f) 0f else (Random.nextFloat() * 1.5f - 0.45f) * maxY
                    val duration = Random.nextInt(850, 1650)
                    dragScope.launch { offsetX.animateTo(targetX, tween(duration)) }
                    dragScope.launch { offsetY.animateTo(targetY.coerceIn(-maxY, maxY), tween(duration)) }
                    dragScope.launch {
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
                            sitting = false
                            dragging = true
                            dragX = offsetX.value
                            dragY = offsetY.value
                            dragScope.launch {
                                offsetX.stop()
                                offsetY.stop()
                                rotation.stop()
                                squash.stop()
                            }
                        },
                        onDragEnd = {
                            val releasedWhileAirborne = dragY < maxY - with(density) { 24.dp.toPx() }
                            if (releasedWhileAirborne) ragdollTrigger++
                            dropping = true
                            dragging = false
                            dragScope.launch {
                                val direction = if (offsetX.value >= 0f) 1f else -1f
                                val lean = direction * if (releasedWhileAirborne) 64f else 18f
                                offsetY.animateTo(maxY, tween(220))
                                dragY = maxY
                                dragScope.launch { rotation.animateTo(lean, tween(130)) }
                                dragScope.launch { squash.animateTo(if (releasedWhileAirborne) 0.74f else 0.84f, tween(100)) }
                                offsetY.animateTo(
                                    (maxY - 13f).coerceAtLeast(-maxY),
                                    spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMedium)
                                )
                                dragScope.launch {
                                    rotation.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)
                                    )
                                }
                                squash.animateTo(
                                    1f,
                                    spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMedium)
                                )
                                dragY = offsetY.value
                                dragX = offsetX.value
                                if (releasedWhileAirborne) delay(900L)
                                dropping = false
                            }
                        },
                        onDragCancel = {
                            dropping = true
                            dragging = false
                            dragScope.launch {
                                offsetY.animateTo(maxY, tween(220))
                                dragY = maxY
                                rotation.animateTo(0f, spring())
                                squash.animateTo(1f, spring())
                                dragX = offsetX.value
                                dragY = offsetY.value
                                dropping = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX = (dragX + dragAmount.x).coerceIn(-maxX, maxX)
                            dragY = (dragY + dragAmount.y).coerceIn(-maxY, maxY)
                            val nextX = dragX
                            val nextY = dragY
                            val nextRotation = (dragAmount.x / 9f).coerceIn(-18f, 18f)
                            dragScope.launch {
                                offsetX.snapTo(nextX)
                                offsetY.snapTo(nextY)
                                rotation.snapTo(nextRotation)
                            }
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
                animation = when {
                    dragging || dropping -> null
                    sitting -> PetAnimation.Sitting
                    else -> PetAnimation.Idle
                },
                autonomousIdle = !dragging && !dropping && !sitting,
                ragdollTrigger = ragdollTrigger
            )
        }
        Button(
            onClick = { sitting = !sitting },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp)
        ) {
            Text(if (sitting) "STAND UP" else "SIT")
        }
    }
}
