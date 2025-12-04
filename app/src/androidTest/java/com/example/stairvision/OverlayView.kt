package com.example.stairvision

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 48f
        style = Paint.Style.FILL
    }

    private var results: List<FloatArray> = emptyList()

    fun setResults(detections: List<FloatArray>) {
        results = detections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (det in results) {
            val x = det[0] * width
            val y = det[1] * height
            val w = det[2] * width
            val h = det[3] * height
            val confidence = det[4]

            val left = x - w / 2
            val top = y - h / 2
            val right = x + w / 2
            val bottom = y + h / 2

            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText(
                "Stairs ${(confidence * 100).toInt()}%",
                left,
                top - 10,
                textPaint
            )
        }
    }
}
