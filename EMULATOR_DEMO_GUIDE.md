# StairVision Emulator Demo Guide
## Remote Demo Without Physical Device

This guide explains how to demonstrate StairVision using Android Studio's emulator for remote presentations or testing without a physical device.

---

## ⚠️ Important Limitations

**Emulator Camera Limitations:**
- Emulator cameras have limited functionality compared to physical devices
- **Recommended**: Use screen recording + voiceover for professional demo
- **Alternative**: Use emulator with webcam for live demo (limited quality)

---

## 🎬 METHOD 1: Pre-Recorded Demo (RECOMMENDED)

### Best for: Professional presentations, consistent results

This method uses screen recording of the app running on a physical device or emulator with pre-loaded test images.

### Steps:

#### 1. Prepare Test Images
```bash
# Create test images directory
mkdir -p ~/Desktop/StairDemo/test_images

# Copy stair images from dataset
cp "/Users/yeshwanthnani/Downloads/RGB-D stair dataset/RGB-D stair dataset/test/images/"*.png ~/Desktop/StairDemo/test_images/

# You should have ascending, descending, and side-view stair images
```

#### 2. Record Demo Video

**Option A: Using Actual Device**
1. Connect OnePlus 9 Pro via USB
2. Open StairVision app
3. Use macOS QuickTime Player:
   - File → New Movie Recording
   - Click dropdown next to record → Select your iPhone/Android device
   - Point camera at stair images on laptop screen
   - Record detection working

**Option B: Using Emulator with Image Feed**
1. Launch emulator (see Method 2)
2. Use **scrcpy** for better screen recording:
```bash
# Install scrcpy
brew install scrcpy

# Record emulator screen
scrcpy --record=stairvision_demo.mp4
```

#### 3. Edit and Annotate
- Use iMovie or similar to add:
  - Title slides explaining features
  - Voiceover describing detection process
  - Annotations highlighting UI elements
  - Text overlays for metrics (FPS, accuracy)

#### 4. Upload to YouTube
- Unlisted link for instructor
- Public for portfolio

---

## 💻 METHOD 2: Live Emulator Demo

### Best for: Interactive Q&A, testing modifications

### Step 1: Create AVD (Android Virtual Device)

1. **Open Android Studio**
2. **Tools → Device Manager**
3. **Create Device**

**Recommended Configuration:**
```
Device: Pixel 6 Pro
API Level: 34 (Android 14)
System Image: Google APIs (x86_64)
RAM: 4096 MB
VM Heap: 512 MB
Internal Storage: 8 GB
```

4. **Enable Camera**:
   - Check "Enable Device Frame"
   - Under "Advanced Settings":
     - Front Camera: **Webcam0** or **Emulated**
     - Back Camera: **Webcam0** or **Emulated**

### Step 2: Configure Emulator Settings

After creating AVD:

1. **Launch Emulator**
   ```bash
   # From command line (optional - faster)
   ~/Library/Android/sdk/emulator/emulator -avd Pixel_6_Pro_API_34 -camera-back webcam0
   ```

2. **In Emulator, Enable Camera**:
   - Click "..." (Extended Controls)
   - Camera → Set to "VirtualScene"
   - Or Camera → Webcam0 for your Mac webcam

### Step 3: Load StairVision Project

1. **File → Open** → Select `cs663-stair-detection-android`
2. **Wait for Gradle Sync**
3. **Select Emulator** from device dropdown
4. **Click ▶️ Run**

### Step 4: Feed Test Images

**Option A: Using Emulated Camera (VirtualScene)**
- Limited usefulness as it shows 3D scene, not real images
- App will launch but won't detect stairs

**Option B: Using Webcam**
1. Display stair images on your laptop screen
2. Point webcam at screen
3. App will attempt detection (limited success due to camera quality)

**Option C: Using scrcpy to Feed Pre-loaded Images** (BEST)
1. Push test images to emulator:
   ```bash
   # Push images to emulator downloads
   adb push ~/Desktop/StairDemo/test_images/ /sdcard/Download/Stairs/
   ```

2. Modify app to load from gallery instead of camera (requires code change)

---

## 🎥 METHOD 3: Screen Recording + Live Narration

### Best for: Live presentations, online defenses

### Setup:

1. **Prepare Screen Recording Software**:
   ```bash
   # Install OBS Studio (free, professional)
   brew install --cask obs
   ```

2. **OBS Configuration**:
   - Add Source → Window Capture → Android Studio Emulator
   - Add Source → Display Capture (if showing code)
   - Add Source → Audio Input (microphone for narration)

3. **Presentation Flow**:
   ```
   Scene 1: Project Overview (PowerPoint/Keynote)
   Scene 2: Training Notebook (Show Colab)
   Scene 3: Android Code Walkthrough (Android Studio)
   Scene 4: App Demo (Emulator or Device)
   Scene 5: Results & Metrics
   ```

### During Demo:

1. **Show Code**:
   - MainActivity.kt → Explain CameraX setup
   - YoloDetector.kt → Explain inference
   - Show TFLite model in assets

2. **Launch App**:
   - Show splash screen
   - Camera permission grant
   - UI explanation

3. **Simulated Detection** (options):
   - Hold printed stair photos to webcam
   - Use tablet with stair images
   - Screen share pre-recorded device footage

4. **Highlight Features**:
   - Bounding boxes
   - Stair type classification
   - Distance estimation
   - Audio feedback
   - Gemini AI guidance

---

## 🛠️ METHOD 4: Modified Demo Mode (Code Change)

### For truly remote demo without physical setup

