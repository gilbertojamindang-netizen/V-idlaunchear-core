# VøidLaunchear final device test checklist

This checklist is for the next APK test pass. It deliberately separates implemented source paths from behavior that still requires a real Android device and Minecraft account.

## Already present — do not reimplement
- Custom launcher background image/video selection and persistence.
- Account management and Microsoft authentication flow.
- Existing-world Quick Play argument construction.
- Server Quick Play argument construction with address parsing and legacy argument fallback.
- Minecraft version manifest validation and cache fallback.
- Modrinth and CurseForge project/version search and pagination.
- Vulkan capability detection and safe renderer fallback.
- Low-end performance advisor and profile application.
- Companion stage with idle/walk/sit, small autonomous roaming, drag handling, and a PetLib ragdoll trigger.

## Device checks still required
1. Background: import a valid image, import a valid video, restart app, switch between them, and verify a failed/invalid import does not erase the previous background.
2. Login: complete Microsoft device-code login, restart app, confirm account persists, then test expired-token refresh. Test offline launch separately; do not treat offline mode as a way to bypass ownership/account requirements.
3. Versions: load the official version catalog, select representative 1.18.2, 1.20.1, 1.21.x and current supported release, install, then launch each compatible profile.
4. Existing-world Quick Play: launch a known save by its folder name; test a missing save and confirm the launcher reports/handles the failure instead of silently launching normal gameplay.
5. Server Quick Play: test hostname, hostname with port, IPv4 with port, IPv6 if supported, and malformed/blank input. Confirm malformed input does not silently become an ordinary launch.
6. New World: no launcher-owned Create New World screen exists in the current Kotlin/Java source tree. Use Minecraft's native Create New World screen for now. Automatic launcher creation-to-entry remains an open feature gap; do not add undocumented command-line flags or fabricate world metadata.
7. Mods: search and install one Modrinth mod and one CurseForge mod; test no results, pagination, cancellation, rate limiting, and retry after network restoration. JSON GET calls now retry transient I/O/DNS, HTTP 5xx and 429 with bounded backoff; POST is not automatically retried. Retry unit tests were added but not executed.
8. Renderer: test OpenGL on a known-compatible device. Test Vulkan only when the device/driver capability check reports support; confirm unsupported Vulkan falls back safely.
9. Low-end profile: apply the low-memory profile, launch, check memory allocation and render/simulation distances, then restore balanced settings.
10. Companion: drag, release after lifting, observe walk/sit/idle and roaming. Current ragdoll is an animation trigger, not a full rigid-body physics simulation.
11. Controls: verify touch layout editing, on-screen controls, keyboard, mouse, rotation, and landscape orientation in an actual game session.
12. Branding: verify app label, launcher icon, splash/loading collision animation, credits, and version label on a clean install and upgrade install.

## Build gate
Do not dispatch an APK build until the feature work is reviewed and the project owner explicitly approves the build. Source changes and source tests are not proof of a successful Android build or device test.

## Latest source commits (2026-11-06)
- `63847cc5933244185f20a3202dc28529cb7cf203` — stricter server address parsing.
- `0a97173e50bb313a3a1a6b3283bf7538df7532ce` — server address parser tests.
- `34e17ad92c9bfbec1991adb774a8678b3f2cdd9c` — bounded retry for transient JSON GET failures and HTTP 429/5xx.
- `7efdba33ba8f326e1a1d0a0b240761bf491b1247` — retry behavior tests.
- `d5aa9ef3d2364225a29322d51478163a352ea937` — retry-count semantics and input validation.
- `ff0cb045da3ebe09c71ceadc77b3621fdf22d6ff` — mod download retry documentation.
- `54fa11ca7722327a942d3bef5874da44c722b6ca` — New World source-tree audit.
