package com.example.stairvision

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * GeminiAudioAssistant - Provides audio navigation feedback for low-vision users
 * 
 * This class integrates:
 * 1. Gemini AI for generating contextual navigation warnings
 * 2. Android TextToSpeech for audio output
 * 3. Rate limiting to prevent audio spam
 * 
 * Usage:
 *   val assistant = GeminiAudioAssistant(context)
 *   assistant.speakIfNeeded(detections)
 */
class GeminiAudioAssistant(private val context: Context) {

    companion object {
        private const val TAG = "GeminiAudioAssistant"
        
        // Rate limiting: minimum time between announcements (milliseconds)
        private const val MIN_ANNOUNCEMENT_INTERVAL_MS = 3000L  // 3 seconds
        private const val CLEAR_PATH_INTERVAL_MS = 7000L        // 7 seconds for "clear" messages
        
        // Gemini configuration
        // TODO: Replace with your actual Gemini API key
        private const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
        private const val USE_MOCK_GEMINI = true  // Set to false when real API key is added
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    
    // Tracking state
    private var lastAnnouncementTime = 0L
    private var lastClearPathTime = 0L
    private var lastDetectionState = false  // true if stairs were detected last time
    
    init {
        initializeTextToSpeech()
    }

    /**
     * Initialize Android TextToSpeech engine
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported for TTS")
                } else {
                    isTtsInitialized = true
                    Log.d(TAG, "TextToSpeech initialized successfully")
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed")
            }
        }
    }

    /**
     * Main entry point - determine if we should speak and what to say
     * 
     * @param detections List of detected stairs from YOLO
     */
    fun speakIfNeeded(detections: List<YoloDetector.Detection>) {
        if (!isTtsInitialized) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val stairsDetected = detections.isNotEmpty()

        // Determine if we should announce based on rate limiting and state changes
        val shouldAnnounce = when {
            // Stairs detected: announce if enough time has passed OR state changed from clear to detected
            stairsDetected -> {
                val timeSinceLastAnnouncement = currentTime - lastAnnouncementTime
                val stateChanged = !lastDetectionState
                (timeSinceLastAnnouncement >= MIN_ANNOUNCEMENT_INTERVAL_MS) || stateChanged
            }
            
            // Clear path: only announce periodically, not continuously
            else -> {
                val timeSinceLastClear = currentTime - lastClearPathTime
                timeSinceLastClear >= CLEAR_PATH_INTERVAL_MS
            }
        }

        if (shouldAnnounce) {
            val message = if (stairsDetected) {
                generateStairWarning(detections)
            } else {
                generateClearPathMessage()
            }
            
            speak(message)
            
            // Update timing and state
            if (stairsDetected) {
                lastAnnouncementTime = currentTime
            } else {
                lastClearPathTime = currentTime
            }
            lastDetectionState = stairsDetected
        }
    }

    /**
     * Generate warning message for detected stairs using Gemini (or mock)
     * 
     * @param detections List of detected stairs
     * @return Warning message to speak
     */
    private fun generateStairWarning(detections: List<YoloDetector.Detection>): String {
        val detectionCount = detections.size
        val avgConfidence = detections.map { it.score }.average()
        
        // Get primary detection (closest/most confident)
        val primaryDetection = detections.maxByOrNull { 
            // Prioritize closer stairs and higher confidence
            (5f - it.getDistanceMeters()) + it.score 
        } ?: detections.first()
        
        return if (USE_MOCK_GEMINI) {
            // Mock Gemini responses for demo
            generateMockStairWarning(primaryDetection, detectionCount, avgConfidence)
        } else {
            // Real Gemini API call
            generateGeminiStairWarning(primaryDetection, detectionCount, avgConfidence)
        }
    }

    /**
     * Mock Gemini responses for demo/testing with detailed stair information
     * In production, this would be replaced with actual Gemini API calls
     */
    private fun generateMockStairWarning(
        detection: YoloDetector.Detection,
        count: Int,
        confidence: Double
    ): String {
        val stairType = detection.getTypeDescription()
        val distance = detection.getDistanceDescription()
        val distanceMeters = detection.getDistanceMeters()
        
        // Generate contextual warning based on urgency
        val urgencyLevel = when (detection.distance) {
            YoloDetector.Distance.VERY_CLOSE -> "urgent"
            YoloDetector.Distance.CLOSE -> "warning"
            else -> "notice"
        }
        
        // Create detailed, natural-sounding warnings
        val detailedResponses = when (urgencyLevel) {
            "urgent" -> listOf(
                "Stop! $stairType immediately ahead!",
                "Caution! $stairType less than 1 meter ahead!",
                "Warning! $stairType right in front of you!",
                "Stop now! $stairType extremely close!"
            )
            "warning" -> listOf(
                "$stairType ${distanceMeters.toInt()} meters ahead, proceed carefully.",
                "Caution, $stairType $distance.",
                "$stairType detected $distance, watch your step.",
                "Be careful, $stairType approximately ${distanceMeters.toInt()} meters ahead."
            )
            else -> listOf(
                "$stairType detected $distance.",
                "$stairType visible ahead at ${distanceMeters.toInt()} meters.",
                "Notice: $stairType $distance."
            )
        }
        
        return detailedResponses.random()
    }

    /**
     * Call Gemini API to generate contextual stair warning with detailed information
     * 
     * TODO: Implement actual Gemini API integration
     * 
     * Implementation notes:
     * 1. Add Gemini SDK dependency to build.gradle:
     *    implementation 'com.google.ai.client.generativeai:generativeai:0.1.2'
     * 
     * 2. Initialize Gemini client:
     *    val generativeModel = GenerativeModel(
     *        modelName = "gemini-pro",
     *        apiKey = GEMINI_API_KEY
     *    )
     * 
     * 3. Create prompt and generate:
     *    val prompt = """
     *        Give a short spoken navigation warning for a blind user.
     *        Context: 
     *        - Stair type: ${detection.getTypeDescription()}
     *        - Distance: ${detection.getDistanceDescription()}
     *        - Confidence: ${(confidence * 100).toInt()}%
     *        Requirements:
     *        - Safety-focused
     *        - Include stair type and distance
     *        - 10 words or less
     *        - No emojis
     *        - Clear and direct
     *    """.trimIndent()
     *    val response = generativeModel.generateContent(prompt)
     *    return response.text ?: "Stairs detected ahead"
     */
    private fun generateGeminiStairWarning(
        detection: YoloDetector.Detection,
        count: Int,
        confidence: Double
    ): String {
        Log.w(TAG, "Real Gemini API not implemented yet, using fallback")
        
        // TODO: Replace this with actual Gemini API call
        // For now, return a reasonable fallback with detail
        val stairType = detection.getTypeDescription()
        val distance = detection.getDistanceDescription()
        
        return when (detection.distance) {
            YoloDetector.Distance.VERY_CLOSE -> "Stop! $stairType immediately ahead!"
            YoloDetector.Distance.CLOSE -> "$stairType detected $distance, proceed carefully."
            else -> "$stairType $distance."
        }
    }

    /**
     * Generate clear path message using Gemini (or mock)
     */
    private fun generateClearPathMessage(): String {
        return if (USE_MOCK_GEMINI) {
            // Mock responses
            listOf(
                "Path looks clear.",
                "No obstacles detected.",
                "Clear ahead.",
                "Way is clear."
            ).random()
        } else {
            // Real Gemini call
            generateGeminiClearMessage()
        }
    }

    /**
     * Call Gemini for clear path message
     * 
     * TODO: Implement actual Gemini API integration
     */
    private fun generateGeminiClearMessage(): String {
        Log.w(TAG, "Real Gemini API not implemented yet, using fallback")
        return "Path is clear."
    }

    /**
     * Speak the message using TextToSpeech
     */
    private fun speak(message: String) {
        if (!isTtsInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak: $message")
            return
        }

        Log.d(TAG, "Speaking: $message")
        
        // Stop any ongoing speech
        textToSpeech?.stop()
        
        // Speak with high priority (QUEUE_FLUSH replaces any pending speech)
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "StairVisionAudio")
    }

    /**
     * Enable or disable audio announcements
     */
    fun setEnabled(enabled: Boolean) {
        if (!enabled) {
            textToSpeech?.stop()
        }
        Log.d(TAG, "Audio assistant ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if TTS is ready
     */
    fun isReady(): Boolean = isTtsInitialized

    /**
     * Clean up resources when done
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsInitialized = false
        Log.d(TAG, "GeminiAudioAssistant shutdown")
    }

    /**
     * Immediately speak a custom message (for testing)
     */
    fun speakNow(message: String) {
        speak(message)
    }
}
