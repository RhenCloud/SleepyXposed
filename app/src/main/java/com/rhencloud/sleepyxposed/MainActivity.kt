package com.rhencloud.sleepyxposed

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    
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
        
        prefs = getSharedPreferences("sleepy_config", Context.MODE_PRIVATE)
        
        // Initialize views
        serverUrlEdit = findViewById(R.id.server_url)
        secretEdit = findViewById(R.id.secret)
        deviceIdEdit = findViewById(R.id.device_id)
        showNameEdit = findViewById(R.id.show_name)
        enabledSwitch = findViewById(R.id.enabled_switch)
        saveButton = findViewById(R.id.save_button)
        statusText = findViewById(R.id.status_text)
        
        // Load saved configuration
        loadConfiguration()
        
        // Set up save button
        saveButton.setOnClickListener {
            saveConfiguration()
        }
    }
    
    private fun loadConfiguration() {
        serverUrlEdit.setText(prefs.getString("server_url", ""))
        secretEdit.setText(prefs.getString("secret", ""))
        deviceIdEdit.setText(prefs.getString("id", ""))
        showNameEdit.setText(prefs.getString("show_name", ""))
        enabledSwitch.isChecked = prefs.getBoolean("enabled", false)
    }
    
    private fun saveConfiguration() {
        val url = serverUrlEdit.text.toString()
        val secret = secretEdit.text.toString()
        val id = deviceIdEdit.text.toString()
        val showName = showNameEdit.text.toString()
        val enabled = enabledSwitch.isChecked
        
        if (url.isEmpty() || secret.isEmpty() || id.isEmpty() || showName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefs.edit().apply {
            putString("server_url", url)
            putString("secret", secret)
            putString("id", id)
            putString("show_name", showName)
            putBoolean("enabled", enabled)
            apply()
        }
        
        // Clear cached config in ForegroundAppMonitor
        ForegroundAppMonitor.cachedConfig = null
        
        statusText.text = "Configuration saved successfully!\n\nPlease reboot your device for changes to take effect."
        statusText.visibility = View.VISIBLE
        
        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
        
        LogRepository.addLog(LogLevel.INFO, "Configuration saved")
    }
}
