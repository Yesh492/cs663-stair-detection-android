package com.example.stairvision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

/**
 * MainActivity - Professional stair detection with comprehensive safety features
 * 
 * Features:
 * - Real-time stair detection with YOLO
 * - Professional visual overlay with guidance lines and arrows
 * - Haptic feedback for distance alerts
 * - Audio navigation assistance
 * - HAZARD DETECTED banner
 * - Configurable settings
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // UI elements - Camera and overlay
    private lateinit var previewView: PreviewView
    private lateinit var overlay: OverlayView
    
    // UI elements - Professional interface
    private lateinit var hazardBanner: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var systemStatus: TextView
    private lateinit var stairIcon: TextView
    private lateinit var stairType: TextView
    private lateinit var distanceText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var stairDetails: TextView
    private lateinit var fpsCounter: TextView
    private lateinit var firstStepText: TextView
    private lateinit var lateralGuidance: TextView
    
    // Enhanced UI elements
    private lateinit var emergencyStopButton: Button
    private lateinit var detectionHistoryText: TextView
    private lateinit var statsPanel: TextView

    // Detection and assistance
    private lateinit var yoloDetector: YoloDetector
    private lateinit var audioAssistant: GeminiAudioAssistant
    private lateinit var hapticFeedback: HapticFeedbackManager
    private lateinit var geminiVision: GeminiVisionAssistant  // 🤖 Gemini AI integration
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    // Store current frame for Gemini analysis
    private var currentFrameBitmap: Bitmap? = null

    // Performance tracking
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0f
    
    // User preferences
    private var hapticEnabled = true
    private var lateralGuidanceEnabled = true
    private var confidenceThreshold = 0.6f
    
    // Enhanced features state
    private var detectionPaused = false
    private val detectionHistory = mutableListOf<List<YoloDetector.Detection>>()
    private val recentDetections = mutableListOf<Pair<Long, String>>()
    private var totalDetections = 0
    private var lastStatsUpdate = 0L
    private var lastDetectionTime = System.currentTimeMillis()
    private var isIdleMode = false
    private var frameSkipCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "StairVision Professional Edition starting...")

        // Initialize UI elements
        initializeUI()

        // Initialize detection and assistance systems
        yoloDetector = YoloDetector(this)
        audioAssistant = GeminiAudioAssistant(this)
        hapticFeedback = HapticFeedbackManager(this)
        geminiVision = GeminiVisionAssistant(this, audioAssistant)  // 🤖 Initialize Gemini AI

        // Load user preferences
        loadUserPreferences()

        // Configure systems based on preferences
        audioAssistant.setEnabled(DetectionConfig.ENABLE_AUDIO_FEEDBACK)
        hapticFeedback.setEnabled(hapticEnabled)

        // TEST TTS ON STARTUP - Wait 3 seconds then test
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (audioAssistant.isReady()) {
                audioAssistant.speakNow("StairVision audio system ready")
                Log.d(TAG, "✅ TTS test executed successfully")
            } else {
                Log.e(TAG, "❌ TTS NOT READY - Check if Google TTS is installed")
                runOnUiThread {
                    stairType.text = "⚠️ TTS Not Available - Install Google Text-to-Speech"
                }
            }
        }, 3000)

        // Setup settings button
        settingsButton.setOnClickListener {
            hapticFeedback.singlePulse()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Setup emergency stop button
        emergencyStopButton.setOnClickListener {
            detectionPaused = !detectionPaused
            
            if (detectionPaused) {
                emergencyStopButton.text = "START"
                emergencyStopButton.setBackgroundColor(getColor(android.R.color.holo_green_dark))
                audioAssistant.speakNow("Detection paused")
                stairType.text = "DETECTION PAUSED"
                hazardBanner.visibility = View.GONE
            } else {
                emergencyStopButton.text = "STOP"
                emergencyStopButton.setBackgroundColor(getColor(android.R.color.holo_red_dark))
                audioAssistant.speakNow("Detection resumed")
                stairType.text = "Scanning..."
            }
            
            hapticFeedback.singlePulse()
        }

        // Request camera permission
        requestCameraPermission()
    }

    private fun initializeUI() {
        // Camera and overlay
        previewView = findViewById(R.id.cameraPreview)
        overlay = findViewById(R.id.overlay)
        
        // Professional interface elements
        hazardBanner = findViewById(R.id.hazardBanner)
        settingsButton = findViewById(R.id.settingsButton)
        systemStatus = findViewById(R.id.systemStatus)
        stairIcon = findViewById(R.id.stairIcon)
        stairType = findViewById(R.id.stairType)
        distanceText = findViewById(R.id.distanceText)
        confidenceText = findViewById(R.id.confidenceText)
        stairDetails = findViewById(R.id.stairDetails)
        fpsCounter = findViewById(R.id.fpsCounter)
        firstStepText = findViewById(R.id.firstStepText)
        lateralGuidance = findViewById(R.id.lateralGuidance)
        
        // Enhanced UI elements
        emergencyStopButton = findViewById(R.id.emergencyStopButton)
        detectionHistoryText = findViewById(R.id.detectionHistory)
        statsPanel = findViewById(R.id.statsPanel)
    }

    private fun loadUserPreferences() {
        val prefs = getSharedPreferences("StairVisionSettings", MODE_PRIVATE)
        hapticEnabled = prefs.getBoolean("haptic_enabled", true)
        lateralGuidanceEnabled = prefs.getBoolean("lateral_guidance_enabled", true)
        confidenceThreshold = prefs.getFloat("confidence_threshold", 0.6f)
        
        Log.d(TAG, "Preferences loaded: haptic=$hapticEnabled, lateral=$lateralGuidanceEnabled, threshold=$confidenceThreshold")
    }

    override fun onResume() {
        super.onResume()
        // Reload preferences in case they changed in settings
        loadUserPreferences()
        hapticFeedback.setEnabled(hapticEnabled)
    }

    private fun requestCameraPermission() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.d(TAG, "Camera permission granted")
                startCamera()
            } else {
                Log.e(TAG, "Camera permission denied")
                stairType.text = "Camera permission required"
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(YoloDetector.INPUT_SIZE, YoloDetector.INPUT_SIZE))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processFrame(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

                Log.d(TAG, "Camera started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                runOnUiThread {
                    stairType.text = "Camera error: ${e.message}"
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Check if detection is paused
            if (detectionPaused) {
                imageProxy.close()
                return
            }
            
            updateFps()

            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap == null) {
                Log.w(TAG, "Failed to convert ImageProxy to Bitmap")
                return
            }

            val inputBuffer = preprocessImage(bitmap)
            var detections = yoloDetector.detect(inputBuffer, confidenceThreshold)
            
            // Demo mode if enabled
            if (DetectionConfig.ENABLE_DEMO_MODE && detections.isEmpty()) {
                detections = generateDemoDetections()
            }
            
            // Store current frame for Gemini analysis
            currentFrameBitmap = bitmap

            runOnUiThread {
                updateProfessionalUI(detections)
                
                // 🤖 GEMINI AI: Analyze scene with intelligent guidance
                if (detections.isNotEmpty()) {
                    geminiVision.analyzeScene(bitmap, detections)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun updateProfessionalUI(detections: List<YoloDetector.Detection>) {
        // Update overlay with visual guides
        if (DetectionConfig.SHOW_BOUNDING_BOXES) {
            overlay.setDetections(detections)
        } else {
            overlay.clear()
        }

        if (detections.isEmpty()) {
            showSafeState()
        } else {
            showHazardState(detections)
        }

        // Update FPS counter
        if (DetectionConfig.SHOW_FPS) {
            fpsCounter.text = "FPS: ${currentFps.toInt()}"
        }

        // Provide audio and haptic feedback
        if (DetectionConfig.ENABLE_AUDIO_FEEDBACK) {
            audioAssistant.speakIfNeeded(detections)
        }

        if (hapticEnabled && detections.isNotEmpty()) {
            val primaryDetection = detections.maxByOrNull { 
                (5f - it.getDistanceMeters()) + it.score 
            }
            primaryDetection?.let {
                hapticFeedback.alertForDistance(it.distance)
            }
        }
    }

    private fun showSafeState() {
        // Hide hazard banner
        hazardBanner.visibility = View.GONE
        
        // Update icon and text to indicate safe state
        stairIcon.text = "✅"
        stairType.text = "NO HAZARDS DETECTED"
        distanceText.text = "Path clear"
        confidenceText.text = "Keep scanning..."
        stairDetails.text = "Searching for stairs"
        
        // Hide optional info
        firstStepText.visibility = View.GONE
        lateralGuidance.visibility = View.GONE
    }

    private fun showHazardState(detections: List<YoloDetector.Detection>) {
        // Show red hazard banner
        hazardBanner.visibility = View.VISIBLE
        
        // Get primary (closest/most confident) detection
        val primary = detections.maxByOrNull { 
            (5f - it.getDistanceMeters()) + it.score 
        } ?: detections.first()

        // ENHANCED: Show type with icon and explicit type name
        val typeIcon = getStairTypeIcon(primary.stairType)
        val typeDesc = primary.getTypeDescription().uppercase()
        val count = if (detections.size > 1) " (${detections.size})" else ""
        stairType.text = "$typeIcon $typeDesc$count"
        
        // Update icon based on urgency
        stairIcon.text = when (primary.distance) {
            YoloDetector.Distance.VERY_CLOSE -> "🚨"
            YoloDetector.Distance.CLOSE -> "⚠️"
            YoloDetector.Distance.MEDIUM -> "⚡"
            YoloDetector.Distance.FAR -> "👁️"
        }

        // Update distance and confidence
        val distMeters = primary.getDistanceMeters().toInt()
        distanceText.text = "${distMeters}m away"
        confidenceText.text = "Confidence: ${(primary.score * 100).toInt()}%"

        // ENHANCED: Show step count estimation
        val stepCount = estimateStepCount(primary.h)
        val typeDisplay = primary.getTypeDescription()
        val confDisplay = "${(primary.score * 100).toInt()}%"
        stairDetails.text = "$typeDisplay | $stepCount | Conf: $confDisplay"

        // ENHANCED: Dynamic first step distance based on actual detection
        if (primary.distance == YoloDetector.Distance.VERY_CLOSE || 
            primary.distance == YoloDetector.Distance.CLOSE) {
            firstStepText.visibility = View.VISIBLE
            val firstStepDist = getFirstStepDistance(primary.distance)
            firstStepText.text = "First step: $firstStepDist"
        } else {
            firstStepText.visibility = View.GONE
        }

        // ENHANCED: Explicit handrail guidance for arm support
        if (lateralGuidanceEnabled) {
            val centerOffset = kotlin.math.abs(primary.x - 0.5f)
            // Show guidance if off-center OR when very close (always helpful then)
            if (centerOffset > 0.15f || primary.distance == YoloDetector.Distance.VERY_CLOSE) {
                lateralGuidance.visibility = View.VISIBLE
                lateralGuidance.text = getHandrailGuidance(primary.x)
                
                // Trigger lateral haptic guidance
                if (hapticEnabled) {
                    hapticFeedback.lateralGuidance(true, primary.x < 0.5f)
                }
            } else {
                lateralGuidance.visibility = View.GONE
            }
        } else {
            lateralGuidance.visibility = View.GONE
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(
            bitmap, 
            YoloDetector.INPUT_SIZE, 
            YoloDetector.INPUT_SIZE, 
            true
        )

        val rotated = rotateBitmapIfNeeded(resized)

        val inputBuffer = ByteBuffer.allocateDirect(
            1 * YoloDetector.INPUT_SIZE * YoloDetector.INPUT_SIZE * 3 * 4
        )
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(YoloDetector.INPUT_SIZE * YoloDetector.INPUT_SIZE)
        rotated.getPixels(pixels, 0, YoloDetector.INPUT_SIZE, 0, 0, 
                         YoloDetector.INPUT_SIZE, YoloDetector.INPUT_SIZE)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(DetectionConfig.CAMERA_ROTATION.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun updateFps() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= 1000) {
            currentFps = frameCount * 1000f / elapsed
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
    
    /**
     * Estimate number of steps based on height in frame
     */
    private fun estimateStepCount(height: Float): String {
        return when {
            height > 0.5f -> "15-20 steps"
            height > 0.3f -> "10-15 steps"
            height > 0.2f -> "5-10 steps"
            else -> "3-5 steps"
        }
    }
    
    /**
     * Get explicit first step distance
     */
    private fun getFirstStepDistance(distance: YoloDetector.Distance): String {
        return when (distance) {
            YoloDetector.Distance.VERY_CLOSE -> "0.5-1m"
            YoloDetector.Distance.CLOSE -> "1-2m"
            YoloDetector.Distance.MEDIUM -> "2-3m"
            YoloDetector.Distance.FAR -> "3-5m"
        }
    }
    
    /**
     * Get handrail guidance for arm support
     */
    private fun getHandrailGuidance(x: Float): String {
        return when {
            x < 0.35f -> "⬅️ HANDRAIL ON RIGHT\nUse right arm for support"
            x > 0.65f -> "➡️ HANDRAIL ON LEFT\nUse left arm for support"
            else -> "✅ CENTERED\nHandrails available both sides"
        }
    }
    
    /**
     * Get stair type icon
     */
    private fun getStairTypeIcon(type: YoloDetector.StairType): String {
        return when (type) {
            YoloDetector.StairType.ASCENDING -> "⬆️"
            YoloDetector.StairType.DESCENDING -> "⬇️"
            YoloDetector.StairType.SIDE_VIEW -> "↔️"
            YoloDetector.StairType.SPIRAL -> "🔄"
            YoloDetector.StairType.UNKNOWN -> "❓"
        }
    }

    // Demo mode detection generation
    private var demoModeFrame = 0
    private var demoDistance = YoloDetector.Distance.FAR

    private fun generateDemoDetections(): List<YoloDetector.Detection> {
        demoModeFrame++
        
        if (demoModeFrame % 50 == 0) {
            demoDistance = when (demoDistance) {
                YoloDetector.Distance.FAR -> YoloDetector.Distance.MEDIUM
                YoloDetector.Distance.MEDIUM -> YoloDetector.Distance.CLOSE
                YoloDetector.Distance.CLOSE -> YoloDetector.Distance.VERY_CLOSE
                YoloDetector.Distance.VERY_CLOSE -> YoloDetector.Distance.FAR
            }
        }
        
        val stairType = when (demoModeFrame / 100 % 3) {
            0 -> YoloDetector.StairType.DESCENDING
            1 -> YoloDetector.StairType.ASCENDING
            else -> YoloDetector.StairType.SIDE_VIEW
        }
        
        val (x, y, w, h) = when (demoDistance) {
            YoloDetector.Distance.VERY_CLOSE -> floatArrayOf(0.5f, 0.7f, 0.6f, 0.5f)
            YoloDetector.Distance.CLOSE -> floatArrayOf(0.5f, 0.6f, 0.4f, 0.35f)
            YoloDetector.Distance.MEDIUM -> floatArrayOf(0.5f, 0.5f, 0.3f, 0.25f)
            YoloDetector.Distance.FAR -> floatArrayOf(0.5f, 0.4f, 0.2f, 0.15f)
        }
        
        return listOf(YoloDetector.Detection(x, y, w, h, 0.85f, stairType, demoDistance))
    }

    override fun onDestroy() {
        super.onDestroy()
        yoloDetector.close()
        audioAssistant.shutdown()
        hapticFeedback.shutdown()
        cameraExecutor.shutdown()
        Log.d(TAG, "StairVision Professional Edition closed")
    }
}