This adds a "Demo Mode" that cycles through test images instead of using camera.

### Implementation:

1. **Enable Demo Mode** in `DetectionConfig.kt`:
```kotlin
object DetectionConfig {
    const val ENABLE_DEMO_MODE = true  // Change from false
}
```

2. **Add Test Images to Assets**:
```bash
# Copy test images
mkdir -p app/src/main/assets/demo_images
cp ~/Desktop/StairDemo/test_images/*.png app/src/main/assets/demo_images/
```

3. **App Behavior in Demo Mode**:
- Loads images from assets sequentially
- Simulates detection at 3 FPS
- Shows all UI features
- Audio announcements work
- Perfect for presentations

4. **Rebuild and Run**:
```bash
./gradlew assembleDebug
# Launch on emulator
```

---

## 📊 Comparison of Methods

| Method | Setup Time | Reliability | Realism | Best For |
|--------|------------|-------------|---------|----------|
| Pre-recorded Video | 2-3 hours | 100% | High | Final presentation |
| Live Emulator | 30 mins | 60% | Medium | Testing, Q&A |
| Screen Recording | 1 hour | 90% | High | Live demos |
| Demo Mode (modified) | 2 hours | 100% | Medium | Remote defenses |

---

## 🎬 Professional Demo Video Script

### Segment 1: Introduction (0:00-0:30)
```
"Hello, I'm presenting StairVision, an AI-powered stair detection system 
for visual accessibility. This CS663 Mobile Vision project combines 
YOLOv8 object detection with Google Gemini AI to provide real-time 
navigation assistance."
```

### Segment 2: Problem Statement (0:30-1:00)
```
[Show statistics slide]
"1 million stair-related injuries occur annually. Traditional assistive 
technologies like white canes only provide contact-based detection. 
StairVision offers proactive advance warning with contextual guidance."
```

### Segment 3: Training Process (1:00-2:00)
```
[Show Colab notebook]
"I trained a custom YOLOv8 model on 400 annotated stair images using 
Google Colab. After 100 epochs, the model achieved 97% mAP@50. The 
model was then exported to TensorFlow Lite for mobile deployment."
```

### Segment 4: App Demo (2:00-4:00)
```
[Show app running]
"The app provides multi-modal feedback: visual bounding boxes, audio 
announcements, and haptic vibration. Watch as it detects these ascending 
stairs... [point at test image]... The system correctly identifies the 
type, estimates distance, and even suggests which arm to use for the handrail."
```

### Segment 5: Gemini Integration (4:00-5:00)
```
[Show Gemini response]
"After 8 seconds, Google Gemini AI analyzes the scene and provides rich 
contextual guidance: 'I see a well-maintained ascending staircase with 
about 10 to 15 steps. There's a handrail on the right side...' This level 
of detail far exceeds basic object detection."
```

### Segment 6: Results & Conclusion (5:00-6:00)
```
[Show metrics]
"StairVision achieves 91% real-world accuracy at 4-5 FPS on a smartphone. 
With zero additional hardware cost, it democratizes accessibility technology. 
Thank you for watching."
```

---

## 🔧 Emulator Troubleshooting

### Issue: Emulator is Slow
**Solution:**
```bash
# Enable hardware acceleration
# In Android Studio: Tools → AVD Manager → Edit AVD → 
# Graphics: Hardware - GLES 2.0

# Or use command line with GPU
emulator -avd Pixel_6_Pro_API_34 -gpu host
```

### Issue: Camera Not Working
**Solution:**
1. Check Camera Permission in emulator settings
2. Try different camera option (VirtualScene vs Webcam)
3. Restart emulator

### Issue: App Crashes on Emulator
**Solution:**
- Emulator may not have sufficient RAM for TFLite inference
- Increase AVD RAM to 4096 MB minimum
- Reduce confidence threshold to see if TFLite works

### Issue: No Audio from Emulator
**Solution:**
```bash
# Enable audio
emulator -avd Pixel_6_Pro_API_34 -no-audio
# Then test with TTS
```

---

## 📹 Quick Demo Setup (5 Minutes)

For last-minute demo:

1. **Record your screen** (macOS):
   ```
   Cmd + Shift + 5 → Select area → Record
   ```

2. **Hold printed stair photo** to Mac webcam

3. **Launch app on emulator** with webcam as back camera

4. **Narrate live** as detection happens

5. **Stop recording**, upload to YouTube (unlisted)

---

## 🎓 For CS663 Submission

**Include in Submission:**
1. ✅ GitHub repository links (both training & Android)
2. ✅ YouTube demo video (6 minutes)
3. ✅ README with emulator instructions
4. ✅ Screenshots of detection working
5. ✅ Presentation slides (optional)

**Demo Video Checklist:**
- [ ] Title slide with project name
- [ ] Problem statement with statistics
- [ ] Training process (Colab)
- [ ] Code walkthrough (1-2 minutes)
- [ ] Live app demo (2-3 minutes)
- [ ] Results and metrics
- [ ] Conclusion
- [ ] GitHub links in description

---

## 📧 Need Help?

**Resources:**
- Android Emulator Docs: https://developer.android.com/studio/run/emulator
- scrcpy (screen mirror): https://github.com/Genymobile/scrcpy
- OBS Studio: https://obsproject.com/

**Project Repositories:**
- Training: https://github.com/Yesh492/cs663-stair-detection-training
- Android: https://github.com/Yesh492/cs663-stair-detection-android

---

**RECOMMENDATION**: For best results, use **Method 1 (Pre-recorded Video)** with voiceover. This ensures professional quality and eliminates technical issues during presentation.
