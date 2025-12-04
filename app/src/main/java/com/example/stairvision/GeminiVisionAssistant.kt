package com.example.stairvision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * GeminiVisionAssistant - Uses Google Gemini AI for intelligent scene analysis
 * and context-aware audio guidance for visually impaired users
 */
class GeminiVisionAssistant(
    private val context: Context,
    private val audioAssistant: GeminiAudioAssistant
) {
    private val TAG = "GeminiVisionAssistant"
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    private var lastAnalysisTime = 0L
    private val ANALYSIS_INTERVAL_MS = 8000L // Analyze every 8 seconds to save cost
    
    private var isAnalyzing = false
    
    /**
     * Analyze the scene with Gemini AI and provide intelligent guidance
     */
    fun analyzeScene(
        bitmap: Bitmap,
        detections: List<YoloDetector.Detection>,
        force: Boolean = false
    ) {
        // Don't analyze if already analyzing or too soon
        val now = System.currentTimeMillis()
        if (isAnalyzing || (!force && (now - lastAnalysisTime) < ANALYSIS_INTERVAL_MS)) {
            return
        }
        
        // Empty detections - skip Gemini to save cost
        if (detections.isEmpty() && !force) {
            return
        }
        
        lastAnalysisTime = now
        isAnalyzing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🤖 Starting Gemini AI analysis...")
                
                val prompt = buildIntelligentPrompt(detections)
                
                // Resize bitmap to reduce API cost
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                
                val response = generativeModel.generateContent(
                    content {
                        image(resizedBitmap)
                        text(prompt)
                    }
                )
                
                response.text?.let { guidance ->
                    Log.d(TAG, "✅ Gemini guidance: $guidance")
                    audioAssistant.speakNow(guidance)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gemini analysis failed", e)
                // Fallback to basic announcement
                if (detections.isNotEmpty()) {
                    val primary = detections.first()
                    val basicGuidance = "Stairs detected ahead. ${primary.getTypeDescription()}. Distance approximately ${primary.getDistanceMeters().toInt()} meters."
                    audioAssistant.speakNow(basicGuidance)
                }
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    /**
     * Answer user's specific question about the scene
     */
    fun answerQuestion(bitmap: Bitmap, question: String) {
        if (isAnalyzing) {
            audioAssistant.speakNow("Please wait, analyzing the scene.")
            return
        }
        
        isAnalyzing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🤖 Gemini Q&A: $question")
                
                val prompt = """
                    You are an AI assistant helping a visually impaired person navigate safely.
                    The user asks: "$question"
                    
                    Analyze the image and provide:
                    1. A clear, direct answer to their question
                    2. Any relevant safety information
                    3. Specific guidance about what you see
                    
                    Response requirements:
                    - Be conversational and encouraging
                    - Focus on actionable information
                    - Mention stairs, handrails, obstacles, lighting
                    - Keep response under 3-4 sentences
                    - Be specific about locations (left, right, ahead)
                """.trimIndent()
                
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                
                val response = generativeModel.generateContent(
                    content {
                        image(resizedBitmap)
                        text(prompt)
                    }
                )
                
                response.text?.let { answer ->
                    Log.d(TAG, "✅ Gemini answer: $answer")
                    audioAssistant.speakNow(answer)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gemini Q&A failed", e)
                audioAssistant.speakNow("Sorry, I couldn't analyze the scene. Please try again later.")
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    /**
     * Build context-aware prompt for Gemini based on YOLO detections
     */
    private fun buildIntelligentPrompt(detections: List<YoloDetector.Detection>): String {
        if (detections.isEmpty()) {
            return """
                You are assisting a visually impaired person using a stair detection app.
                The AI did not detect any stairs in this image.
                
                Analyze and describe:
                1. What is visible in the scene (hallway, room, outdoor, etc.)
                2. Any potential obstacles or hazards for mobility
                3. Brief navigation guidance (safe to proceed, turn around, etc.)
                
                Keep response encouraging, clear, and under 3 sentences.
                Focus on safety and mobility.
            """.trimIndent()
        }
        
        val primary = detections.first()
        val stairType = primary.getTypeDescription()
        val distance = primary.getDistanceDescription()
        
        // Estimate step count from height
        val stepCount = when {
            primary.h > 0.5f -> "15-20"
            primary.h > 0.3f -> "10-15"
            primary.h > 0.2f -> "5-10"
            else -> "3-5"
        }
        
        // Determine handrail side
        val handrailGuide = when {
            primary.x < 0.35f -> "Handrail likely on the right side"
            primary.x > 0.65f -> "Handrail likely on the left side"  
            else -> "Handrails may be available on both sides"
        }
        
        return """
            You are assisting a visually impaired person using a stair detection app.
            
            AI Detection Results:
            - Type: $stairType
            - Distance: $distance  
            - Estimated steps: $stepCount
            - Position: ${handrailGuide}
            
            Provide intelligent guidance including:
            1. Confirm the stair detection with type (ascending/descending)
            2. Describe condition (well-maintained, worn out, narrow, wide)
            3. Mention lighting (well-lit, dim, shadowy)
            4. Specify handrail location if visible (left, right, both, none)
            5. Any safety concerns (wet floor, obstacles, uneven steps)
            6. Brief navigation advice (approach slowly, use handrail, etc.)
            
            Response requirements:
            - Use conversational, encouraging tone
            - Be specific and actionable
            - Keep under 4 sentences
            - Focus on most helpful navigation tips
            - Mention step count: "$stepCount steps"
        """.trimIndent()
    }
    
    /**
     * Check if Gemini is currently analyzing
     */
    fun isAnalyzing(): Boolean = isAnalyzing
}
