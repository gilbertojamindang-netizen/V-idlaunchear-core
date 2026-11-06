# VøidLaunchear — Instant Play implementation plan

Status: partially implemented in existing source; end-to-end behavior is not yet build- or device-verified.

## User-facing goal

Make the path from tapping **Play** to entering Minecraft as direct as the Minecraft runtime permits. This is a launch-flow feature, not an in-game floating overlay.

## Required launch paths

1. **Existing single-player world** — one tap launches the selected Minecraft installation and opens the selected save automatically.
2. **New single-player world** — after the user finishes world creation settings and confirms Create, launch Minecraft and enter the newly created world without returning to the launcher or requiring a second manual world selection.
3. **Saved multiplayer server** — one tap launches the selected installation and connects to the selected server.
4. **Startup/loading optimization** — avoid redundant launcher-side work, reuse valid cached metadata/assets, and show clear progress while required runtime initialization continues.

## Correctness and safety requirements

- Never claim zero-second loading: Minecraft must initialize the JVM, renderer, assets, world data, and network connection.
- Do not skip required downloads, integrity checks, account authentication, or world initialization merely to make the progress screen disappear.
- Preserve the existing normal launch path as a fallback if a quick-play target is missing, incompatible, or fails to open.
- Validate save names and server addresses; never build shell commands by concatenating untrusted user input.
- A new-world flow must preserve all selected world options (name, seed, game mode, difficulty, cheats, datapacks, and version compatibility).
- Server quick join must report connection/authentication errors and allow a normal launch fallback.
- Quick Play must not silently change the selected Minecraft version or account.

## Existing implementation found in source (2026-11-06)

- `LaunchGameViewModel.kt` exposes `quickPlaySave(version, saveName)` and `quickPlayServer(version, address)`, which create `LaunchGameOperation.TryLaunch` with a `QuickPlay.Save` or `QuickPlay.Server` target.
- `LauncherElements.kt` defines the parcelable `QuickPlay` variants and the `LaunchGameOperation.RealLaunch` state, preserving the selected version and target through the account/renderer checks.
- The `LaunchGameOperation.RealLaunch` consumer assigns `version.quickPlaySingle = quickPlay` before calling `LaunchGameViewModel.start(...)`.
- `LaunchArgs.kt` reads `version.quickPlaySingle` and appends Minecraft's supported command-line arguments:
  - Save: `--quickPlaySingleplayer <save-name>`, only when the selected version metadata advertises single-player Quick Play support.
  - Server: `--quickPlayMultiplayer <host:port>` when supported, with a parsed-address legacy `--server <host> --port <port>` fallback otherwise.
- `LaunchArgs.kt` parses the server address with `ServerAddress.parse`; invalid addresses are logged and the automatic join arguments are omitted.
- `GameLaunchFlow.kt` still performs account validation, mod scanning, required integrity/download work, and then calls `runGame(context, version, account)`. The quick-play target is carried on the version object rather than passed as a separate parameter to this function.
- The selected-save callback is present in the saves UI. The existence of these paths means existing-world and server quick-play argument generation is already implemented in source; the remaining work is to verify save-name mapping, error/fallback behavior, and actual runtime behavior.
- The new-world creation-to-launch handoff has not yet been traced or verified. Do not mark it complete until the creation UI and game launch path preserve the complete world options and automatically open the new world.
- The splash loading screen already contains the VØID/LAUNCHER collision animation in current source; this area needs a visual/branding review rather than assuming it is absent.
- The manifest already enforces landscape orientation for launcher and game activities.

## Remaining implementation and verification

1. Trace how save metadata/name maps to the exact `--quickPlaySingleplayer` target expected by each supported Minecraft version, including unsupported versions and missing saves.
2. Trace the complete new-world creation UI and add a safe handoff so Create launches the chosen version and enters the newly created world with every selected option preserved. If Minecraft does not expose a supported direct-create argument for a version, use a compatible in-game creation handoff rather than inventing an unsupported CLI flag.
3. Verify server parsing and connection failure feedback, including preserving the selected version/account and allowing normal launch fallback.
4. Review launch preparation/caching only where correctness is preserved; keep required initialization, account checks, integrity checks, and downloads.
5. Add tests for target validation, missing saves, invalid server addresses, fallback behavior, and one-launch-at-a-time protection where the current test structure permits.
6. Verify on-device for an existing world, a newly created world, and a multiplayer server before marking Instant Play complete.

## Acceptance criteria

- Existing world: one Play action reaches the selected world without an extra world-list selection.
- New world: one Create confirmation reaches the newly created world without returning to the launcher.
- Server: one Join action reaches the selected server, with useful error feedback on failure.
- Cold and warm launches both work; warm launch may be faster but neither skips required game initialization.
- Normal launch remains available and all existing account/version protections remain intact.

## Additional source audit (2026-11-06)

- The saves screen sends `saveData.saveFile.name` to the Quick Play callback. This is the world-folder identifier expected by Minecraft's save quick-play argument; the displayed in-world level title is not substituted.
- Launcher image/video background selection already exists through `BackgroundViewModel.import(...)`, and the UI supports both image and video media. This was an audit finding, not a new feature implementation.
- The background import path was hardened in commit `9c450ac298106bc9612876601150c405a756808a`: it now copies and validates a staged replacement before moving the active background aside, and attempts rollback if installation fails. This prevents an invalid selection from deleting the currently working background.
- Image validation decodes image bounds; video validation uses `MediaMetadataRetriever`, so the staged temporary filename does not need the original extension.
- The staged import change has not been compiled or exercised on-device. Verify provider URI copying, image/video decoding, and rollback behavior in a future test pass.

- Quick Play request guards were added in commit `eed6c9c5161cac545e943c01d8d987810112da8c`: blank save identifiers are ignored, blank server addresses are ignored, and nonblank server addresses are trimmed before creating the Quick Play request. This prevents an empty target from accidentally falling through to a normal game launch. It does not replace full address parsing in `LaunchArgs.kt` or runtime connection verification.

## Verification status

Source inspection only. No Gradle build, APK build, or device test was run. Existing-world/server Quick Play argument construction is present in source, but feature-level behavior is not yet verified. New-world automatic entry remains outstanding.

## New-world source-tree audit (2026-11-06)

A repository-wide Kotlin/Java source-tree scan found the save metadata/parser
and existing-save manager screens, but no launcher-owned Create New World
screen or world-creation form to connect to Quick Play. The actual Create New
World interface is therefore currently the Minecraft runtime's own UI, not a
launcher screen exposed by this repository. A safe one-tap creation handoff
cannot be implemented by adding an unverified CLI flag or fabricating a
`level.dat`; doing so risks corrupt or incompatible worlds. Keep the native
Minecraft creation flow until a supported, version-aware integration exists.
This is an identified product gap, not a completed Instant Create World
feature.

## Invalid server Quick Play behavior (2026-11-06)

Commit `941158ed4abf9d15036d14b78f733c2cd3ca080a` changes invalid server
address handling in `LaunchArgs`: it now logs a clear validation message and
throws an argument error instead of omitting join arguments and silently
starting an ordinary single-player session. This closes a misleading fallback
path. The launcher UI's presentation of this error still needs on-device
verification.
