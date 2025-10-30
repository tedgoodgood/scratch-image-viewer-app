# Fui图片查看器 (ComposeApp)

**Fui图片查看器** is a modern Android image viewer application built entirely with **Kotlin**, **Jetpack Compose**, and **Material Design 3**. It features an innovative scratch overlay system for privacy protection, allowing users to reveal hidden images by brushing away a customizable overlay.

**元亨利贞,有利于我** - Offline protection, auspicious and beneficial.

## Features

### Core Viewer Capabilities
- **Image Gallery**: Browse through multiple images with intuitive navigation
- **Previous/Next Navigation**: Button controls and keyboard arrow keys (← →)
- **Swipe Gestures**: Horizontal swipe to navigate between images
- **Fullscreen Mode**: Toggle to immersive fullscreen viewing (hides status & navigation bars)
- **Orientation Support**: Responsive layout adapts to portrait and landscape orientations

### Scratch Overlay System (Privacy Protection)
- **Customizable Overlay**: Apply a dark overlay to hide image content
- **Brush Tool**: Draw/scratch to reveal the underlying image
  - Adjustable brush size (8-160 dp)
  - Color picker with 12 preset colors
  - Smooth stroke rendering with rounded caps
  - Real-time brush preview
- **"全看" Reset Button**: Instantly clear all scratch marks to hide the image again
- **Overlay Image**: Load an additional image as overlay for enhanced privacy

### Settings Panel
- **Brush Size Slider**: Adjust stroke width with live display (8-160 dp)
- **Color Picker**: 12 discrete color swatches with visual selection indicator
- **Overlay Tools**: 
  - Select overlay image from device storage
  - Clear overlay button
  - Display selected overlay file name
- **Brush Preview**: Visual representation of current brush size and color
- **Responsive Layout**: 
  - Bottom panel in portrait mode
  - Side panel in landscape mode

### UI/UX
- **Top Control Bar**:
  - Dropdown menu for quick image selection
  - Import images button
  - Previous/Next buttons with disabled states
  - Settings toggle
  - Reset overlay button ("全看")
  - Fullscreen toggle
- **Footer Text**: "Fui图片查看器 - 离线保护,元亨利贞,有利于我"
- **Accessibility**: Content descriptions and semantic labels for all controls
- **State Persistence**: ViewModel ensures state survives configuration changes (rotations)
- **Edge-to-Edge Display**: Modern immersive UI with system bar integration

## Tech Stack

| Tool / Library | Version | Purpose |
| -------------- | ------- | ------- |
| Kotlin | 2.0.0 | Primary language |
| Android Gradle Plugin | 8.5.2 | Build system |
| Compose BOM | 2024.09.01 | UI framework |
| Material 3 | via Compose BOM | Design system |
| Activity Compose | 1.9.2 | Activity integration |
| Lifecycle ViewModel Compose | 2.8.4 | State management |
| Lifecycle Runtime Compose | 2.8.4 | Lifecycle awareness |
| Coil Compose | 2.7.0 | Image loading |
| Accompanist System UI Controller | 0.36.0 | System bars control |
| Material Icons Extended | via Compose BOM | Icon library |
| Kotlinx Coroutines Android | 1.8.1 | Async operations |

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
│   │   │   ├── ComposeApp.kt              # Application class
│   │   │   ├── MainActivity.kt            # Main activity hosting ImageViewerScreen
│   │   │   ├── ui/theme/                  # Material 3 theme setup
│   │   │   └── viewer/                    # Image viewer module
│   │   │       ├── ImageViewerScreen.kt   # Main viewer UI (controls, layout, gestures)
│   │   │       └── ImageViewerViewModel.kt # State management (images, brush, overlay)
│   │   ├── res/                           # Resources (icons, strings, themes)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/                                # Gradle wrapper files
├── scripts/
│   └── generate_keystore.sh               # Release keystore helper script
├── build.gradle.kts                       # Root Gradle config
├── settings.gradle.kts                    # Gradle settings
├── gradle.properties                      # Global Gradle properties
├── keystore.properties.example            # Signing config template
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

## Usage Instructions

### Basic Navigation
1. **Launch the app**: The viewer opens with 3 sample images
2. **Navigate images**:
   - Tap **Previous (←)** or **Next (→)** buttons
   - Use **keyboard arrow keys** (on devices with keyboards)
   - **Swipe left/right** on the image
   - Select from the **dropdown menu** in the top bar

### Importing Your Own Images
1. Tap **"选择图片"** (Select Images) in the top bar
2. Choose one or multiple images from your device
3. Selected images are added to the gallery

### Using the Scratch Overlay
1. **Draw on the image**: Touch and drag to create scratch strokes
   - The overlay darkens the image initially
   - Your strokes reveal the underlying image
2. **Adjust brush size**: Open settings panel, use the slider
3. **Change brush color**: Tap a color swatch in the settings panel
4. **Reset overlay**: Tap **"全看"** (Reset) button to hide the image again
5. **Clear strokes**: Navigate to another image and back, or use reset

### Settings Panel
1. **Open settings**: Tap the **Settings** icon (gear) on the right side
2. **Brush Size**: Adjust slider (8-160 dp), see live preview below
3. **Brush Color**: Choose from 12 colors (black, gray, blue, green, etc.)
4. **Overlay Tools**:
   - **Select Overlay**: Load an image to use as overlay texture
   - **Clear Overlay**: Remove the loaded overlay image

### Fullscreen Mode
1. **Enter fullscreen**: Tap the **Fullscreen** icon in the top bar
   - Status and navigation bars hide
   - Controls hide for immersive viewing
