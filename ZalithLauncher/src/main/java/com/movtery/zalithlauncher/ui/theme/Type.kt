/*
 * VøidLaunchear typography.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * This file is distributed under the same GPL-3.0-or-later license as the
 * upstream Zalith Launcher 2 project.
 */
package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.movtery.zalithlauncher.setting.enums.LauncherFontStyle

fun createAppTypography(fontStyle: LauncherFontStyle): Typography {
    val base = Typography()
    val family = when (fontStyle) {
        LauncherFontStyle.SYSTEM -> return base
        LauncherFontStyle.SANS_SERIF -> FontFamily.SansSerif
        LauncherFontStyle.SERIF -> FontFamily.Serif
        LauncherFontStyle.MONOSPACE -> FontFamily.Monospace
        LauncherFontStyle.CURSIVE -> FontFamily.Cursive
    }

    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
}
