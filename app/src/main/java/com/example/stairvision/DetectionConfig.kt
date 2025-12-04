package com.example.stairvision

/**
 * DetectionConfig - Centralized configuration for stair detection
 * 
 * Adjust these values to tune the detection behavior:
 * - Lower threshold = more detections but more false positives
 * - Higher threshold = fewer false positives but might miss some stairs
 * 
 * For testing different scenarios:
 * - Indoor low-light: Try 0.3-0.4
 * - Well-lit stairs: Try 0.5-0.6
 * - Strict mode (demo): Try 0.6-0.7
 */
object DetectionConfig {
    
    /**
     * Confidence threshold for valid detections
     * Range: 0.0 to 1.0
     * Recommended: 0.85 for STRICT detection (minimal false positives)
     * 
     * INCREASED TO 0.85 - Drastically reduces false positives
     */
    const val CONFIDENCE_THRESHOLD = 0.85f
    
    /**
     * Enable/disable bounding box overlay
     */
    const val SHOW_BOUNDING_BOXES = true
    
    /**
     * Enable/disable FPS display in status text
     */
    const val SHOW_FPS = true
    
    /**
     * Enable/disable detailed logging
     * Set to true for debugging, false for production
     */
    const val ENABLE_DEBUG_LOGGING = true
    
    /**
     * Enable/disable audio navigation assistance
     * When enabled, the app will speak warnings about detected stairs
     */
    const val ENABLE_AUDIO_FEEDBACK = true
    
    /**
     * DEMO MODE - Enable mock detections for testing when model doesn't detect
     * Set to true to simulate detections for demonstration purposes
     * Set to false for real model inference
     */
    const val ENABLE_DEMO_MODE = false
    
    /**
     * Minimum time between audio announcements (seconds)
     * Prevents audio spam
     */
    const val AUDIO_ANNOUNCEMENT_INTERVAL_SECONDS = 3
    
    /**
     * Time between "clear path" announcements (seconds)
     * Only speaks when no stairs detected
     */
    const val CLEAR_PATH_INTERVAL_SECONDS = 7
    
    /**
     * Camera rotation adjustment (degrees)
     * Common values: 0, 90, 180, 270
     * Adjust if preview appears rotated
     */
    const val CAMERA_ROTATION = 90
    
    // Preset configurations for quick testing
    object Presets {
        const val STRICT_MODE = 0.85f       // Minimal false positives (CURRENT)
        const val BALANCED_MODE = 0.6f      // Good balance
        const val SENSITIVE_MODE = 0.3f     // Catch more stairs, more false positives
        const val CUSTOM_MODE = CONFIDENCE_THRESHOLD
    }
}
