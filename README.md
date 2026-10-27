# 👟 StepVerse (MyFit) v1.3

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="StepVerse Logo"/>
</p>

Welcome to **StepVerse** (MyFit) **v1.3**, a modern, minimalist, premium Android step-counting application built with **Jetpack Compose** and **Material 3**, inspired by Apple Fitness, Nothing OS, and Samsung Health.

---

## ✨ Key Features & Highlights

### ⚡ 1. 24/7 Background Step Tracking
- **Always Active**: Runs as a robust Foreground Service equipped with a partial `WakeLock` and health tracking permissions.
- **Screen-Lock & Off Support**: Keeps tracking your steps accurately even when your phone is in your pocket, screen is locked, or display is turned off.

### 🛡️ 2. Smart Hybrid Step Engine (Zero Inflation)
- **Hardware Priority**: Automatically prioritizes the hardware step counter sensor (`TYPE_STEP_COUNTER`) for absolute 1:1 physical step accuracy.
- **Intelligent Accelerometer Fallback**: Strict mathematical cadence filtering guarantees zero step count inflation.

### 🎨 3. App Customization & UI Styles
- **Theme Palettes & UI Styles**: Personalize your app experience with custom color palettes, daily step goal adjustment, and translucency controls.

---

## 🛠️ Tech Stack & Architecture
- **UI Framework**: Jetpack Compose & Material 3 (Clean Dark Theme).
- **Architecture**: MVVM with Kotlin Coroutines and reactive `StateFlow`.
- **Local Persistence**: Room Database for offline step storage.

---

## 📦 Getting Started & Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/KAIRO407/MyFit.git
   ```
2. Open the project in **Android Studio**.
3. Install the pre-built **[MyFit.apk](MyFit.apk)** directly on your Android device!

---

## 👨‍💻 Developed By
**Sujal**
