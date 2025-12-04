# StairVision Android App
## CS663 Mobile Vision Project - Real-Time Stair Detection

Android application for real-time stair detection using YOLOv8 and Google Gemini AI, providing multi-modal accessibility features.

---

## 📱 App Features

### Core Functionality
- **Real-Time Detection**: 4-5 FPS stair detection using TFLite
- **Stair Classification**: Ascending, descending, side view
- **Distance Estimation**: Proximity warnings (0.5-5m range)
- **Multi-Modal Feedback**:
  - Visual: Bounding boxes, hazard banners, status text
  - Audio: TTS announcements + Gemini AI guidance
  - Haptic: Distance-based vibration patterns

### AI Integration
- **Google Gemini 1.5 Flash**: Context-aware scene analysis
- **Intelligent Guidance**: Handrail location, step count, lighting assessment
- **Fallback System**: Basic TTS when offline

---

## 🚀 Quick Start

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer
- **Android Device** with Android 8.0+ (API 26+)
- **USB Debugging** enabled
- **Gemini API Key** (for AI features)

### Installation Steps

1. **Clone Repository**
```bash
git clone https://github.com/Yesh492/cs663-stair-detection-android.git
cd cs663-stair-detection-android
```

2. **Open in Android Studio**
- Launch Android Studio
- File → Open → Select project directory
- Wait for Gradle sync

3. **Configure API Key**
Create `local.properties` in project root:
```properties
sdk.dir=/YOUR/ANDROID/SDK/PATH
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

4. **Connect Device**
- Enable Developer Options (tap Build Number 7 times)
- Enable USB Debugging
- Connect via USB
- Allow USB debugging on device

5. **Build & Run**
- Click ▶️ green "Run" button
- Select your device
- Wait for build (~2-4 minutes first time)
- App launches automatically

---

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/stairvision/
│   │   ├── MainActivity.kt              # Main activity, CameraX setup
│   │   ├── YoloDetector.kt             # TFLite inference engine
│   │   ├── GeminiVisionAssistant.kt    # Gemini AI integration
│   │   ├── GeminiAudioAssistant.kt     # TTS management
│   │   ├── HapticFeedbackManager.kt    # Vibration control
│   │   ├── OverlayView.kt              # Bounding box visualization
│   │   ├── SettingsActivity.kt         # User preferences
│   │   ├── DetectionConfig.kt          # Global configuration
│   │   ├── AudioFeedbackManager.kt     # Legacy TTS
│   │   └── EnhancedConfig.kt           # Feature toggles
│   │
│   ├── assets/
│   │   └── stair_yolo_best_float32.tflite  # YOLOv8 model (12MB)
│   │
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml       # Main UI layout
│   │   │   └── activity_settings.xml   # Settings UI
│   │   └── values/
│   │       ├── strings.xml
│   │       └── colors.xml
│   │
│   └── AndroidManifest.xml              # App permissions & components
│
├── build.gradle.kts                      # App-level Gradle
└── proguard-rules.pro                    # Code obfuscation rules

build.gradle.kts                          # Project-level Gradle
settings.gradle.kts                       # Gradle settings
gradle/                                   # Gradle wrapper
gradlew                                   # Gradle wrapper script (Unix)
gradlew.bat                               # Gradle wrapper script (Windows)
```

---

## 🔧 Configuration

### Detection Parameters
Edit `DetectionConfig.kt`:
```kotlin
object DetectionConfig {
    const val CONFIDENCE_THRESHOLD = 0.6f    // Detection sensitivity
    const val ENABLE_AUDIO_FEEDBACK = true    // TTS announcements
    const val SHOW_BOUNDING_BOXES = true      // Visual overlay
    const val CAMERA_ROTATION = 90            // Device-specific
}
```

### Gemini AI Settings
- **Analysis Interval**: 8 seconds (rate limiting)
- **Image Size**: 512×512 (cost optimization)
- **Model**: gemini-1.5-flash
- **Fallback**: Basic TTS if API fails

---

## 📊 Performance Metrics

| Metric | Value | Device |
|--------|-------|--------|
| FPS | 4.2 avg | OnePlus 9 Pro |
| Inference Time | 187ms avg | Snapdragon 888 |
| Memory Usage | 18MB peak | 12GB RAM device |
| Battery Drain | 8%/hour | Continuous use |
| Detection Accuracy | 91% | Real-world testing |

---

## 🎯 Testing Scenarios

### Scenario 1: Basic Detection
1. Launch app
2. Grant camera permission
3. Point at stairs (ascending/descending)
4. Verify:
   - Red "HAZARD DETECTED" banner
   - Stair type displayed (⬆️/⬇️)
   - Green bounding box
   - Audio announcement

### Scenario 2: Emergency Stop
1. While detecting stairs
2. Tap red "STOP" button
3. Verify detection pauses
4. Tap green "START" to resume

### Scenario 3: Gemini AI
1. Detect stairs
2. Wait 8 seconds
3. Listen to Gemini guidance
4. Verify contextual information (handrail, steps, lighting)

### Scenario 4: Settings
1. Tap gear icon
2. Toggle haptic feedback
3. Return to main screen
4. Verify vibration disabled

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| App crashes on launch | Check Logcat; Rebuild project |
| No camera preview | Grant camera permission manually |
| Slow detection (<2 FPS) | Use high-end device; Close background apps |
| No audio | Check device volume; Verify TTS enabled |
| Gemini not working | Check API key in local.properties |
| Build errors | Sync Gradle; Invalidate caches |

### Logcat Commands

```bash
# View app logs
adb logcat | grep "MainActivity\|YoloDetector\|GeminiVision"

# Check for errors
adb logcat *:E

# Clear log buffer
adb logcat -c
```

---

## 📱 Supported Devices

### Tested On
- ✅ One Plus 9 Pro (Android 13) - **Primary Test Device**
- ✅ Google Pixel 6 (Android 14)
- ✅ Samsung Galaxy S21 (Android 13)

### Minimum Requirements
- Android 8.0+ (API 26)
- 2GB+ RAM
- Rear camera
- Internet (for Gemini AI, optional)

---

## 🔗 Related Repository

**Training Repository:**  
https://github.com/Yesh492/stair-detection-training

Contains:
- YOLO training notebooks
- Dataset conversion scripts
- Model artifacts
- Training documentation

---

## 📄 Permissions

Required permissions in AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## 🎓 CS663 Project Information

**Course**: CS663 Mobile Vision  
**Project**: StairVision - AI-Powered Stair Detection  
**Author**: Yeshwanth Nani  
**Model**: YOLOv8n (97% mAP@50)  
**Framework**: TensorFlow Lite + Google Gemini AI

---

## 📧 Contact

**Repository**: https://github.com/Yesh492/cs663-stair-detection-android  
**Training Repo**: https://github.com/Yesh492/stair-detection-training  
**Documentation**: See Wiki pages

---

## 📝 Build Information

**APK Location**: `app/build/outputs/apk/debug/app-debug.apk`

**Build Commands**:
```bash
# Debug build
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.example.stairvision/.MainActivity
```

---

**Note**: This repository contains ONLY the Android application. For training code, datasets, and model artifacts, see the `stair-detection-training` repository.
