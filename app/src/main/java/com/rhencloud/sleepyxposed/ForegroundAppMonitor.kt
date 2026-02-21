package com.rhencloud.sleepyxposed

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class ForegroundAppMonitor : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "SleepyXposed"
        private const val SYSTEM_SERVER = "android"
        private const val REPORT_DELAY_MS = 1000L
        private const val CONFIG_NAME = "sleepy_config"
        
        private var lastForegroundPackage: String? = null
        private var currentForegroundPackage: String? = null
        private var currentForegroundActivity: String? = null
        
        var cachedConfig: Config? = null
        
        /**
         * Get the current foreground application package name.
         * @return The package name of the current foreground app, or null if not available
         */
        @JvmStatic
        fun getCurrentForegroundPackage(): String? {
            return currentForegroundPackage
        }
        
        /**
         * Get the current foreground activity name.
         * @return The activity name of the current foreground app, or null if not available
         */
        @JvmStatic
        fun getCurrentForegroundActivity(): String? {
            return currentForegroundActivity
        }
        
        /**
         * Get the full component name of the current foreground app.
         * @return The component name in format "packageName/activityName", or just packageName if activity is unknown
         */
        @JvmStatic
        fun getCurrentForegroundComponentName(): String? {
            return currentForegroundPackage?.let { pkg ->
                currentForegroundActivity?.let { activity ->
                    "$pkg/$activity"
                } ?: pkg
            }
        }
        
        /**
         * Utility method to get application label/display name from package name.
         * This requires a Context object and PackageManager.
         * 
         * Example usage from within a hook that has access to Context:
         * val displayName = ForegroundAppMonitor.getAppDisplayName(context, "com.android.chrome")
         * 
         * @param context Android Context
         * @param packageName Package name of the app
         * @return Display name of the app, or package name if not found
         */
        @JvmStatic
        fun getAppDisplayName(context: android.content.Context, packageName: String): String {
            return try {
                val packageManager = context.packageManager
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: Exception) {
                XposedBridge.log("$TAG: Failed to get app display name for $packageName: ${e.message}")
                packageName
            }
        }
    }

    data class Config(
        val url: String,
        val secret: String,
        val id: String,
        val showName: String,
        val enabled: Boolean = true
    )

    private val handler = Handler(Looper.getMainLooper())
    private var reportRunnable: Runnable? = null
    private var systemContext: Context? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // We only hook into the system server process
        if (lpparam.packageName != SYSTEM_SERVER) {
            return
        }

        try {
            // Get system context for accessing battery manager and shared preferences
            hookActivityTaskManagerService(lpparam)
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook: ${e.message}")
            LogRepository.addLog(LogLevel.ERROR, "Failed to hook: ${e.message}")
        }
    }

    private fun hookActivityTaskManagerService(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook the method that handles activity resume in the system
        // This is called when an activity comes to the foreground
        val activityRecordClass = XposedHelpers.findClass(
            "com.android.server.wm.ActivityRecord",
            lpparam.classLoader
        )

        // Get system context
        try {
            val activityThreadClass = XposedHelpers.findClass(
                "android.app.ActivityThread",
                lpparam.classLoader
            )
            val currentActivityThread = XposedHelpers.callStaticMethod(
                activityThreadClass,
                "currentActivityThread"
            )
            systemContext = XposedHelpers.callMethod(
                currentActivityThread,
                "getSystemContext"
            ) as? Context
            
            if (systemContext != null) {
                // Load configuration from shared preferences
                loadConfiguration()
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to get system context: ${e.message}")
        }

        // Hook the completeResumeLocked method which is called when an activity is resumed
        XposedHelpers.findAndHookMethod(
            activityRecordClass,
            "completeResumeLocked",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        // Get the ActivityRecord instance
                        val activityRecord = param.thisObject

                        // Get package name
                        val packageName = XposedHelpers.getObjectField(
                            activityRecord,
                            "packageName"
                        ) as? String

                        // Get activity info to extract the full component name
                        val activityInfo = XposedHelpers.getObjectField(
                            activityRecord,
                            "info"
                        )
                        
                        val activityName = if (activityInfo != null) {
                            XposedHelpers.getObjectField(activityInfo, "name") as? String
                        } else {
                            null
                        }

                        if (packageName != null) {
                            // Update current foreground app info
                            currentForegroundPackage = packageName
                            currentForegroundActivity = activityName
                            
                            // Only log and execute operations if the package changed
                            if (packageName != lastForegroundPackage) {
                                // Foreground app has changed!
                                lastForegroundPackage = packageName

                                val componentName = if (activityName != null) {
                                    "$packageName/$activityName"
                                } else {
                                    packageName
                                }

                                // Log the app switch
                                XposedBridge.log("$TAG: Foreground app switched to: $componentName")
                                LogRepository.addLog(LogLevel.INFO, "App switched to: $componentName")

                                // Execute custom operations here
                                executeCustomOperations(packageName, activityName)
                            }
                        }
                    } catch (e: Throwable) {
                        XposedBridge.log("$TAG: Error in hook: ${e.message}")
                        LogRepository.addLog(LogLevel.ERROR, "Hook error: ${e.message}")
                    }
                }
            }
        )

        XposedBridge.log("$TAG: Successfully hooked into ActivityRecord.completeResumeLocked")
        LogRepository.addLog(LogLevel.INFO, "Successfully hooked into system")
    }

    private fun loadConfiguration() {
        try {
            systemContext?.let { context ->
                val prefs = context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE)
                val url = prefs.getString("server_url", null)
                val secret = prefs.getString("secret", null)
                val id = prefs.getString("id", null)
                val showName = prefs.getString("show_name", null)
                val enabled = prefs.getBoolean("enabled", false)

                if (url != null && secret != null && id != null && showName != null) {
                    cachedConfig = Config(url, secret, id, showName, enabled)
                    XposedBridge.log("$TAG: Configuration loaded successfully")
                    LogRepository.addLog(LogLevel.INFO, "Config loaded: $showName")
                } else {
                    XposedBridge.log("$TAG: Configuration incomplete")
                    LogRepository.addLog(LogLevel.WARN, "Config incomplete")
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to load configuration: ${e.message}")
            LogRepository.addLog(LogLevel.ERROR, "Config load failed: ${e.message}")
        }
    }

    /**
     * Execute custom operations when foreground app switches.
     * This is where you can add your custom logic.
     * Now integrates with Sleepy API to report device status.
     */
    private fun executeCustomOperations(packageName: String, activityName: String?) {
        // Cancel any pending report
        reportRunnable?.let {
            handler.removeCallbacks(it)
        }

        // Schedule a delayed report (debouncing)
        reportRunnable = Runnable {
            // Load configuration
            val config = cachedConfig ?: run {
                LogRepository.addLog(LogLevel.DEBUG, "No config available, skipping report")
                reportRunnable = null
                return@Runnable
            }

            if (!config.enabled) {
                LogRepository.addLog(LogLevel.DEBUG, "Reporting disabled in config")
                reportRunnable = null
                return@Runnable
            }

            try {
                // Get app display name
                val appName = try {
                    systemContext?.let { context ->
                        val pm = context.packageManager
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } ?: packageName
                } catch (e: Exception) {
                    packageName
                }

                // Get battery info
                val batteryInfo = try {
                    systemContext?.let { context ->
                        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                        val battery = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                        val chargingStatus = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
                        val isCharging = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING || 
                                       chargingStatus == BatteryManager.BATTERY_STATUS_FULL
                        
                        val batteryStr = if (battery in 0..100) "$battery%" else "-"
                        val chargingIcon = if (isCharging) "⚡️" else "🔋"
                        "[$batteryStr]$chargingIcon"
                    } ?: ""
                } catch (e: Exception) {
                    ""
                }

                val statusText = "$appName$batteryInfo"

                // Send to Sleepy server
                SleepyApiClient.sendDeviceStatus(
                    url = config.url,
                    secret = config.secret,
                    id = config.id,
                    showName = config.showName,
                    using = true, // Device is being used since app is in foreground
                    status = statusText,
                    callback = object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            XposedBridge.log("$TAG: Failed to send status: ${e.message}")
                            LogRepository.addLog(LogLevel.ERROR, "Failed to send: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (response.isSuccessful) {
                                    XposedBridge.log("$TAG: Status sent successfully: $statusText")
                                    LogRepository.addLog(LogLevel.INFO, "Sent: $statusText")
                                } else {
                                    XposedBridge.log("$TAG: Server error: ${response.code}")
                                    LogRepository.addLog(LogLevel.WARN, "Server error: ${response.code}")
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                XposedBridge.log("$TAG: Error in custom operations: ${e.message}")
                LogRepository.addLog(LogLevel.ERROR, "Operation error: ${e.message}")
            }

            reportRunnable = null
        }

        handler.postDelayed(reportRunnable!!, REPORT_DELAY_MS)
        
        // Example: Log when specific apps come to foreground
        when (packageName) {
            "com.android.chrome" -> {
                XposedBridge.log("$TAG: [OPERATION] Browser opened")
                LogRepository.addLog(LogLevel.DEBUG, "Browser opened")
            }
            "com.android.systemui" -> {
                // Usually system UI, might want to ignore
            }
            else -> {
                XposedBridge.log("$TAG: [OPERATION] App switch detected: $packageName")
            }
        }
    }
}
