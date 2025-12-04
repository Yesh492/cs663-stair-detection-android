package com.example.stairvision

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * HapticFeedbackManager - Provides vibration patterns for navigation guidance
 * 
 * Features:
 * - Distance-based alert patterns (closer = more intense)
 * - Lateral guidance (directional vibration - left/right side stronger)
 * - Configurable patterns and intensities
 * 
 * Usage:
 *   val haptic = HapticFeedbackManager(context)
 *   haptic.alertForDistance(YoloDetector.Distance.CLOSE)
 *   haptic.lateralGuidance(isOffCenter = true, centerRight = false)
 */
class HapticFeedbackManager(private val context: Context) {

    companion object {
        private const val TAG = "HapticFeedbackManager"
        
        // Vibration patterns (timings in milliseconds: [delay, vibrate, pause, vibrate, ...])
        private val PATTERN_VERY_CLOSE = longArrayOf(0, 100, 50, 100, 50, 100) // Continuous
        private val PATTERN_CLOSE = longArrayOf(0, 200, 100, 200) // Repeated thump
        private val PATTERN_MEDIUM = longArrayOf(0, 150, 300, 150) // Moderate pulse
        private val PATTERN_FAR = longArrayOf(0, 100, 500) // Single pulse
        
        // Lateral guidance pattern (for directional hints)
        private val PATTERN_LATERAL = longArrayOf(0, 50, 100, 50, 100, 50)
        
        // Intensities (0-255)
        private const val INTENSITY_HIGH = 255
        private const val INTENSITY_MEDIUM = 180
        private const val INTENSITY_LOW = 100
    }

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var isEnabled = true
    private var lastVibrationTime = 0L
    private val MIN_VIBRATION_INTERVAL_MS = 500L // Prevent vibration spam

    /**
     * Alert user based on detected stair distance
     */
    fun alertForDistance(distance: YoloDetector.Distance) {
        if (!isEnabled || !canVibrate()) return

        val (pattern, intensities) = when (distance) {
            YoloDetector.Distance.VERY_CLOSE -> {
                Pair(PATTERN_VERY_CLOSE, intArrayOf(0, INTENSITY_HIGH, 0, INTENSITY_HIGH, 0, INTENSITY_HIGH))
            }
            YoloDetector.Distance.CLOSE -> {
                Pair(PATTERN_CLOSE, intArrayOf(0, INTENSITY_MEDIUM, 0, INTENSITY_MEDIUM))
            }
            YoloDetector.Distance.MEDIUM -> {
                Pair(PATTERN_MEDIUM, intArrayOf(0, INTENSITY_LOW, 0, INTENSITY_LOW))
            }
            YoloDetector.Distance.FAR -> {
                Pair(PATTERN_FAR, intArrayOf(0, INTENSITY_LOW))
            }
        }

        vibrateWithPattern(pattern, intensities)
        Log.d(TAG, "Alert vibration for distance: $distance")
    }

    /**
     * Provide lateral guidance vibration (helps user center on stairs)
     * 
     * @param isOffCenter True if stairs are off-center
     * @param centerRight True if user needs to move right, false for left
     */
    fun lateralGuidance(isOffCenter: Boolean, centerRight: Boolean) {
        if (!isEnabled || !canVibrate() || !isOffCenter) return

        // Create asymmetric pattern based on direction
        val direction = if (centerRight) "right" else "left"
        
        // Simple pulse pattern - in full implementation, could vary intensity by side
        val intensities = intArrayOf(0, INTENSITY_LOW, 0, INTENSITY_LOW, 0, INTENSITY_LOW)
        
        vibrateWithPattern(PATTERN_LATERAL, intensities)
        Log.d(TAG, "Lateral guidance vibration: move $direction")
    }

    /**
     * Single pulse (for button presses, confirmations)
     */
    fun singlePulse(intensity: Int = INTENSITY_MEDIUM) {
        if (!isEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(100, intensity)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Execute vibration with pattern and varying intensities
     */
    private fun vibrateWithPattern(pattern: LongArray, intensities: IntArray) {
        if (!canVibrate()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, intensities, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            lastVibrationTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    /**
     * Check if enough time has passed since last vibration
     */
    private fun canVibrate(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastVibration = currentTime - lastVibrationTime
        return timeSinceLastVibration >= MIN_VIBRATION_INTERVAL_MS
    }

    /**
     * Enable or disable haptic feedback
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            vibrator.cancel()
        }
        Log.d(TAG, "Haptic feedback ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if haptic feedback is enabled
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Stop all vibrations
     */
    fun stop() {
        vibrator.cancel()
    }

    /**
     * Clean up
     */
    fun shutdown() {
        stop()
        Log.d(TAG, "Haptic feedback manager shutdown")
    }
}
