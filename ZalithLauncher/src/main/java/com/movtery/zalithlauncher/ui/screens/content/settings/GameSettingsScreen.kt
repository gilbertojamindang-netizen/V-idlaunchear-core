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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.plugin.natives.NativePlugin
import com.movtery.zalithlauncher.game.plugin.natives.NativePluginManager
import com.movtery.zalithlauncher.path.URL_CLOUD_NATIVE_LIB_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_NATIVE_LIB_PLUGINS
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.setting.unit.min
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.MemoryPreview
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.StringListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.TextInputSettingsCard
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.sendDLPlugin
import com.movtery.zalithlauncher.utils.device.VoidPerformanceAdvisor
import com.movtery.zalithlauncher.utils.device.VoidPerformanceApplier
import com.movtery.zalithlauncher.utils.device.VoidSmartLagAnalyzer
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun GameSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Game, settingsScreenKey, false)
    ) { isVisible ->
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.versionIsolation,
                        title = stringResource(R.string.settings_game_version_isolation_title),
                        summary = stringResource(R.string.settings_game_version_isolation_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.skipGameIntegrityCheck,
                        title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
                        summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
                    )

                    TextInputSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.versionCustomInfo,
                        title = stringResource(R.string.settings_game_version_custom_info_title),
                        summary = stringResource(R.string.settings_game_version_custom_info_summary)
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    val runtimes = remember { RuntimesManager.getRuntimes() }

                    if (runtimes.isNotEmpty()) {
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = AllSettings.javaRuntime,
                            items = RuntimesManager.getRuntimes().filter { it.isCompatible() },
                            title = stringResource(R.string.settings_game_java_runtime_title),
                            summary = stringResource(R.string.settings_game_java_runtime_summary),
                            getItemText = { it.name },
                            getItemId = { it.name }
                        )
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = if (runtimes.isNotEmpty()) CardPosition.Middle else CardPosition.Top,
                        unit = AllSettings.autoPickJavaRuntime,
                        title = stringResource(R.string.settings_game_auto_pick_java_runtime_title),
                        summary = stringResource(R.string.settings_game_auto_pick_java_runtime_summary)
                    )

                    val nativePlugins = remember {
                        NativePluginManager.getPlugins()
                    }

                    @Composable
                    fun DLNativeLibsButton() {
                        IconButton(
                            onClick = {
                                eventViewModel.sendDLPlugin(
                                    githubLink = URL_GITHUB_NATIVE_LIB_PLUGINS,
                                    cloudDrives = listOf(
                                        EventViewModel.Event.DownloadPlugins.CloudDrive(
                                            language = "zh",
                                            link = URL_CLOUD_NATIVE_LIB_PLUGINS
                                        )
                                    )
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download_2_filled),
                                contentDescription = stringResource(R.string.generic_download)
                            )
                        }
                    }
                    if (nativePlugins.isNotEmpty()) {
                        StringListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.disableNativeLibPlugins,
                            items = nativePlugins,
                            onItemsChange = { value, item ->
                                if (value) {
                                    this - item.packageName
                                } else {
                                    this + item.packageName
                                }
                            },
                            title = stringResource(R.string.settings_game_native_lib_plugin_title),
                            summary = stringResource(R.string.settings_game_native_lib_plugin_summary),
                            getItemID = { it.packageName },
                            getItemText = { it.displayName },
                            getItemSummary = { plugin ->
                                NativePluginSummaryLayout(plugin)
                            },
                            getItemCheck = { contains -> !contains },
                            trailingIcon = {
                                DLNativeLibsButton()
                            },
                        )
                    } else {
                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            title = stringResource(R.string.settings_game_native_lib_plugin_title),
                            summary = stringResource(R.string.settings_game_native_lib_plugin_summary),
                            onClick = {},
                            trailingIcon = {
                                DLNativeLibsButton()
                            }
                        )
                    }

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.ramAllocation,
                        title = stringResource(R.string.settings_game_java_memory_title),
                        summary = stringResource(R.string.settings_game_java_memory_summary),
                        valueRange = AllSettings.ramAllocation.floatRange.start..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                        suffix = "MB",
                        fineTuningControl = true,
                        previewContent = {
                            MemoryPreview(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 2.dp),
                                preview = (AllSettings.ramAllocation.state ?: AllSettings.ramAllocation.min).toDouble(),
                                usedText = { usedMemory, totalMemory ->
                                    stringResource(R.string.settings_game_java_memory_used_text, usedMemory.toInt(), totalMemory.toInt())
                                },
                                previewText = { preview ->
                                    stringResource(R.string.settings_game_java_memory_allocation_text, preview.toInt())
                                }
                            )
                        }
                    )

                    TextInputSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.jvmArgs,
                        title = stringResource(R.string.settings_game_jvm_args_title),
                        summary = stringResource(R.string.settings_game_jvm_args_summary)
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.showLogAutomatic,
                        title = stringResource(R.string.settings_game_show_log_automatic_title),
                        summary = stringResource(R.string.settings_game_show_log_automatic_summary)
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.logTextSize,
                        title = stringResource(R.string.settings_game_log_text_size_title),
                        summary = stringResource(R.string.settings_game_log_text_size_summary),
                        valueRange = AllSettings.logTextSize.floatRange,
                        suffix = "Sp",
                        fineTuningControl = true
                    )

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.logBufferFlushInterval,
                        title = stringResource(R.string.settings_game_log_buffer_flush_interval_title),
                        summary = stringResource(R.string.settings_game_log_buffer_flush_interval_summary),
                        valueRange = AllSettings.logBufferFlushInterval.floatRange,
                        suffix = "ms",
                        fineTuningControl = true
                    )
                }
            }

            // VØID Smart Lag Fix: read-only modpack and Android thermal diagnostics.
            AnimatedItem { ctx ->
                val selectedVersion = VersionsManager.currentVersion.value
                val modpackReport = remember(selectedVersion?.getVersionName()) {
                    selectedVersion?.let {
                        VoidSmartLagAnalyzer.inspectModpack(File(it.getGameDir(), "mods"))
                    }
                }
                val thermal = VoidSmartLagAnalyzer.thermalStatus(context)
                val deviceRecommendation = remember {
                    VoidPerformanceAdvisor.recommend(context)
                }
                val coroutineScope = rememberCoroutineScope()
                val showApplyConfirmation = remember { mutableStateOf(false) }
                val isApplyingRecommendation = remember { mutableStateOf(false) }
                val applyResultMessage = remember { mutableStateOf<String?>(null) }
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    Text(
                        text = "VØID SMART LAG FIX",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Device profile: ${deviceRecommendation.profile} • RAM suggestion: ${deviceRecommendation.maxMemoryMb} MB • Render distance: ${deviceRecommendation.renderDistance}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Modpack scan: ${modpackReport?.load ?: "No Minecraft version selected"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    modpackReport?.let { report ->
                        Text(
                            text = "${report.enabledMods} enabled JARs • ${report.totalBytes / (1024L * 1024L)} MB total • Largest JAR: ${report.largestModBytes / (1024L * 1024L)} MB",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(text = report.recommendation, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "Thermal status: $thermal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = VoidSmartLagAnalyzer.thermalAdvice(thermal),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = deviceRecommendation.explanation,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Live FPS and FPS-drop warnings are shown in the in-game VØID status widget while Minecraft is running.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showApplyConfirmation.value = true },
                        enabled = selectedVersion != null && !isApplyingRecommendation.value
                    ) {
                        Text(
                            if (isApplyingRecommendation.value) "Applying safe profile..."
                            else "Apply recommended settings"
                        )
                    }
                    applyResultMessage.value?.let { message ->
                        Text(text = message, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "Applying changes creates a timestamped backup of options.txt and version.config first.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (showApplyConfirmation.value) {
                        AlertDialog(
                            onDismissRequest = {
                                if (!isApplyingRecommendation.value) {
                                    showApplyConfirmation.value = false
                                }
                            },
                            title = { Text("Apply VØID performance profile?") },
                            text = {
                                Text(
                                    "Close Minecraft before continuing. VØID will back up the selected installation's settings, then apply the recommended RAM, render distance, simulation distance, and entity distance. You can restore from the saved backup if needed."
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    enabled = !isApplyingRecommendation.value,
                                    onClick = {
                                        showApplyConfirmation.value = false
                                        isApplyingRecommendation.value = true
                                        applyResultMessage.value = null
                                        coroutineScope.launch {
                                            val result = runCatching {
                                                VoidPerformanceApplier.apply(deviceRecommendation)
                                            }
                                            applyResultMessage.value = result.fold(
                                                onSuccess = {
                                                    "Applied to Minecraft ${it.minecraftVersion}. Backup ID: ${it.backupId}"
                                                },
                                                onFailure = {
                                                    "Could not apply settings: ${it.message ?: "unknown error"}"
                                                }
                                            )
                                            isApplyingRecommendation.value = false
                                        }
                                    }
                                ) {
                                    Text("Apply")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    enabled = !isApplyingRecommendation.value,
                                    onClick = { showApplyConfirmation.value = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    Text(
                        text = "Recommendations are estimates, not guaranteed FPS improvements. Reduce settings or stop playing if the device becomes too hot.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


@Composable
private fun NativePluginSummaryLayout(plugin: NativePlugin) {
    FlowRow(
        modifier = Modifier.alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_renderer_from_plugins,plugin.appName),
            style = MaterialTheme.typography.labelSmall
        )

        val minVer = plugin.minMCVer
        val maxVer = plugin.maxMCVer

        if (minVer != null || maxVer != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                minVer?.let {
                    Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                }

                maxVer?.let {
                    Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}