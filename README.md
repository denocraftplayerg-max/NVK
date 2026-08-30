# LTW GPU Bridge — Minecraft 1.21.11

Fabric mod that feeds chunk positions and frustum planes to the LTW native library
for GPU-driven frustum culling via compute shaders.

## How it works
## Build

```bash
./gradlew build
```

Output: `build/libs/LTW-GPU-Mod-0.1.jar`

## Install

Copy the .jar to your PojavLauncher mods folder alongside LTW.

## Requirements

- Minecraft 1.21.11
- Fabric Loader >= 0.18.0
- Fabric API
- LTW library loaded by PojavLauncher
