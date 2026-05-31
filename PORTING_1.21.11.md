# MapSyncer 1.21.11 Fabric port notes

This package was adjusted from the supplied source to target Minecraft/Fabric 1.21.11.

## Main changes

- `gradle.properties`
  - `minecraft_version=1.21.11`
  - `fabric_api_version=0.141.4+1.21.11`
  - Java target changed to 21.
- `build.gradle`
  - Uses `net.fabricmc.fabric-loom-remap`.
  - Uses Mojang official mappings via `loom.officialMojangMappings()`.
  - Uses `modImplementation` for Fabric Loader and Fabric API.
- `fabric.mod.json` / `mapsyncer.mixins.json`
  - Java requirement and mixin compatibility changed from 25 to 21.
- Networking registration
  - Replaced the newer `clientboundPlay()` / `serverboundPlay()` calls with 1.21.11 Fabric API `playS2C()` / `playC2S()`.
- Client rendering
  - Replaced `GuiGraphicsExtractor` with `GuiGraphics`.
  - Replaced `Screen#extractRenderState(...)` with `Screen#render(...)`.
  - Replaced 26.x helper names such as `text`, `centeredText`, `textWithWordWrap`, and `outline` with 1.21.11-compatible `drawString`, `drawCenteredString`, `drawWordWrap`, and `renderOutline`.

## Build

Use JDK 21, then run:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The mod jar should be generated under:

```text
build/libs/
```

## Verification note

The source was migrated and checked for obvious 26.x API names. In this sandbox, the Gradle build could not be completed because the environment could not resolve `services.gradle.org` to download the Gradle distribution. Build verification should be done locally or in a networked CI environment.
