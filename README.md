# VøidLaunchear

<p align="center">
  <strong>A community-built Android launcher for Minecraft: Java Edition.</strong><br>
  A custom VØID identity, configurable launcher experience, and ongoing performance work.
</p>

<p align="center">
  <b>App identity:</b> VøidLaunchear &nbsp;·&nbsp; <b>Platform:</b> Android
</p>

---

## About

VøidLaunchear is an independent Android launcher project for Minecraft: Java Edition. This repository contains the launcher source code, project customizations, and build configuration.

The project is actively developed. Features, device support, and compatibility can vary by release, Android version, Minecraft version, renderer, and hardware.

## Project features and direction

- Custom VøidLaunchear branding and launcher interface.
- Minecraft Java version management and game launching.
- Account and local game configuration support provided by the underlying open-source components.
- Mod and game-file management integrations.
- Configurable controls, Java runtimes, and graphics/rendering options.
- Renderer and Vulkan-related options where supported by the device and installed drivers.
- Launcher appearance customization, including theme colors and background options available in the app.
- Quick Join improvements, including server compatibility feedback and connection-error reporting where detectable.
- Performance and low-device optimization work.

Some items are still being improved. This list does not guarantee that every feature works on every device or for every Minecraft release.

## Compatibility

- **Operating system:** Android.
- **Game edition:** Minecraft: Java Edition.
- **Target Minecraft versions:** 1.18.2 and newer, subject to available game files, Java runtime requirements, and launcher compatibility.
- **Graphics:** OpenGL/Vulkan-related functionality depends on the selected renderer, GPU, Android version, and driver support.

Minecraft is a trademark of Mojang Synergies AB. VøidLaunchear is an independent, unofficial project and is not affiliated with, endorsed by, or sponsored by Mojang or Microsoft.

## Build from source

This project uses Gradle, Kotlin, and Android build tooling.

1. Clone or download this repository.
2. Open the project in a compatible Android development environment.
3. Install the Android SDK/JDK versions required by the Gradle configuration.
4. Allow Gradle to sync and resolve dependencies.
5. Build using the included Gradle wrapper or the repository's GitHub Actions workflow.

Build variants, SDK requirements, and workflow details may change. Refer to the current Gradle files and workflow configuration before building.

## Project credits

- **Project direction, customization, and coordination:** Gilbert Jamindang
- **Original open-source project and upstream contributors:** credited in the retained source headers and license notices.
- **Third-party libraries and components:** their respective authors and maintainers.

VøidLaunchear is based on and includes code from existing open-source projects. Original copyright notices, required attribution, license texts, and applicable license terms are retained. This README is not a replacement for those notices.

## License

The project source is distributed under the license and applicable terms included in the repository. Read [LICENSE](LICENSE) before using, modifying, or redistributing this software. Third-party components may have their own licenses and notices.

## Disclaimer

You are responsible for complying with the Minecraft EULA, applicable platform terms, and the licenses of all included components. Do not use this project to distribute unauthorized game files or other copyrighted material.

---

<p align="center">
  <strong>VØID · VøidLaunchear</strong><br>
  An independent community project.
</p>
