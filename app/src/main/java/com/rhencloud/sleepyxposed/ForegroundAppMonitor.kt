package com.rhencloud.sleepyxposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

class ForegroundAppMonitor : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "SleepyXposed"
        private const val SYSTEM_SERVER = "android"
        private const val REPORT_DELAY_MS = 1000L
        private const val MATCH_ANY_USER_FLAG = 0x00002000
        private const val LOCK_REPORT_COOLDOWN_MS = 1_000L
        private const val ACTION_LOG_PREFIX = "LockReceiver action="

        private var lastForegroundPackage: String? = null
        private var currentForegroundPackage: String? = null
        private var currentForegroundActivity: String? = null
        private var lockReceiverRegistered: Boolean = false
        private var lastLockReportAt: Long = 0L

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
         * @return The component name in format "packageName/activityName", or just packageName if
         * activity is unknown
         */
        @JvmStatic
        fun getCurrentForegroundComponentName(): String? {
            return currentForegroundPackage?.let { pkg ->
                currentForegroundActivity?.let { activity -> "$pkg/$activity" } ?: pkg
            }
        }

        /**
         * Utility method to get application label/display name from package name. This requires a
         * Context object and PackageManager.
         *
         * @param context Android Context
         * @param packageName Package name of the app
         * @return Display name of the app, or package name if not found
         */
        @JvmStatic
        fun getAppDisplayName(context: android.content.Context, packageName: String): String {
            return try {
                val packageManager = context.packageManager
                val applicationInfo =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val flags =
                                    PackageManager.ApplicationInfoFlags.of(
                                            MATCH_ANY_USER_FLAG.toLong()
                                    )
                            packageManager.getApplicationInfo(packageName, flags)
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getApplicationInfo(packageName, 0)
                        }
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: Exception) {
                XposedBridge.log(
                        "$TAG: Failed to get app display name for $packageName: ${e.message}"
                )
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

    private var handler: Handler? = null
    private var reportRunnable: Runnable? = null
    private var systemContext: Context? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Log every package load attempt for debugging
        XposedBridge.log("$TAG: handleLoadPackage called for: ${lpparam.packageName}")

        // We only hook into the system server process
        if (lpparam.packageName != SYSTEM_SERVER) {
            return
        }

        XposedBridge.log("$TAG: Detected system server, starting hook initialization...")

        try {
            // Get system context for accessing battery manager and shared preferences
            hookActivityTaskManagerService(lpparam)
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun hookActivityTaskManagerService(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG: Starting hookActivityTaskManagerService...")

        // Hook the method that handles activity resume in the system
        // This is called when an activity comes to the foreground
        val activityRecordClass =
                XposedHelpers.findClass("com.android.server.wm.ActivityRecord", lpparam.classLoader)

        XposedBridge.log("$TAG: Found ActivityRecord class")

        // Get system context
        try {
            val activityThreadClass =
                    XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader)
            val currentActivityThread =
                    XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            systemContext =
                    XposedHelpers.callMethod(currentActivityThread, "getSystemContext") as? Context

            XposedBridge.log("$TAG: System context obtained: $systemContext")

            if (systemContext != null) {
                XposedBridge.log("$TAG: System context obtained successfully")
                val looper =
                        systemContext?.mainLooper ?: Looper.myLooper() ?: Looper.getMainLooper()
                if (looper != null) {
                    handler = Handler(looper)
                } else {
                    XposedBridge.log("$TAG: No Looper available for handler, reporting disabled")
                }
                // Load configuration from shared preferences
                loadConfiguration()
                registerLockScreenReceiver()
            } else {
                XposedBridge.log("$TAG: Failed to obtain system context")
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to get system context: ${e.message}")
            e.printStackTrace()
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
                            val packageName =
                                    XposedHelpers.getObjectField(activityRecord, "packageName") as?
                                            String

                            // Get activity info to extract the full component name
                            val activityInfo = XposedHelpers.getObjectField(activityRecord, "info")

                            val activityName =
                                    if (activityInfo != null) {
                                        XposedHelpers.getObjectField(activityInfo, "name") as?
                                                String
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

                                    var appName =
                                            getAppDisplayName(systemContext!!, packageName) as?
                                                    String

                                    val componentName =
                                            if (activityName != null) {
                                                "$packageName/$activityName/$appName"
                                            } else {
                                                packageName
                                            }

                                    // Log the app switch
                                    XposedBridge.log(
                                            "$TAG: Foreground app switched to: $componentName"
                                    )

                                    // Execute custom operations here
                                    executeCustomOperations(packageName)
                                }
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log("$TAG: Error in hook: ${e.message}")
                        }
                    }
                }
        )

        XposedBridge.log("$TAG: Successfully hooked into ActivityRecord.completeResumeLocked")
    }

    private fun loadConfiguration() {
        XposedBridge.log("$TAG: loadConfiguration called")
        try {
            val sleepyConfig = ConfigManager.loadConfigFromXSharedPreferences()

            // Convert SleepyConfig to internal Config format
            cachedConfig =
                    Config(
                            url = sleepyConfig.serverUrl,
                            secret = sleepyConfig.secret,
                            id = sleepyConfig.deviceId,
                            showName = sleepyConfig.showName,
                            enabled = sleepyConfig.enabled
                    )

            XposedBridge.log(
                    "$TAG: Configuration loaded from XSharedPreferences: ${sleepyConfig.showName}"
            )
            XposedBridge.log(
                    "$TAG: Config validity: url=${sleepyConfig.serverUrl.isNotBlank()}, secret=${sleepyConfig.secret.isNotBlank()}, id=${sleepyConfig.deviceId.isNotBlank()}, showName=${sleepyConfig.showName.isNotBlank()}, enabled=${sleepyConfig.enabled}"
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to load configuration: ${e.message}")
        }
    }

    /**
     * Execute custom operations when foreground app switches. This is where you can add your custom
     * logic. Now integrates with Sleepy API to report device status.
     */
    private fun executeCustomOperations(packageName: String) {
        val handlerInstance = handler
        if (handlerInstance == null) {
            XposedBridge.log("$TAG: Handler not initialized, skipping report")
            return
        }

        // Cancel any pending report
        reportRunnable?.let { handlerInstance.removeCallbacks(it) }

        // Schedule a delayed report (debouncing)
        reportRunnable = Runnable {
            // Load configuration
            var config = cachedConfig
            if (config == null || !isConfigUsable(config)) {
                XposedBridge.log(
                        "$TAG: Config cache missing or incomplete, reloading configuration"
                )
                loadConfiguration()
                config = cachedConfig
            }
            if (config == null || !isConfigUsable(config)) {
                XposedBridge.log("$TAG: Config is null after reload, skipping report")
                reportRunnable = null
                return@Runnable
            }

            if (!config.enabled) {
                XposedBridge.log("$TAG: Config disabled, skipping report")
                reportRunnable = null
                return@Runnable
            }

            try {
                // Get app display name
                val appName =
                        systemContext?.let { context -> getAppDisplayName(context, packageName) }
                                ?: packageName

                val batteryInfo = buildBatteryInfo()

                val statusText = "$appName$batteryInfo"

                // Send to Sleepy server
                SleepyApiClient.sendDeviceStatus(
                        baseUrl = config.url,
                        secret = config.secret,
                        id = config.id,
                        showName = config.showName,
                        using = true, // Device is being used since app is in foreground
                        status = statusText,
                        callback =
                                object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        XposedBridge.log(
                                                "$TAG: Failed to send status: ${e.message}"
                                        )
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        response.use {
                                            if (response.isSuccessful) {
                                                XposedBridge.log(
                                                        "$TAG: Status sent successfully: $statusText"
                                                )
                                            } else {
                                                XposedBridge.log(
                                                        "$TAG: Server error: ${response.code}"
                                                )
                                            }
                                        }
                                    }
                                }
                )
            } catch (e: Exception) {
                XposedBridge.log("$TAG: Error in custom operations: ${e.message}")
            }

            reportRunnable = null
        }

        handlerInstance.postDelayed(reportRunnable!!, REPORT_DELAY_MS)
    }

    private fun isConfigUsable(config: Config?): Boolean {
        return config != null &&
                config.url.isNotBlank() &&
                config.secret.isNotBlank() &&
                config.id.isNotBlank() &&
                config.showName.isNotBlank()
    }

    private fun buildBatteryInfo(): String {
        return try {
            systemContext?.let { context ->
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val battery = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val chargingStatus =
                        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
                val isCharging =
                        chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                                chargingStatus == BatteryManager.BATTERY_STATUS_FULL

                val batteryStr = if (battery in 0..100) "$battery%" else "-"
                val chargingIcon = if (isCharging) "⚡️" else "🔋"
                "[$batteryStr]$chargingIcon"
            }
                    ?: ""
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to get battery info: ${e.message}")
            ""
        }
    }

    private fun registerLockScreenReceiver() {
        if (lockReceiverRegistered) return

        val ctx = systemContext ?: return
        try {
            val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_USER_PRESENT)
                    }

            val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val action = intent?.action
                            XposedBridge.log("$TAG: $ACTION_LOG_PREFIX$action")

                            if (Intent.ACTION_SCREEN_OFF == action) {
                                val now = System.currentTimeMillis()
                                if (now - lastLockReportAt < LOCK_REPORT_COOLDOWN_MS) return
                                lastLockReportAt = now

                                handler?.post { sendLockScreenStatus() } ?: sendLockScreenStatus()
                            }
                        }
                    }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                ctx.registerReceiver(receiver, filter)
            }

            lockReceiverRegistered = true
            XposedBridge.log("$TAG: Lock screen receiver registered")
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Failed to register lock receiver: ${e.message}")
        }
    }

    private fun sendLockScreenStatus() {
        var config = cachedConfig
        if (config == null || !isConfigUsable(config)) {
            loadConfiguration()
            config = cachedConfig
        }

        if (config == null || !isConfigUsable(config)) {
            XposedBridge.log("$TAG: Lock report skipped, config unavailable")
            return
        }

        val batteryInfo = buildBatteryInfo()
        val statusText = "Screen locked$batteryInfo"
        XposedBridge.log("$TAG: Sending lock screen status: $statusText")

        try {
            SleepyApiClient.sendDeviceStatus(
                    baseUrl = config.url,
                    secret = config.secret,
                    id = config.id,
                    showName = config.showName,
                    using = false,
                    status = statusText,
                    callback =
                            object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    XposedBridge.log(
                                            "$TAG: Failed to send lock status: ${e.message}"
                                    )
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    response.use {
                                        if (response.isSuccessful) {
                                            XposedBridge.log("$TAG: Lock status sent successfully")
                                        } else {
                                            XposedBridge.log(
                                                    "$TAG: Lock status server error: ${response.code}"
                                            )
                                        }
                                    }
                                }
                            }
            )
        } catch (e: Exception) {
            XposedBridge.log("$TAG: Error sending lock status: ${e.message}")
        }
    }
}
