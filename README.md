# 🏃‍♂️ MyFit v1.1 — Professional Fitness Edition

Welcome to **MyFit**, a modern, lightweight, and battery-efficient Android fitness tracking application built with **Jetpack Compose** and **Material 3**. 

MyFit is designed to help you effortlessly track your daily steps, monitor your walking history, and achieve your fitness goals with a gorgeous, highly customizable UI.

---

## ✨ Key Features & Highlights

### ⚡ 1. 24/7 Background Step Tracking
- **Always Active**: Runs as a robust Foreground Service equipped with a partial `WakeLock` and health tracking permissions.
- **Screen-Lock & Off Support**: Keeps tracking your steps accurately even when your phone is in your pocket, screen is locked, or display is turned off.

### 🛡️ 2. Smart Hybrid Step Engine (Zero Inflation)
- **Hardware Priority**: Automatically prioritizes the hardware step counter sensor (`TYPE_STEP_COUNTER`) for absolute 1:1 physical step accuracy.
- **Intelligent Accelerometer Fallback**: If the hardware step counter is silent (common on emulators and certain test devices), our smart hybrid accelerometer fallback algorithm takes over automatically.
- **Mathematical Cadence Filtering**: Uses strict mathematical bounds (`deltaMag in 3.2f..8.0f` with a `400ms` refractory period) to prevent false positives from hand shakes, typing, or vibrations—guaranteeing 15 actual steps count as 15 steps (no artificial 500+ step inflation).

### 🎨 3. Vibrant Themes & Custom UI Styles
- **8 Vibrant Theme Palettes**: Choose from Cyber Cyan, Electric Violet, Sunset Orange, Neon Pink, Emerald Mint, Solar Gold, Midnight Blue, and Crimson Fire.
- **3 Dynamic UI Styles**: Switch between **Glassy (Neon Glow)**, **Translucent (Frosted)**, and **Opaque (Solid Dark)**.
- **Adjustable Translucency Intensity**: Fine-tune your card opacity slider to your personal preference.

### 🏆 4. Interactive Dashboard & Milestones
- **Swipeable Pager**: Seamlessly swipe between **Dashboard**, **Step History Logs**, and **Fitness Milestones**.
- **Ergonomic Bottom-Left Vertical Controls**: Settings and Milestones buttons are stacked neatly on the bottom-left for effortless thumb reach.
- **Goal Achievement Celebrations**: Receive celebratory trophy modal alerts the moment you crush your daily step target.

---

## 🛠️ Tech Stack & Architecture
- **UI Framework**: Jetpack Compose & Material 3 (with custom dynamic gradients and glassmorphism styling).
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines and reactive `StateFlow`.
- **Local Persistence**: Room Database for offline daily step storage and historical log retrieval.
- **Background Processing**: Android Foreground Services (`health` type) with persistent notifications and WakeLocks.

---

## 📦 Getting Started & Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/KAIRO407/MyFit.git
   ```
2. Open the project in **Android Studio**.
3. Connect an Android device or start an emulator (API 29+).
4. Build and run the app or install the pre-built [`app-debug.apk`](app-debug.apk) directly from the repository root.

---

## 👨‍💻 Developed By
**Sujal**
