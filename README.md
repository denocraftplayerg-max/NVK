# <a href="https://github.com/xCollateral/VulkanMod"> <img src="./src/main/resources/assets/vulkanmod/Vlogo.png" width="30" height="30"/> </a> VulkanMod (Multi-Screen Fix Fork)

[English](#english) | [Українська](#ukrainian)

---

<a name="english"></a>
## 🇬🇧 English Description

This is a fork of the **VulkanMod** fabric mod, which introduces a brand new **Vulkan** based voxel rendering engine to **Minecraft Java**.

**The main purpose of this fork is to fix critical bugs and ensure stability on multi-screen systems (multi-monitor setups).**

### Why this fork?
- **Multi-Screen Fix:** The original mod may have issues with initialization or rendering when using multiple displays. This version is designed to solve these specific problems.
- **Retains original benefits:** Replaces the default OpenGL renderer with Vulkan 1.2 and brings performance improvements.
- **Performance:** Reduced CPU overhead and utilization of modern hardware capabilities.

### Demonstration (Original Mod):
[![Demonstration Video](http://img.youtube.com/vi/sbr7UxcAmOE/0.jpg)](https://youtu.be/sbr7UxcAmOE)

## Installation

### Download:
Please download the releases **from this repository** to get the multi-screen fixes.

### Install guide:
>1) Install the [fabric modloader](https://fabricmc.net).
>1) Download the `Vulkanmod.jar` file (from this fork) and put it into `.minecraft/mods`.
>1) Enjoy playing on multiple monitors!

## Features

### Fixes in this fork:
>- [x] **Correct behavior on multi-monitor systems**
>- [x] Window initialization fixes

### Optimizations (from original mod):
>- [x] Multiple chunk culling algorithms
>- [x] Reduced CPU overhead & Improved GPU performance
>- [x] Indirect Draw mode
>- [x] Chunk rendering optimizations

## Notes
- This fork is based on the work by [xCollateral](https://github.com/xCollateral/VulkanMod).
- Please report multi-screen specific issues in the Issues tab of this repository.

---

<a name="ukrainian"></a>
## 🇺🇦 Український опис

Це форк Fabric-моду **VulkanMod**, який впроваджує новий воксельний рушій рендерингу на базі **Vulkan** для **Minecraft Java**.

**Головна мета цього форку — виправлення критичних помилок та забезпечення стабільної роботи на системах із кількома моніторами (мультиекранних конфігураціях).**

### Чому цей форк?
- **Виправлення мультиекранності:** Оригінальний мод може мати проблеми з ініціалізацією або рендерингом при використанні кількох дисплеїв. Ця версія створена, щоб вирішити ці проблеми.
- **Зберігає всі переваги оригінального VulkanMod:** Заміна стандартного OpenGL рендерера на Vulkan 1.2.
- **Продуктивність:** Зменшення навантаження на процесор та використання сучасних можливостей відеокарт.

### Встановлення

### Завантаження:
Завантажуйте релізи **саме з цього репозиторію (форку)**, щоб отримати виправлення для мультиекранних систем.

### Інструкція:
>1) Встановіть [Fabric modloader](https://fabricmc.net).
>1) Завантажте файл `Vulkanmod.jar` (з цього форку) та помістіть його в папку `.minecraft/mods`.
>1) Насолоджуйтесь грою на кількох моніторах!

## Особливості

### Виправлення у цьому форку:
>- [x] **Коректна робота на системах з кількома моніторами**
>- [x] Виправлення помилок ініціалізації вікна

### Оптимізації (з оригінального моду):
>- [x] Різноманітні алгоритми відсікання чанків (culling)
>- [x] Зменшене навантаження на CPU та покращена продуктивність GPU
>- [x] Режим Indirect Draw (зменшує оверхед CPU)
>- [x] Оптимізація рендерингу чанків

## Примітки
- Цей форк базується на чудовій роботі [xCollateral](https://github.com/xCollateral/VulkanMod).
- Будь ласка, повідомляйте про проблеми, специфічні для мультиекранної роботи, у вкладку Issues цього репозиторію.
