package com.example.stairvision

/**
 * EnhancedConfig - Advanced features configuration
 * 
 * New features for production deployment:
 * - Volume key controls
 * - Emergency stop
 * - Detection smoothing
 * - Performance optimization
 * - Enhanced stats
 */
object EnhancedConfig {
    
    // Volume Key Controls
    const val VOLUME_KEY_ENABLED = true
    const val VOLUME_UP_INCREASES_FREQUENCY = true  // true: more frequent, false: less frequent
    const val MIN_ANNOUNCEMENT_INTERVAL_MS = 500L   // Fastest: 0.5 seconds
    const val MAX_ANNOUNCEMENT_INTERVAL_MS = 5000L  // Slowest: 5 seconds
    const val DEFAULT_ANNOUNCEMENT_INTERVAL_MS = 2000L  // Default: 2 seconds
    
    // Emergency Stop
    const val EMERGENCY_STOP_ENABLED = true
    const val EMERGENCY_BUTTON_SIZE_DP = 80  // Large button size
    
    // Detection Smoothing (Temporal Filtering)
    const val DETECTION_SMOOTHING_ENABLED = true
    const val SMOOTHING_WINDOW_SIZE = 5  // Average over last 5 frames
    const val MIN_CONSISTENT_DETECTIONS = 2  // Need 2 out of 5 frames to confirm
    
    // Detection History
    const val SHOW_DETECTION_HISTORY = true
    const val HISTORY_SIZE = 3  // Show last 3 detections
    
    // Performance Optimization
    const val BATTERY_OPTIMIZATION_ENABLED = true
    const val IDLE_FPS_REDUCTION = true  // Reduce FPS when no stairs for 10 seconds
    const val ACTIVE_TARGET_FPS = 5  // Normal FPS
    const val IDLE_TARGET_FPS = 2    // Reduced FPS when idle
    const val IDLE_TIMEOUT_MS = 10000L  // 10 seconds of no detection = idle
    
    // Stats Panel
    const val SHOW_STATS_PANEL = true
    const val STATS_UPDATE_INTERVAL_MS = 1000L  // Update once per second
    
    // Flashlight Support
    const val FLASHLIGHT_ENABLED = true
    const val AUTO_FLASHLIGHT_LOW_LIGHT = false  // Auto-enable in dark
    
    // Screenshot Capture
    const val SCREENSHOT_ENABLED = true
    const val SCREENSHOT_QUALITY = 90  // JPEG quality 0-100
    
    // Quick Threshold Adjustment
    const val QUICK_THRESHOLD_SLIDER = true
    const val THRESHOLD_MIN = 0.3f
    const val THRESHOLD_MAX = 0.95f
    const val THRESHOLD_STEP = 0.05f
    
    // Advanced Audio
    const val AUDIO_DISTANCE_BEEPS = true  // Beep frequency based on distance
    const val AUDIO_PANIC_MODE = true  // Urgent audio when very close
    
    // Vibration Patterns
    const val ENHANCED_VIBRATION = true
    const val VIBRATION_DISTANCE_MODULATION = true  // Vary intensity by distance
}
