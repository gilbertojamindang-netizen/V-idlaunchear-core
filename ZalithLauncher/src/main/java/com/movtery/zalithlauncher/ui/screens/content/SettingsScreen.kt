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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedDirection
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.content.settings.AboutInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.GameSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.GamepadSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.JavaManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.LauncherSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSettingsScreen
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun SettingsScreen(
    key: NestedNavKey.Settings,
    backStackViewModel: ScreenBackStackViewModel,
    openLicenseScreen: (raw: Int) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                modifier = Modifier.fillMaxHeight(),
                isVisible = isVisible,
                settingsScreenKey = backStackViewModel.settingsScreen.currentKey,
                navigateTo = { settingKey ->
                    key.backStack.navigateOnce(settingKey)
                }
            )
            NavigationUI(
                key = key,
                mainScreenKey = backStackViewModel.mainScreen.currentKey,
                settingsScreenKey = backStackViewModel.settingsScreen.currentKey,
                onCurrentKeyChange = { newKey ->
                    backStackViewModel.settingsScreen.currentKey = newKey
                },
                openLicenseScreen = openLicenseScreen,
                eventViewModel = eventViewModel,
                submitError = submitError,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val settingItems = listOf(
    CategoryItem(NormalNavKey.Settings.Renderer, { CategoryIcon(R.drawable.ic_video_settings, R.string.settings_tab_renderer) }, R.string.settings_tab_renderer),
    CategoryItem(NormalNavKey.Settings.Game, { CategoryIcon(R.drawable.ic_rocket_launch_filled, R.string.settings_tab_game) }, R.string.settings_tab_game),
    CategoryItem(NormalNavKey.Settings.Control, { CategoryIcon(R.drawable.ic_videogame_asset_outlined, R.string.settings_tab_control) }, R.string.settings_tab_control),
    CategoryItem(NormalNavKey.Settings.Gamepad, { CategoryIcon(R.drawable.ic_sports_esports_outlined, R.string.settings_tab_gamepad) }, R.string.settings_tab_gamepad),
    CategoryItem(NormalNavKey.Settings.Launcher, { CategoryIcon(R.drawable.ic_setting_launcher, R.string.settings_tab_launcher) }, R.string.settings_tab_launcher),
    CategoryItem(NormalNavKey.Settings.JavaManager, { CategoryIcon(R.drawable.ic_java, R.string.settings_tab_java_manage) }, R.string.settings_tab_java_manage, division = true),
    CategoryItem(NormalNavKey.Settings.ControlManager, { CategoryIcon(R.drawable.ic_videogame_asset_outlined, R.string.settings_tab_control_manage) }, R.string.settings_tab_control_manage),
    CategoryItem(NormalNavKey.Settings.AboutInfo, { CategoryIcon(R.drawable.ic_info_outlined, R.string.settings_tab_info_about) }, R.string.settings_tab_info_about, division = true)
)

@Composable
private fun TabMenu(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    settingsScreenKey: TitledNavKey?,
    navigateTo: (TitledNavKey) -> Unit
) {
    val ctx = rememberAnimatedContext(isVisible = isVisible)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .width(148.dp)
            .background(Color(0xFF100D17))
            .fadeEdge(scrollState)
            .animated(ctx, from = AnimatedDirection.Left)
            .verticalScroll(scrollState)
            .padding(horizontal = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF38204F), Color(0xFF1B1425))
                    )
                )
                .padding(vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Ø",
                color = Color(0xFFF0D8FF),
                fontSize = 29.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "VØID CONTROL",
                color = Color(0xFFE8C8FF),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                maxLines = 1
            )
            Text(
                text = "SYSTEM SETTINGS",
                color = Color(0xFFB99ACD),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(13.dp))
        Text(
            text = "CONFIGURATION",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, bottom = 8.dp),
            color = Color(0xFFB99ACD),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        settingItems.forEach { item ->
            if (item.division) {
                Spacer(modifier = Modifier.height(5.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .alpha(0.35f),
                    color = Color(0xFFB99ACD)
                )
            }
            val selected = settingsScreenKey == item.key
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                onClick = { navigateTo(item.key) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) Color(0xFF653A91)
                    else Color(0xFF211B2B),
                contentColor = if (selected) Color.White
                    else Color(0xFFD4C9DE),
                tonalElevation = if (selected) 3.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) Color.White.copy(alpha = 0.16f)
                                else Color(0xFF352941)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        item.icon()
                    }
                    Text(
                        text = stringResource(item.textRes),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun NavigationUI(
    key: NestedNavKey.Settings,
    mainScreenKey: TitledNavKey?,
    settingsScreenKey: TitledNavKey?,
    onCurrentKeyChange: (TitledNavKey?) -> Unit,
    openLicenseScreen: (raw: Int) -> Unit,
    eventViewModel: EventViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = key,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.Settings.Renderer> {
                    RendererSettingsScreen(key, settingsScreenKey, mainScreenKey, eventViewModel)
                }
                entry<NormalNavKey.Settings.Game> {
                    GameSettingsScreen(key, settingsScreenKey, mainScreenKey, eventViewModel)
                }
                entry<NormalNavKey.Settings.Control> {
                    ControlSettingsScreen(key, settingsScreenKey, mainScreenKey, eventViewModel, submitError)
                }
                entry<NormalNavKey.Settings.Gamepad> {
                    GamepadSettingsScreen(key, settingsScreenKey, mainScreenKey, eventViewModel)
                }
                entry<NormalNavKey.Settings.Launcher> {
                    LauncherSettingsScreen(
                        key = key,
                        settingsScreenKey = settingsScreenKey,
                        mainScreenKey = mainScreenKey,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.Settings.JavaManager> {
                    JavaManageScreen(key, settingsScreenKey, mainScreenKey, eventViewModel, submitError)
                }
                entry<NormalNavKey.Settings.ControlManager> {
                    ControlManageScreen(key, settingsScreenKey, mainScreenKey, eventViewModel, submitError)
                }
                entry<NormalNavKey.Settings.AboutInfo> {
                    AboutInfoScreen(
                        key = key,
                        settingsScreenKey = settingsScreenKey,
                        mainScreenKey = mainScreenKey,
                        checkUpdate = {
                            eventViewModel.sendEvent(EventViewModel.Event.CheckUpdate)
                        },
                        onViewSponsors = {
                            eventViewModel.sendEvent(EventViewModel.Event.ShowSponsors)
                        },
                        openLicense = openLicenseScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        }
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}