# LTW GPU Bridge — Minecraft 1.21.11

[![Build & Release](https://github.com/denocraftplayerg-max/NVK/actions/workflows/build.yml/badge.svg)](https://github.com/denocraftplayerg-max/NVK/actions/workflows/build.yml)

Fabric mod that feeds chunk positions and frustum planes to the LTW native library
for GPU-driven frustum culling via compute shaders.

## 🏗️ Build

```bash
./gradlew build
```

Output: `build/libs/LTW-GPU-Mod-0.1.jar`

## 📦 Installation

1. Install **Minecraft 1.21.11** with **Fabric Loader 0.18.4+**
2. Install **Fabric API** (0.140.0+ for 1.21.11)
3. Copy the `.jar` from releases to your PojavLauncher `mods/` folder
4. Ensure LTW native library (`libltw.so`) is loaded by PojavLauncher

## 🔄 How it works
## 📋 Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.18.0 |
| Fabric API | 0.140.0+ |
| Java | 21+ |
| LTW library | Loaded by PojavLauncher |

## 🏷️ Release Tags

Create a release by pushing a tag:

```bash
git tag v0.1.0
git push origin v0.1.0
```

GitHub Actions will automatically build and create a GitHub Release with the `.jar`.

## 📜 License

LGPL-3.0 — Copyright (c) 2025 Denorium / Denorium Protocol