2. **Exit fullscreen**: 
   - Tap the **Close (X)** button in the top-left corner
   - Or tap the **Fullscreen Exit** icon (if visible)

### Orientation Changes
- The app automatically adapts to **portrait** and **landscape** orientations
- In landscape, the settings panel appears on the right side
- In portrait, the settings panel appears at the bottom
- All state (current image, brush settings, strokes) persists across rotations

### Keyboard Shortcuts
- **Left Arrow (←)**: Previous image
- **Right Arrow (→)**: Next image

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

## Permissions

The app requests the following permissions:
- **INTERNET**: For loading sample images from Unsplash
- **READ_EXTERNAL_STORAGE** (API ≤ 32): For importing images from device storage
- **READ_MEDIA_IMAGES** (API ≥ 33): For importing images on Android 13+

## Architecture

- **MVVM Pattern**: ViewModel manages UI state, Screen composables observe state
- **State Management**: Kotlin `StateFlow` for reactive UI updates
- **Immutable State**: `ViewerUiState` data class ensures predictable state changes
- **Coroutines**: Async operations (image loading, overlay decoding) run on IO dispatcher
- **Compose Canvas**: Custom drawing for scratch overlay with `BlendMode.Clear` to reveal images
- **Gesture Detection**: `detectDragGestures` for brush strokes, `detectHorizontalDragGestures` for swipe navigation
- **System UI Control**: `SystemUiController` for fullscreen mode (hide/show system bars)
- **Focus Management**: `FocusRequester` for keyboard arrow key handling

## Key Compose Features Demonstrated

- **BoxWithConstraints**: Responsive layout based on available space
- **AnimatedVisibility**: Smooth show/hide animations for controls and settings
- **LaunchedEffect**: Lifecycle-aware side effects (system bars, focus)
- **Custom Canvas Drawing**: Scratch overlay with path rendering
- **Blend Modes**: `BlendMode.Clear` for erasing overlay
- **Activity Result API**: File picker integration for importing images
- **State Hoisting**: ViewModel provides single source of truth
- **Material 3 Components**: TopAppBar, Surface, Card, Slider, Button, Icon, etc.
- **Edge-to-Edge**: Modern immersive UI with `enableEdgeToEdge()`

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
- Check `AndroidManifest.xml` for `INTERNET` permission

**Scratch overlay not working**
- Ensure you're using Android API 24+ (minSdk requirement)
- Check if strokes are too small (increase brush size in settings)

**Fullscreen not working**
- Verify `accompanist-systemuicontroller` is included in dependencies
- Check if device supports immersive mode (some emulators have limitations)

## Future Enhancements

- Undo/Redo for scratch strokes
- Save scratched images to gallery
- Custom overlay opacity control
- HSV color picker for more color choices
- Multiple overlay layers
- Export/share functionality
- Gesture zoom/pan on images
- Dark/Light theme toggle
- Localization (English, Chinese)
- Image metadata display (EXIF)

## Downloading Pre-built APKs

### From GitHub Actions

This repository includes GitHub Actions that automatically build debug APKs on every push and pull request.

**To download:**
1. Go to the [Actions tab](../../actions) of this repository
2. Click on the most recent workflow run (typically named "Build Android APK")
3. Scroll down to the "Artifacts" section
4. Download `composeapp-debug-apk`
5. Extract the ZIP and install the APK on your device

**Note:** Debug APKs are signed with the debug keystore and are suitable for testing but not for production release.

### From GitHub Releases

For push events to the main/master branch, automated debug builds are also published as pre-releases:

1. Navigate to the [Releases page](../../releases)
2. Look for pre-release builds tagged as `build-<number>`
3. Download the attached APK file (e.g., `composeapp-debug-<number>.apk`)
4. Install on your Android device

### Installing APKs on Android Devices

To install downloaded APKs:
1. **Enable "Install from Unknown Sources"** in your device settings (Android 8+ has per-app settings)
2. Transfer the APK to your device (USB, email, cloud storage, etc.)
3. Open the APK file using a file manager
4. Follow the on-screen prompts to install

**Security Note:** Always verify the source of APK files before installation. Only install APKs from trusted sources.

### Setting Up Release Signing for CI/CD

The current workflow builds debug-signed APKs. To build production-ready release APKs in CI/CD:

1. **Generate a release keystore** (follow the "Release Signing Configuration" section above)

2. **Add the keystore as a GitHub Secret:**
   - Encode your keystore to base64: `base64 -i release.keystore.jks > keystore.txt`
   - Go to repository Settings → Secrets and variables → Actions
   - Add the following repository secrets:
     - `KEYSTORE_FILE`: Contents of `keystore.txt` (base64-encoded keystore)
     - `KEYSTORE_PASSWORD`: Your keystore password
     - `KEY_ALIAS`: Your key alias (e.g., "composeapp")
     - `KEY_PASSWORD`: Your key password

3. **Modify the workflow** to decode the keystore and configure signing:
   ```yaml
   - name: Decode keystore
     run: |
       echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > release.keystore.jks

   - name: Build release APK
     run: ./gradlew assembleRelease
     env:
       KEYSTORE_FILE: release.keystore.jks
       KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
       KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
       KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
   ```

4. **Update the signing config** in `app/build.gradle.kts` to read from environment variables when available

## License / Contributing

Add your preferred license and contribution guidelines here.

---

**Fui图片查看器** - 离线保护,元亨利贞,有利于我

Built with ❤️ using Kotlin & Jetpack Compose. Happy viewing! 🎉
