package com.example.stairvision

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * AudioFeedbackManager - Comprehensive audio system with TTS and fallback beeps
 * 
 * Features:
 * - TextToSpeech with automatic fallback to tones
 * - Distance-based audio tones (when TTS unavailable)
 * - Continuous narrator mode
 * - Obstacle memory with replay
 * - Volume key interaction
 */
class AudioFeedbackManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioFeedbackManager"
        
        // Audio tone frequencies for distance indication
        private const val TONE_VERY_CLOSE = ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK  // 1000Hz urgent
        private const val TONE_CLOSE = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD         // 800Hz warning
        private const val TONE_MEDIUM = ToneGenerator.TONE_PROP_BEEP                     // 600Hz notice
        private const val TONE_FAR = ToneGenerator.TONE_PROP_ACK                         // 400Hz info
        
        // Timing constants
        private const val CONTINUOUS_NARRATOR_INTERVAL_MS = 2000L  // Every 2 seconds
        private const val MIN_ANNOUNCEMENT_INTERVAL_MS = 1500L     // Prevent spam
        private const val OBSTACLE_MEMORY_SIZE = 5
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var toneGenerator: ToneGenerator? = null
    
    // State tracking
    private var lastAnnouncementTime = 0L
    private var lastStairDetectionTime = 0L
    private var continuousNarratorEnabled = true
    private var useFallbackTones = false
    
    // Obstacle memory
    private val obstacleHistory = mutableListOf<DetectionEvent>()
    
    data class DetectionEvent(
        val timestamp: Long,
        val stairType: String,
        val distance: Float,
        val confidence: Float
    )

    init {
        initializeAudioSystems()
    }

    private fun initializeAudioSystems() {
        // Initialize TTS
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language not supported, using fallback tones")
                    useFallbackTones = true
                    isTtsReady = false
                } else {
                    isTtsReady = true
                    useFallbackTones = false
                    Log.d(TAG, "✅ TTS initialized successfully")
                }
            } else {
                Log.e(TAG, "TTS initialization failed, using fallback tones")
                useFallbackTones = true
                isTtsReady = false
            }
        }

        // Initialize tone generator for fallback
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            Log.d(TAG, "✅ Tone generator initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize tone generator", e)
        }
    }

    /**
     * Main announcement method with automatic fallback
     */
    fun announceDetection(detections: List<YoloDetector.Detection>) {
        val currentTime = System.currentTimeMillis()
        
        if (detections.isNotEmpty()) {
            // Update obstacle history
            val primary = detections.maxByOrNull { 
                (5f - it.getDistanceMeters()) + it.score 
            } ?: detections.first()
            
            addToObstacleHistory(primary)
            lastStairDetectionTime = currentTime
            
            // Check if enough time has passed since last announcement
            if (currentTime - lastAnnouncementTime >= MIN_ANNOUNCEMENT_INTERVAL_MS) {
                announceStairs(primary, detections.size)
                lastAnnouncementTime = currentTime
            }
        } else if (continuousNarratorEnabled) {
            // Continuous narrator: announce clear path periodically
            if (currentTime - lastAnnouncementTime >= CONTINUOUS_NARRATOR_INTERVAL_MS) {
                announceClearPath()
                lastAnnouncementTime = currentTime
            }
        }
    }

    /**
     * Announce stairs detected
     */
    private fun announceStairs(detection: YoloDetector.Detection, count: Int) {
        val stairType = detection.getTypeDescription()
        val distance = detection.getDistanceMeters().toInt()
        val urgency = when (detection.distance) {
            YoloDetector.Distance.VERY_CLOSE -> "Stop! "
            YoloDetector.Distance.CLOSE -> "Caution! "
            else -> ""
        }
        
        val message = "$urgency$stairType detected, $distance meters ahead"
        
        if (isTtsReady && !useFallbackTones) {
            speak(message)
        } else {
            // Fallback: Play distance-appropriate tone
            playToneForDistance(detection.distance)
        }
    }

    /**
     * Announce clear path
     */
    private fun announceClearPath() {
        val timeSinceLastStair = System.currentTimeMillis() - lastStairDetectionTime
        val message = if (timeSinceLastStair < 10000) {
            "Path clear, continue forward"
        } else {
            "Scanning for obstacles, path clear"
        }
        
        if (isTtsReady && !useFallbackTones) {
            speak(message)
        } else {
            // Single low tone for "clear"
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        }
    }

    /**
     * Play distance-based audio tone
     */
    private fun playToneForDistance(distance: YoloDetector.Distance) {
        val (toneType, duration) = when (distance) {
            YoloDetector.Distance.VERY_CLOSE -> Pair(TONE_VERY_CLOSE, 500)
            YoloDetector.Distance.CLOSE -> Pair(TONE_CLOSE, 300)
            YoloDetector.Distance.MEDIUM -> Pair(TONE_MEDIUM, 200)
            YoloDetector.Distance.FAR -> Pair(TONE_FAR, 150)
        }
        
        try {
            toneGenerator?.startTone(toneType, duration)
            Log.d(TAG, "Played tone for distance: $distance")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play tone", e)
        }
    }

    /**
     * Speak text using TTS
     */
    private fun speak(message: String) {
        textToSpeech?.stop()
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "StairVisionAudio")
        Log.d(TAG, "Speaking: $message")
    }

    /**
     * Add detection to obstacle history
     */
    private fun addToObstacleHistory(detection: YoloDetector.Detection) {
        val event = DetectionEvent(
            timestamp = System.currentTimeMillis(),
            stairType = detection.getTypeDescription(),
            distance = detection.getDistanceMeters(),
            confidence = detection.score
        )
        
        obstacleHistory.add(0, event)  // Add to front
        if (obstacleHistory.size > OBSTACLE_MEMORY_SIZE) {
            obstacleHistory.removeAt(obstacleHistory.size - 1)
        }
    }

    /**
     * Replay last obstacle detection
     */
    fun replayLastObstacle() {
        if (obstacleHistory.isEmpty()) {
            speak("No recent obstacles detected")
            return
        }
        
        val last = obstacleHistory.first()
        val secondsAgo = (System.currentTimeMillis() - last.timestamp) / 1000
        val message = "Last detection: ${last.stairType}, ${last.distance.toInt()} meters, " +
                     "$secondsAgo seconds ago"
        
        if (isTtsReady && !useFallbackTones) {
            speak(message)
        } else {
            // Play quick beep pattern for replay
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        }
    }

    /**
     * Toggle continuous narrator
     */
    fun setContinuousNarrator(enabled: Boolean) {
        continuousNarratorEnabled = enabled
        Log.d(TAG, "Continuous narrator: ${if (enabled) "ON" else "OFF"}")
    }

    /**
     * Force speak now (for testing)
     */
    fun speakNow(message: String) {
        if (isTtsReady) {
            speak(message)
        } else {
            Log.w(TAG, "TTS not ready, playing tone instead")
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        }
    }

    /**
     * Check if TTS is ready
     */
    fun isReady(): Boolean = isTtsReady

    /**
     * Check if using fallback tones
     */
    fun isUsingFallback(): Boolean = useFallbackTones

    /**
     * Clean up resources
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        toneGenerator?.release()
        textToSpeech = null
        toneGenerator = null
        Log.d(TAG, "AudioFeedbackManager shutdown")
    }
}
