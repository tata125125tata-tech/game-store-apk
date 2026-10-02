# 🎮 Cosmo Game Store — Android Client

An Android client for **Cosmo Game Store** with a built-in store browser, native library, curated recommendations, settings, and official APK & XAPK installer.

---

## 📥 Direct APK Download

### Option 1: GitHub Releases (Recommended)
Every push to `main` / `master` or release tag automatically compiles a debug APK and uploads it to GitHub Releases.

1. Navigate to the **[Releases](../../releases)** tab on GitHub.
2. Look for the latest release (e.g., `latest` or `v1.0.0`).
3. Under **Assets**, click **`CosmoGameStore-debug.apk`** to download directly to your Android device or PC.
4. On your device, tap the downloaded APK to install.
   *(Note: Ensure you allow "Install Unknown Apps" for your browser or file manager).*

### Option 2: GitHub Actions Artifacts
1. Go to the **[Actions](../../actions)** tab.
2. Click on the latest run under **Build & Release Debug APK**.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Download the **`CosmoGameStore-debug-apk`** ZIP archive, which contains the installable `.apk` file and its SHA-256 verification hash.

---

## ✨ Features

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

## ⚙️ Automated GitHub Actions Workflow

The repository includes a ready-to-run GitHub Actions workflow located at:
```text
.github/workflows/build-and-release.yml
```

### When It Runs:
1. **Push to `main` or `master`**: Automatically compiles the debug APK and updates the `latest` rolling release.
2. **Git Tags (`v*`)**: Whenever you push a tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`), a dedicated release is published.
3. **Manual Trigger (`workflow_dispatch`)**:
   - Go to **Actions** → **Build & Release Debug APK** → **Run workflow**.
   - Optionally specify a custom release title and tag.

---

## 🛠️ Local Development & Build

### Prerequisites:
- JDK 17 or JDK 21
- Android SDK (API 34 / 36)

### Build Commands:

```bash
# Build debug APK locally
./gradlew assembleDebug

# Run unit and Robolectric tests
./gradlew testDebugUnitTest
```

The resulting debug APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```
