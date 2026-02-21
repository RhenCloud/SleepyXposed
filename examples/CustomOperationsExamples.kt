package com.rhencloud.sleepyxposed.examples

import com.rhencloud.sleepyxposed.ForegroundAppMonitor
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example extensions for ForegroundAppMonitor
 * 
 * This file shows how you can extend the functionality of the 
 * executeCustomOperations method with more advanced features.
 * 
 * NEW: Examples of how to use the getCurrentForegroundPackage() and related methods
 * to retrieve the current foreground app name.
 */

object CustomOperationsExamples {

    /**
     * Example 0: Get current foreground app information
     * 
     * You can now retrieve the current foreground app at any time:
     */
    fun getCurrentAppInfo() {
        val packageName = ForegroundAppMonitor.getCurrentForegroundPackage()
        val activityName = ForegroundAppMonitor.getCurrentForegroundActivity()
        val componentName = ForegroundAppMonitor.getCurrentForegroundComponentName()
        
        XposedBridge.log("SleepyXposed: Current foreground package: $packageName")
        XposedBridge.log("SleepyXposed: Current foreground activity: $activityName")
        XposedBridge.log("SleepyXposed: Current foreground component: $componentName")
    }

    /**
     * Example 1: Log app switches with timestamps to a file
     */
    fun logToFile(packageName: String, activityName: String?) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            val logFile = File("/sdcard/SleepyXposed/app_switches.log")
            
            // Create directory if it doesn't exist
            logFile.parentFile?.mkdirs()
            
