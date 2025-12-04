package com.example.stairvision

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * SettingsActivity - Configuration screen for StairVision
 * 
 * Features configurable:
 * - Haptic feedback (ON/OFF)
 * - Alert proximity distance (1m - 5m slider)
 * - TTS speed and pitch
 * - Lateral guidance toggle
 * - Confidence threshold adjustment
 */
class SettingsActivity : AppCompatActivity() {

    // UI elements
    private lateinit var hapticToggle: Switch
    private lateinit var lateralGuidanceToggle: Switch
    private lateinit var proximitySlider: SeekBar
    private lateinit var proximityValue: TextView
    private lateinit var confidenceSlider: SeekBar
    private lateinit var confidenceValue: TextView
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        hapticToggle = findViewById(R.id.hapticToggle)
        lateralGuidanceToggle = findViewById(R.id.lateralGuidanceToggle)
        proximitySlider = findViewById(R.id.proximitySlider)
        proximityValue = findViewById(R.id.proximityValue)
        confidenceSlider = findViewById(R.id.confidenceSlider)
        confidenceValue = findViewById(R.id.confidenceValue)
        backButton = findViewById(R.id.backButton)

        // Load current settings
        loadSettings()

        // Set up listeners
        setupListeners()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("StairVisionSettings", MODE_PRIVATE)
        
        // Load haptic feedback setting
        hapticToggle.isChecked = prefs.getBoolean("haptic_enabled", true)
        
        // Load lateral guidance setting
        lateralGuidanceToggle.isChecked = prefs.getBoolean("lateral_guidance_enabled", true)
        
        // Load proximity setting (1.0m - 5.0m)
        val proximity = prefs.getFloat("alert_proximity", 2.5f)
        proximitySlider.progress = ((proximity - 1.0f) * 10).toInt() // 0-40 range
        proximityValue.text = String.format("%.1fm", proximity)
        
        //Load confidence threshold (0.3 - 0.9)
        val confidence = prefs.getFloat("confidence_threshold", 0.6f)
        confidenceSlider.progress = ((confidence - 0.3f) * 100).toInt() // 0-60 range
        confidenceValue.text = "${(confidence * 100).toInt()}%"
    }

    private fun setupListeners() {
        // Haptic feedback toggle
        hapticToggle.setOnCheckedChangeListener { _, isChecked ->
            savePreference("haptic_enabled", isChecked)
            Toast.makeText(this, 
                "Haptic feedback ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT).show()
        }

        // Lateral guidance toggle
        lateralGuidanceToggle.setOnCheckedChangeListener { _, isChecked ->
            savePreference("lateral_guidance_enabled", isChecked)
            Toast.makeText(this,
                "Lateral guidance ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT).show()
        }

        // Proximity slider
        proximitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val proximity = 1.0f + (progress / 10.0f)
                proximityValue.text = String.format("%.1fm", proximity)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 15
                val proximity = 1.0f + (progress / 10.0f)
                savePreference("alert_proximity", proximity)
                Toast.makeText(this@SettingsActivity,
                    "Alert proximity set to ${String.format("%.1fm", proximity)}",
                    Toast.LENGTH_SHORT).show()
            }
        })

        // Confidence slider
        confidenceSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val confidence = 0.3f + (progress / 100.0f)
                confidenceValue.text = "${(confidence * 100).toInt()}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 30
                val confidence = 0.3f + (progress / 100.0f)
                savePreference("confidence_threshold", confidence)
                Toast.makeText(this@SettingsActivity,
                    "Confidence threshold set to ${(confidence * 100).toInt()}%",
                    Toast.LENGTH_SHORT).show()
            }
        })

        // Back button
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun savePreference(key: String, value: Boolean) {
        getSharedPreferences("StairVisionSettings", MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun savePreference(key: String, value: Float) {
        getSharedPreferences("StairVisionSettings", MODE_PRIVATE)
            .edit()
            .putFloat(key, value)
            .apply()
    }
}
