package com.example.stairvision

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * YoloDetector - Handles TensorFlow Lite model inference for stair detection
 * 
 * Model Details:
 * - Input: [1, 640, 640, 3] float32, RGB normalized to [0, 1]
 * - Output: [1, 5, 8400] where 5 = [x, y, w, h, confidence] for each of 8400 anchors
 * - Single class: stairs
 * - Coordinates are in normalized [0, 1] space (center x, center y, width, height)
 */
class YoloDetector(private val context: Context) {

    // Configuration constants
    companion object {
        private const val TAG = "YoloDetector"
        private const val MODEL_FILE = "stair_yolo_best_float32.tflite"
        const val INPUT_SIZE = 640
        
        // Threshold for considering a detection valid
        // 0.5 is a good balance - lower = more detections but more false positives
        // Adjust between 0.3-0.7 based on your needs
        const val CONFIDENCE_THRESHOLD = 0.5f
    }

    /**
     * Enum for stair types based on visual characteristics
     */
    enum class StairType {
        ASCENDING,      // Stairs going up
        DESCENDING,     // Stairs going down
        SIDE_VIEW,      // Stairs viewed from side
        SPIRAL,         // Spiral/curved stairs
        UNKNOWN         // Cannot determine type
    }
    
    /**
     * Enum for distance estimation
     */
    enum class Distance {
        VERY_CLOSE,     // < 1 meter - immediate danger
        CLOSE,          // 1-2 meters
        MEDIUM,         // 2-4 meters
        FAR             // > 4 meters
    }

    data class Detection(
        val x: Float,           // Center X in normalized [0, 1] coordinates
        val y: Float,           // Center Y in normalized [0, 1] coordinates
        val w: Float,           // Width in normalized [0, 1] coordinates
        val h: Float,           // Height in normalized [0, 1] coordinates
        val score: Float,       // Confidence score [0, 1]
        val stairType: StairType,  // Type of stairs detected
        val distance: Distance     // Estimated distance to stairs
    ) {
        /**
         * Get human-readable description of stair type
         */
        fun getTypeDescription(): String = when (stairType) {
            StairType.ASCENDING -> "ascending stairs"
            StairType.DESCENDING -> "descending stairs"
            StairType.SIDE_VIEW -> "stairs from side"
            StairType.SPIRAL -> "spiral stairs"
            StairType.UNKNOWN -> "stairs"
        }
        
        /**
         * Get human-readable distance description
         */
        fun getDistanceDescription(): String = when (distance) {
            Distance.VERY_CLOSE -> "immediately ahead"
            Distance.CLOSE -> "1 to 2 meters ahead"
            Distance.MEDIUM -> "2 to 4 meters ahead"
            Distance.FAR -> "far ahead"
        }
        
        /**
         * Get distance in meters (approximate)
         */
        fun getDistanceMeters(): Float = when (distance) {
            Distance.VERY_CLOSE -> 0.5f
            Distance.CLOSE -> 1.5f
            Distance.MEDIUM -> 3.0f
            Distance.FAR -> 5.0f
        }
    }

    private val interpreter: Interpreter

    init {
        Log.d(TAG, "Initializing YoloDetector...")
        val model = loadModelFile(MODEL_FILE)
        interpreter = Interpreter(model)

        // Log model details for debugging
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d(TAG, "Model loaded successfully")
        Log.d(TAG, "Input shape: [${inputShape.joinToString(", ")}]")
        Log.d(TAG, "Output shape: [${outputShape.joinToString(", ")}]")
        Log.d(TAG, "Confidence threshold: $CONFIDENCE_THRESHOLD")
    }

    /**
     * Load TFLite model from assets into ByteBuffer
     */
    private fun loadModelFile(filename: String): ByteBuffer {
        val assetFile: InputStream = context.assets.open(filename)
        val bytes = assetFile.readBytes()
        assetFile.close()

        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.flip() // Prepare for reading
        return buffer
    }

