# Vøidlaunchear — Horizon Android Integration Assessment

Date: 2026-11-05

## Decision

Keep Zalith Launcher 2 as the game-launch foundation for the Minecraft Java 1.21.11 target. Treat Horizon Android as a separate reference implementation. Do not merge its Rust game engine or replace the Zalith/Pojav launch path.

## Verified Horizon Android facts

Reviewed repository: https://github.com/Vasysik/horizon-android (branch: android)

- Horizon Android is a native Rust/wgpu client for Minecraft Java 26.3.
- Its Android documentation explicitly says it does not run arbitrary Minecraft JAR versions.
- It uses Android NativeActivity and a Rust client; the documentation says there are no project Java/Kotlin classes.
- It offers four touch schemes (joystick or D-pad, paired with tap or crosshair aiming), editable touch layouts, orientation choices, keyboard/mouse input, screenshots, resource packs, and native .hmod packages.
- Rendering options include Vulkan GPU batching, Vulkan compatibility, and experimental OpenGL ES.
- It documents verified resource downloads, retryable cache entries, app-private profile/world data, and Android Keystore encryption for refresh tokens.
- The Android build requires a Rust/Android toolchain, Android SDK platform 35, NDK, JDK and Gradle 8.14, plus generated engine tables prepared by its own scripts.
- Horizon's Android port is actively developed; device/driver compatibility still needs testing.

## What can be adapted as product/design requirements

These are feature references, not code already integrated:

1. Landscape, portrait, reversed and sensor-based orientation settings.
2. Configurable touch layouts: joystick/D-pad, tap/crosshair aiming, button position/size/opacity and handedness.
3. Vulkan/GLES renderer selection only where supported by the Zalith/Pojav backend and device driver.
4. Download progress, checksum verification, retry/resume and verified cache behavior for launcher-managed files.
5. Separate game profiles and clear per-profile storage management.
6. Safe account-token storage using Android Keystore, subject to the authentication provider's requirements.
7. Keyboard/mouse input and controller support, subject to Zalith's input APIs.

## What must not be copied directly

- Horizon's Rust/wgpu game engine and its Minecraft 26.3 game path are not a drop-in replacement for Zalith's Java 1.21.11 runtime.
- Horizon's native .hmod mods are not Fabric/Forge/NeoForge Java mods. Do not present them as interchangeable.
- Horizon's launcher UI is part of a Rust NativeActivity application; copying its screens or build scripts into a conventional Android/Kotlin/Compose Zalith project will not make them work.
- Do not copy game assets, prepared resource tables, APKs, credentials, signing keys or private user data from either project.

## Proposed integration sequence

1. Preserve the current Zalith-based branch and confirm its exact upstream revision, license and build setup.
2. Audit Zalith's existing APIs for orientation, touch controls, renderer selection, profiles, downloads and account storage.
3. Implement only compatible features through Zalith's existing Android/native interfaces.
4. Keep Horizon-specific Rust engine code isolated; do not add it to the 1.21.11 launch path.
5. Build and test on a real Android device before marking any feature complete.

## Status

This document records source inspection and an integration decision only. It does not claim that Horizon code has been ported, that an APK has been built, or that gameplay has been tested.

## Source references

- Horizon Android README: https://github.com/Vasysik/horizon-android/blob/android/README.md
- Android build, launcher, controls and rendering documentation: https://github.com/Vasysik/horizon-android/blob/android/docs/android.md
- Horizon Android feature support: https://github.com/Vasysik/horizon-android/blob/android/docs/android-features.md
- Horizon Android license and third-party notices: https://github.com/Vasysik/horizon-android/blob/android/LICENSE