            val logEntry = "$timestamp - $packageName${activityName?.let { "/$it" } ?: ""}\n"
            logFile.appendText(logEntry)
            
        } catch (e: Exception) {
            XposedBridge.log("SleepyXposed: Failed to log to file: ${e.message}")
        }
    }

    /**
     * Example 2: Count app switches and report statistics
     */
    object AppUsageCounter {
        private val appSwitchCount = mutableMapOf<String, Int>()
        
        fun recordSwitch(packageName: String) {
            appSwitchCount[packageName] = (appSwitchCount[packageName] ?: 0) + 1
            
            // Log statistics every 10 switches
            if (appSwitchCount.values.sum() % 10 == 0) {
                logStatistics()
            }
        }
        
        private fun logStatistics() {
            XposedBridge.log("SleepyXposed: === App Usage Statistics ===")
            appSwitchCount.entries
                .sortedByDescending { it.value }
                .take(5)
                .forEach { (pkg, count) ->
                    XposedBridge.log("SleepyXposed: $pkg: $count switches")
                }
        }
    }

    /**
     * Example 3: Trigger actions for specific apps
     */
    fun triggerAppSpecificActions(packageName: String) {
        when {
            // Gaming apps
            packageName.contains("game", ignoreCase = true) -> {
                XposedBridge.log("SleepyXposed: [ACTION] Game detected - Consider performance boost")
            }
            
            // Social media apps
            packageName in listOf(
                "com.facebook.katana",
                "com.instagram.android",
                "com.twitter.android",
                "com.snapchat.android"
            ) -> {
                XposedBridge.log("SleepyXposed: [ACTION] Social media app opened")
            }
            
            // Video streaming apps
            packageName in listOf(
                "com.google.android.youtube",
                "com.netflix.mediaclient",
                "com.amazon.avod.thirdpartyclient"
            ) -> {
                XposedBridge.log("SleepyXposed: [ACTION] Video app - Consider adjusting screen brightness")
            }
            
            // Browser apps
            packageName.contains("browser", ignoreCase = true) ||
            packageName.contains("chrome", ignoreCase = true) -> {
                XposedBridge.log("SleepyXposed: [ACTION] Browser opened")
            }
        }
    }

    /**
     * Example 4: Track screen time by app
     */
    object ScreenTimeTracker {
        private var currentApp: String? = null
        private var currentAppStartTime: Long = 0
        private val screenTime = mutableMapOf<String, Long>()
        
        fun onAppSwitch(newPackageName: String) {
            val now = System.currentTimeMillis()
            
            // Record time for previous app
            currentApp?.let { prevApp ->
                val duration = now - currentAppStartTime
                screenTime[prevApp] = (screenTime[prevApp] ?: 0) + duration
            }
            
            // Start tracking new app
            currentApp = newPackageName
            currentAppStartTime = now
            
            // Log screen time periodically
            if (screenTime.size % 5 == 0) {
                logScreenTime()
            }
        }
        
        private fun logScreenTime() {
            XposedBridge.log("SleepyXposed: === Screen Time Report ===")
            screenTime.entries
                .sortedByDescending { it.value }
                .take(5)
                .forEach { (pkg, time) ->
                    val minutes = time / 60000
                    XposedBridge.log("SleepyXposed: $pkg: $minutes minutes")
                }
        }
    }

    /**
     * Example 5: Filter out system apps
     */
    fun shouldProcessApp(packageName: String): Boolean {
        val systemPackages = setOf(
            "com.android.systemui",
            "android",
            "com.android.launcher3"
        )
        
        return packageName !in systemPackages
    }

    /**
     * Example 6: Detect rapid app switching (possible user distraction)
     */
    object RapidSwitchDetector {
        private val switchTimestamps = mutableListOf<Long>()
        private const val RAPID_SWITCH_THRESHOLD = 3 // switches
        private const val TIME_WINDOW_MS = 5000L // 5 seconds
        
        fun onAppSwitch() {
            val now = System.currentTimeMillis()
            
            // Remove old timestamps outside the time window
            switchTimestamps.removeAll { it < now - TIME_WINDOW_MS }
            
            // Add current timestamp
            switchTimestamps.add(now)
            
            // Check for rapid switching
            if (switchTimestamps.size >= RAPID_SWITCH_THRESHOLD) {
                XposedBridge.log("SleepyXposed: [WARNING] Rapid app switching detected!")
                // Could trigger a notification or log this behavior
            }
        }
    }

    /**
     * Example usage in ForegroundAppMonitor.executeCustomOperations():
     * 
     * private fun executeCustomOperations(packageName: String, activityName: String?) {
     *     // NEW: Get current foreground app info at any time
     *     val currentPkg = ForegroundAppMonitor.getCurrentForegroundPackage()
     *     val currentActivity = ForegroundAppMonitor.getCurrentForegroundActivity()
     *     val currentComponent = ForegroundAppMonitor.getCurrentForegroundComponentName()
     *     
     *     // Use the examples:
     *     
     *     // Filter system apps
     *     if (!CustomOperationsExamples.shouldProcessApp(packageName)) {
     *         return
     *     }
     *     
     *     // Log to file
     *     CustomOperationsExamples.logToFile(packageName, activityName)
     *     
     *     // Count switches
     *     CustomOperationsExamples.AppUsageCounter.recordSwitch(packageName)
     *     
     *     // Trigger app-specific actions
     *     CustomOperationsExamples.triggerAppSpecificActions(packageName)
     *     
     *     // Track screen time
     *     CustomOperationsExamples.ScreenTimeTracker.onAppSwitch(packageName)
     *     
     *     // Detect rapid switching
     *     CustomOperationsExamples.RapidSwitchDetector.onAppSwitch()
     * }
     * 
     * You can also call these methods from outside the module:
     * 
     * // From any hook or code that has access to ForegroundAppMonitor:
     * val foregroundApp = ForegroundAppMonitor.getCurrentForegroundPackage()
     * XposedBridge.log("Current app: $foregroundApp")
     * 
     * // Get app display name (requires Context):
     * val monitor = ForegroundAppMonitor()
     * val displayName = monitor.getAppDisplayName(context, foregroundApp ?: "")
     * XposedBridge.log("Current app name: $displayName")
     */
}