    /**
     * Run inference on preprocessed input
     * 
     * @param inputBuffer ByteBuffer containing 640x640x3 float32 RGB data normalized to [0, 1]
     * @param confThreshold Optional custom confidence threshold (default uses CONFIDENCE_THRESHOLD)
     * @return List of Detection objects for stairs found
     */
    fun detect(inputBuffer: ByteBuffer, confThreshold: Float = CONFIDENCE_THRESHOLD): List<Detection> {
        // Ensure buffer is positioned at start
        inputBuffer.rewind()

        // Prepare output array: [1][5][8400]
        // Index 0-3: x, y, w, h coordinates (normalized)
        // Index 4: confidence score
        val output = Array(1) { Array(5) { FloatArray(8400) } }

        // Run inference
        val startTime = System.currentTimeMillis()
        interpreter.run(inputBuffer, output)
        val inferenceTime = System.currentTimeMillis() - startTime

        // Process detections
        val detections = mutableListOf<Detection>()
        var maxConfidence = 0f
        var totalAboveThreshold = 0

        for (i in 0 until 8400) {
            val score = output[0][4][i]
            
            // Track maximum confidence for debugging
            if (score > maxConfidence) {
                maxConfidence = score
            }

            // Filter by confidence threshold
            if (score >= confThreshold) {
                totalAboveThreshold++
                
                val x = output[0][0][i]
                val y = output[0][1][i]
                val w = output[0][2][i]
                val h = output[0][3][i]

                // Only add valid detections (coordinates should be in [0, 1] range)
                if (x in 0f..1f && y in 0f..1f && w > 0 && h > 0) {
                    // Analyze stair characteristics
                    val stairType = analyzeStairType(x, y, w, h)
                    val distance = estimateDistance(y, h)
                    
                    detections.add(Detection(x, y, w, h, score, stairType, distance))
                }
            }
        }

        // Log detection results
        Log.d(TAG, "Inference: ${inferenceTime}ms | Max conf: %.3f | Detections: %d (threshold: %.2f)"
            .format(maxConfidence, detections.size, confThreshold))
        
        if (totalAboveThreshold > detections.size) {
            Log.w(TAG, "Filtered ${totalAboveThreshold - detections.size} invalid detections")
        }

        return detections
    }

    /**
     * Analyze stair type based on position and shape
     * 
     * Heuristics:
     * - High Y position (top of frame) + tall = likely ascending (going up)
     * - Low Y position (bottom of frame) + wide = likely descending (going down)  
     * - Tall and narrow = side view
     * - Aspect ratio close to 1:1 with curve indicators = spiral
     */
    private fun analyzeStairType(x: Float, y: Float, w: Float, h: Float): StairType {
        val aspectRatio = w / h
        
        return when {
            // Ascending: stairs appear at top of frame, vertical orientation
            y < 0.4f && h > 0.3f && aspectRatio < 1.2f -> StairType.ASCENDING
            
            // Descending: stairs at bottom, horizontal orientation
            y > 0.6f && w > 0.4f && aspectRatio > 1.0f -> StairType.DESCENDING
            
            // Side view: very tall and narrow
            aspectRatio < 0.6f && h > 0.4f -> StairType.SIDE_VIEW
            
            // Spiral: roughly square aspect ratio, medium size
            aspectRatio in 0.8f..1.2f && w in 0.2f..0.5f -> StairType.SPIRAL
            
            // Default
            else -> StairType.UNKNOWN
        }
    }
    
    /**
     * Estimate distance based on position and size in frame
     * 
     * Heuristics:
     * - Lower Y + larger size = closer
     * - Higher Y + smaller size = farther
     * - Size of bounding box correlates with distance
     */
    private fun estimateDistance(y: Float, h: Float): Distance {
        // Calculate a simple distance score based on vertical position and height
        val sizeScore = h * 100f  // Height as percentage
        val positionScore = y * 100f  // Position as percentage
        
        // Closer objects appear lower in frame and larger
        val distanceScore = (100f - positionScore) + sizeScore
        
        return when {
            distanceScore > 120f -> Distance.VERY_CLOSE  // Large and low in frame
            distanceScore > 80f -> Distance.CLOSE
            distanceScore > 50f -> Distance.MEDIUM
            else -> Distance.FAR
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        interpreter.close()
        Log.d(TAG, "YoloDetector closed")
    }
}
