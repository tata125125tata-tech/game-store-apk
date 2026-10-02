# 🎮 Cosmo Game Store — Android Client

[![Build & Release Debug APK](https://github.com/tata125125tata-tech/game-store-apk/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/tata125125tata-tech/game-store-apk/actions/workflows/build-and-release.yml)
[![Direct APK Download](https://img.shields.io/badge/Download-CosmoGameStore--debug.apk-00E5FF?style=flat&logo=android&logoColor=black)](https://github.com/tata125125tata-tech/game-store-apk/releases/latest/download/CosmoGameStore-debug.apk)
[![Latest Release](https://img.shields.io/github/v/release/tata125125tata-tech/game-store-apk?color=8B5CF6&label=Release)](https://github.com/tata125125tata-tech/game-store-apk/releases)

An Android client for **Cosmo Game Store** with a built-in store browser, native library, curated recommendations, settings, and official APK & XAPK installer.

---

## 📥 Direct APK Download Links

### ⚡ 1. One-Click Direct Download (Latest Release)
Tap the link below on your Android device to immediately download the latest compiled debug APK:

👉 **[Download CosmoGameStore-debug.apk](https://github.com/tata125125tata-tech/game-store-apk/releases/latest/download/CosmoGameStore-debug.apk)**

*(Direct Link: `https://github.com/tata125125tata-tech/game-store-apk/releases/latest/download/CosmoGameStore-debug.apk`)*

---

### 📦 2. Download from GitHub Releases
1. Visit the **[Releases Page](https://github.com/tata125125tata-tech/game-store-apk/releases)**.
2. Select the latest release version.
3. Under **Assets**, click **`CosmoGameStore-debug.apk`**.
4. Open the downloaded file to install on your Android device.

---

### 🛠️ 3. Download from GitHub Actions Run Artifacts
1. Go to the **[GitHub Actions Tab](https://github.com/tata125125tata-tech/game-store-apk/actions)**.
2. Click on the latest run of **Build & Release Debug APK**.
3. Scroll down to the **Artifacts** section.
4. Click **`CosmoGameStore-debug-apk`** to download the ZIP file containing the APK and its SHA-256 verification checksum.

---

## 🚀 How to Push to GitHub

To push your repository to `tata125125tata-tech/game-store-apk` and automatically trigger the build:

```bash
# Rename branch to main
git branch -M main

# Stage and commit all files
git add .
git commit -m "Initialize Cosmo Game Store with GitHub Actions CI/CD"

# Push to GitHub
git push -u origin main
```

Once pushed, GitHub Actions will automatically:
1. Compile the debug APK using JDK 21 and Gradle.
2. Generate SHA-256 integrity hash.
3. Publish a new Release on `https://github.com/tata125125tata-tech/game-store-apk/releases` with the direct download link!

---

## ✨ Application Features

- **Store Browser (Home)**: Seamlessly loads `https://cosmo-game.pages.dev/` with cached state and back-stack history.
- **Native APK/XAPK Interception**:
  - Automatically intercepts all `.apk` and `.xapk` download requests from the web store.
  - Streams real downloads using OkHttp with byte tracking, pause/resume, and speed indicators.
- **Official Package Installer**:
  - Hands APKs to Android's official package installer via `FileProvider`.
  - Parses XAPK archives, uncompresses split APKs, and executes multi-package installs via `android.content.pm.PackageInstaller.Session`.
  - Dispatches OBB expansion files when provided.
- **My Library**: Scans device games and downloaded titles, providing one-tap instant play launching and app management.
- **ForYou Curated Feed**: Native feed consuming the Cosmo API with hero highlights, screenshot carousels, and instant direct downloads.
- **Settings & Storage**:
  - Unknown app installation permission helper.
  - Optional auto-deletion of APKs once installed.
  - Storage breakdown and package cache clearing.

---

## 🛠️ Local Development & Build

```bash
# Build debug APK locally
./gradlew assembleDebug

# Run unit and Robolectric tests
./gradlew testDebugUnitTest
```
