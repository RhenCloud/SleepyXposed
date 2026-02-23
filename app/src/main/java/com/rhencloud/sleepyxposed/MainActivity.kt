package com.rhencloud.sleepyxposed

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var serverUrlEdit: EditText
    private lateinit var secretEdit: EditText
    private lateinit var deviceIdEdit: EditText
    private lateinit var showNameEdit: EditText
    private lateinit var enabledSwitch: Switch
    private lateinit var saveButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        try {
            serverUrlEdit = findViewById(R.id.server_url)
            secretEdit = findViewById(R.id.secret)
            deviceIdEdit = findViewById(R.id.device_id)
            showNameEdit = findViewById(R.id.show_name)
            enabledSwitch = findViewById(R.id.enabled_switch)
            saveButton = findViewById(R.id.save_button)
            statusText = findViewById(R.id.status_text)

            // Load saved configuration from JSON file
            loadConfiguration()

            // Set up save button
            saveButton.setOnClickListener { saveConfiguration() }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing UI: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun loadConfiguration() {
        val config = ConfigManager.loadConfig(this)

        serverUrlEdit.setText(config.serverUrl)
        secretEdit.setText(config.secret)
        deviceIdEdit.setText(config.deviceId)
        showNameEdit.setText(config.showName)
        enabledSwitch.isChecked = config.enabled
    }

    private fun saveConfiguration() {
        val url = serverUrlEdit.text.toString()
        val secret = secretEdit.text.toString()
        val id = deviceIdEdit.text.toString()
        val showName = showNameEdit.text.toString()
        val enabled = enabledSwitch.isChecked

        if (url.isEmpty() || secret.isEmpty() || id.isEmpty() || showName.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        // Create config object
        val config =
                SleepyConfig(
                        serverUrl = url,
                        secret = secret,
                        deviceId = id,
                        showName = showName,
                        enabled = enabled
                )

        // Save to JSON file
        val success = ConfigManager.saveConfig(this, config)

        if (success) {
            statusText.text = getString(R.string.config_saved)
            statusText.visibility = View.VISIBLE

            Toast.makeText(this, getString(R.string.config_saved_toast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save configuration", Toast.LENGTH_SHORT).show()
        }
    }
}
