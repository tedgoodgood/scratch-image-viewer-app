# ComposeApp

ComposeApp is a modern Android starter project built entirely with **Kotlin**, **Jetpack Compose**, and **Material Design 3**. It is configured with Kotlin DSL Gradle scripts, sensible defaults, and the essential libraries you need to ship a polished Compose application quickly.

## Features

- Jetpack Compose UI powered by the latest Compose BOM
- Material 3 theming with dynamic color support
- Kotlin-first Gradle configuration (Kotlin DSL)
- Code shrinking and resource optimization enabled for release builds
- Coil integration for image loading
- Accompanist System UI Controller for status/navigation bar styling
- Release signing configured via `keystore.properties`
- Gradle wrapper pinned to Gradle **8.7** and AGP **8.5.2**

## Tech Stack

| Tool / Library | Version |
| -------------- | ------- |
| Kotlin | 2.0.0 |
| Android Gradle Plugin | 8.5.2 |
| Compose BOM | 2024.09.01 |
| Material 3 | via Compose BOM |
| Activity Compose | 1.9.2 |
| Lifecycle Runtime Compose | 2.8.4 |
| Coil Compose | 2.7.0 |
| Accompanist System UI Controller | 0.36.0 |

**Platform Targets**
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34

## Prerequisites

1. **JDK 17** (Temurin, Oracle, or OpenJDK)
   - Verify with `java -version`
   - Set `JAVA_HOME` to the installed JDK 17 path
2. **Android Studio (Hedgehog or newer)**
   - Includes required Android SDK and build tools
3. **Android SDK**
   - If building from the command line, ensure `ANDROID_SDK_ROOT` or `ANDROID_HOME` is set

> Tip: A helper file `local.properties.example` is included. Copy it to `local.properties` and set your SDK path if Gradle cannot find it.

## Project Structure

```
.
├── app/
│   ├── src/main/
│   │   ├── java/com/example/composeapp/
│   │   │   ├── ComposeApp.kt            # Application class
│   │   │   ├── MainActivity.kt          # Main activity hosting Compose UI
│   │   │   └── ui/theme/                # Material 3 theme setup
│   │   ├── res/                         # Resources (icons, strings, themes)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/                              # Gradle wrapper files
├── scripts/
│   └── generate_keystore.sh             # Release keystore helper script
├── build.gradle.kts                     # Root Gradle config
├── settings.gradle.kts                  # Gradle settings
├── gradle.properties                    # Global Gradle properties
├── keystore.properties.example          # Signing config template
└── README.md
```

## Getting Started

### 1. Clone the repository
```bash
git clone <repository-url>
cd ComposeApp
```

### 2. Open in Android Studio (Recommended)
1. `File > Open`
2. Select the project directory
3. Let Gradle sync complete
4. Press **Run** (Shift + F10) to install on a connected device or emulator

### 3. Command-line workflows
Make the Gradle wrapper executable (macOS/Linux):
```bash
chmod +x gradlew
```

Build the debug APK:
```bash
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:
```bash
./gradlew installDebug
```

## Build Variants

| Variant | Description |
| ------- | ----------- |
| `debug` | Developer-friendly build with no shrinking and debug signing |
| `release` | Optimized build with code + resource shrinking, uses release keystore if configured |

Build commands:
```bash
./gradlew assembleDebug     # Debug APK
./gradlew assembleRelease   # Release APK (requires signing config)
./gradlew bundleRelease     # Optional AAB build (if needed)
```

> **APK-first workflow:** This project is set up to generate APKs by default. Use `assembleRelease` for a signed release APK. App Bundles (`bundleRelease`) remain available if required.

## Release Signing Configuration

Release builds read signing credentials from `keystore.properties`. The file is ignored by Git. A template is provided as `keystore.properties.example` (copy it and fill in real values).

### Step 1: Generate a Keystore (recommended script)
```bash
./scripts/generate_keystore.sh
```
The script creates `release.keystore.jks` (configurable inside the script) and prints next steps.

### Step 2: Configure `keystore.properties`
```
storeFile=release.keystore.jks
storePassword=your_keystore_password
keyAlias=composeapp
keyPassword=your_key_password
```

**Expected keys**

| Property | Description |
| -------- | ----------- |
| `storeFile` | Relative or absolute path to your JKS/PKCS12 keystore |
| `storePassword` | Password for the keystore file |
| `keyAlias` | Alias inside the keystore |
| `keyPassword` | Password for the key alias |

> ⚠️ **Security:** Never commit `keystore.properties` or the keystore file. Store backups securely.

### Step 3: Build the release APK
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

If `keystore.properties` is missing, the release build falls back to the debug signing config so you can still produce a testable APK.

#### Manual keystore generation (alternative)
```bash
keytool -genkey -v \
  -keystore release.keystore.jks \
  -alias composeapp \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

## Code Shrinking & Optimization

Release builds enable:
- **R8/ProGuard** (`isMinifyEnabled = true`) using the optimized default rules + `proguard-rules.pro`
- **Resource shrinking** (`isShrinkResources = true`) to remove unused resources

This setup keeps release APKs lean without affecting debug builds.

## Useful Gradle Tasks

```bash
./gradlew tasks                 # List available tasks
./gradlew clean                 # Clean build outputs
./gradlew lint                  # Run Android lint checks
./gradlew test                  # JVM unit tests
./gradlew connectedAndroidTest  # Instrumented tests (requires device)
```

## Troubleshooting

**SDK not found**
- Copy `local.properties.example` to `local.properties`
- Update `sdk.dir=/absolute/path/to/Android/Sdk`

**Unsupported Java version**
- Ensure JDK 17 is installed and `JAVA_HOME` points to it

**Gradle sync issues**
- Check your internet connection for dependency downloads
- Try `./gradlew --refresh-dependencies`

**Images not loading**
- Coil requires Internet access; ensure emulator/device has connectivity

## License / Contributing

Add your preferred license and contribution guidelines here.

---
Happy building with ComposeApp! 🎉
