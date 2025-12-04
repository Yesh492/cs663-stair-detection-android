package com.example.stairvision

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

/**
 * OverlayView - Professional overlay with visual guidance for stair detection
 * 
 * Features:
 * - Distance-coded bounding boxes (red/orange/yellow/green)
 * - Green line indicating first step edge (at 2.8m)
 * - Blue vertical center line for alignment
 * - Lateral guidance arrows (left/right)
 * - Confidence and distance labels
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val BOX_STROKE_WIDTH = 8f
        private const val TEXT_SIZE = 36f
        private const val TEXT_PADDING = 8f
        
        // First step edge marker (green line at bottom)
        private const val FIRST_STEP_THRESHOLD_METERS = 2.8f
        private const val FIRST_STEP_LINE_WIDTH = 6f
        
        // Center line (blue vertical line)
        private const val CENTER_LINE_WIDTH = 4f
        
        // Lateral guidance
        private const val OFF_CENTER_THRESHOLD = 0.15f  // 15% off center triggers guidance
        private const val ARROW_SIZE = 60f
    }

    // Current detections to display
    private var detections: List<YoloDetector.Detection> = emptyList()

    // Paint for bounding boxes
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
        isAntiAlias = true
    }

    // Paint for text background
    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
    }

    // Paint for text
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = TEXT_SIZE
        style = Paint.Style.FILL
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Paint for first step edge line (green)
    private val firstStepLinePaint = Paint().apply {
        color = Color.argb(255, 76, 175, 80) // Material green
        strokeWidth = FIRST_STEP_LINE_WIDTH
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Paint for center line (blue)
    private val centerLinePaint = Paint().apply {
        color = Color.argb(200, 33, 150, 243) // Material blue, semi-transparent
        strokeWidth = CENTER_LINE_WIDTH
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f) // Dashed line
    }

    // Paint for arrows
    private val arrowPaint = Paint().apply {
        color = Color.argb(200, 255, 193, 7) // Material amber
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /**
     * Update detections and trigger redraw
     */
    fun setDetections(newDetections: List<YoloDetector.Detection>) {
        detections = newDetections
        invalidate()
    }

    /**
     * Main drawing method
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth == 0f || viewHeight == 0f) return

        // Always draw center line (helps with alignment)
        drawCenterLine(canvas, viewWidth, viewHeight)

        if (detections.isEmpty()) return

        // Get primary detection (closest/highest confidence)
        val primaryDetection = detections.maxByOrNull { 
            (5f - it.getDistanceMeters()) + it.score 
        } ?: detections.first()

        // Draw bounding boxes for all detections
        for (detection in detections) {
            drawDetection(canvas, detection, viewWidth, viewHeight)
        }

        // Draw first step edge line if close enough
        if (primaryDetection.distance == YoloDetector.Distance.VERY_CLOSE ||
            primaryDetection.distance == YoloDetector.Distance.CLOSE) {
            drawFirstStepEdge(canvas, primaryDetection, viewWidth, viewHeight)
        }

        // Draw lateral guidance arrows if off-center
        drawLateralGuidance(canvas, primaryDetection, viewWidth, viewHeight)
    }

    /**
     * Draw bounding box for a detection with distance-coded color
     */
    private fun drawDetection(
        canvas: Canvas,
        detection: YoloDetector.Detection,
        viewWidth: Float,
        viewHeight: Float
    ) {
        // Convert normalized coordinates to screen coordinates
        val centerX = detection.x * viewWidth
        val centerY = detection.y * viewHeight
        val boxWidth = detection.w * viewWidth
        val boxHeight = detection.h * viewHeight

        val left = centerX - boxWidth / 2
        val top = centerY - boxHeight / 2
        val right = centerX + boxWidth / 2
        val bottom = centerY + boxHeight / 2

        // Distance-coded colors (matching reference images)
        val boxColor = when (detection.distance) {
            YoloDetector.Distance.VERY_CLOSE -> Color.rgb(211, 47, 47)      // Red - urgent
            YoloDetector.Distance.CLOSE -> Color.rgb(255, 152, 0)           // Orange - warning
            YoloDetector.Distance.MEDIUM -> Color.rgb(255, 235, 59)         // Yellow - caution
            YoloDetector.Distance.FAR -> Color.rgb(76, 175, 80)             // Green - notice
        }
        boxPaint.color = boxColor

        // Draw main bounding box
        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Draw label with stair info
        drawLabel(canvas, detection, left, top, boxColor)
    }

    /**
     * Draw comprehensive label for detection
     */
    private fun drawLabel(
        canvas: Canvas,
        detection: YoloDetector.Detection,
        boxLeft: Float,
        boxTop: Float,
        color: Int
    ) {
        val stairType = detection.getTypeDescription().uppercase()
        val distance = "${detection.getDistanceMeters().toInt()}m"
        val confidence = "${(detection.score * 100).toInt()}%"
        
        val text = "$stairType | $distance"

        // Measure text
        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        // Position above box if possible
        val labelTop = if (boxTop - bounds.height() - TEXT_PADDING * 3 > 0) {
            boxTop - bounds.height() - TEXT_PADDING * 3
        } else {
            boxTop + TEXT_PADDING
        }

        // Draw background
        canvas.drawRect(
            boxLeft,
            labelTop,
            boxLeft + bounds.width() + TEXT_PADDING * 2,
            labelTop + bounds.height() + TEXT_PADDING * 2,
            textBackgroundPaint
        )

        // Draw text
        textPaint.color = color
        canvas.drawText(
            text,
            boxLeft + TEXT_PADDING,
            labelTop + bounds.height() + TEXT_PADDING,
            textPaint
        )
    }

    /**
     * Draw green line indicating first step edge (at ~2.8m reference)
     */
    private fun drawFirstStepEdge(
        canvas: Canvas,
        detection: YoloDetector.Detection,
        viewWidth: Float,
        viewHeight: Float
    ) {
        // Calculate bottom edge of detection box
        val centerY = detection.y * viewHeight
        val boxHeight = detection.h * viewHeight
        val bottom = centerY + boxHeight / 2

        // Draw green line at bottom of stairs (first step edge)
        val lineY = bottom.coerceIn(viewHeight * 0.7f, viewHeight * 0.95f)
        
        canvas.drawRect(
            0f,
            lineY - FIRST_STEP_LINE_WIDTH / 2,
            viewWidth,
            lineY + FIRST_STEP_LINE_WIDTH / 2,
            firstStepLinePaint
        )

        // Draw label
        val labelText = "FIRST STEP EDGE"
        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
        
        textPaint.color = firstStepLinePaint.color
        textPaint.textSize = TEXT_SIZE * 0.8f
        canvas.drawText(
            labelText,
            (viewWidth - textBounds.width()) / 2,
            lineY - 10f,
            textPaint
        )
        textPaint.textSize = TEXT_SIZE
    }

    /**
     * Draw blue vertical center line
     */
    private fun drawCenterLine(canvas: Canvas, viewWidth: Float, viewHeight: Float) {
        val centerX = viewWidth / 2
        canvas.drawLine(
            centerX,
            viewHeight * 0.2f,
            centerX,
            viewHeight * 0.9f,
            centerLinePaint
        )

        // Draw "STAIRS CENTER LINE" text
        val labelText = "CENTER LINE"
        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
        
        // Rotate text 90 degrees for vertical orientation
        canvas.save()
        canvas.rotate(-90f, centerX, viewHeight / 2)
        
        textPaint.color = centerLinePaint.color
        textPaint.textSize = TEXT_SIZE * 0.7f
        canvas.drawText(
            labelText,
            centerX - textBounds.height() / 2,
            viewHeight / 2 + textBounds.width() / 2,
            textPaint
        )
        canvas.restore()
        textPaint.textSize = TEXT_SIZE
    }

    /**
     * Draw lateral guidance arrows when stairs are off-center
     */
    private fun drawLateralGuidance(
        canvas: Canvas,
        detection: YoloDetector.Detection,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val centerX = viewWidth / 2
        val stairCenterX = detection.x * viewWidth
        
        // Calculate offset from center (normalized)
        val offset = (stairCenterX - centerX) / viewWidth
        
        // Only show arrows if significantly off-center
        if (abs(offset) < OFF_CENTER_THRESHOLD) return

        // Determine direction
        val needsMoveRight = offset < 0  // Stairs are left, user needs to move right
        
        // Position arrows at bottom third of screen
        val arrowY = viewHeight * 0.75f
        
        if (needsMoveRight) {
            // Draw right arrow
            drawArrow(canvas, viewWidth * 0.75f, arrowY, true)
        } else {
            // Draw left arrow
            drawArrow(canvas, viewWidth * 0.25f, arrowY, false)
        }
    }

    /**
     * Draw directional arrow
     */
    private fun drawArrow(canvas: Canvas, x: Float, y: Float, pointingRight: Boolean) {
        val path = Path()
        val size = ARROW_SIZE
        
        if (pointingRight) {
            // Right-pointing arrow
            path.moveTo(x - size, y - size)
            path.lineTo(x + size, y)
            path.lineTo(x - size, y + size)
            path.lineTo(x - size * 0.5f, y)
            path.close()
        } else {
            // Left-pointing arrow
            path.moveTo(x + size, y - size)
            path.lineTo(x - size, y)
            path.lineTo(x + size, y + size)
            path.lineTo(x + size * 0.5f, y)
            path.close()
        }
        
        canvas.drawPath(path, arrowPaint)
        
        // Draw direction text below arrow
        val directionText = if (pointingRight) "Move Right" else "Move Left"
        textPaint.color = arrowPaint.color
        textPaint.textSize = TEXT_SIZE * 0.9f
        
        val textBounds = Rect()
        textPaint.getTextBounds(directionText, 0, directionText.length, textBounds)
        
        canvas.drawText(
            directionText,
            x - textBounds.width() / 2,
            y + size + textBounds.height() + 10f,
            textPaint
        )
        textPaint.textSize = TEXT_SIZE
    }

    /**
     * Clear all detections
     */
    fun clear() {
        detections = emptyList()
        invalidate()
    }
}
