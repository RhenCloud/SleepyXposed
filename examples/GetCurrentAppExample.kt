package com.rhencloud.sleepyxposed.examples

import com.rhencloud.sleepyxposed.ForegroundAppMonitor
import de.robv.android.xposed.XposedBridge

/**
 * Example demonstrating how to use the new getCurrentForegroundPackage() methods.
 * 
 * This example shows various ways to retrieve and use the current foreground app information.
 */
object GetCurrentAppExample {

    /**
     * Simple example: Get and log the current foreground app package name
     */
    fun logCurrentApp() {
        val packageName = ForegroundAppMonitor.getCurrentForegroundPackage()
        if (packageName != null) {
            XposedBridge.log("SleepyXposed: Current foreground app package: $packageName")
        } else {
            XposedBridge.log("SleepyXposed: No foreground app detected yet")
        }
    }

    /**
     * Get all information about the current foreground app
     */
    fun getAllCurrentAppInfo() {
        val packageName = ForegroundAppMonitor.getCurrentForegroundPackage()
        val activityName = ForegroundAppMonitor.getCurrentForegroundActivity()
        val componentName = ForegroundAppMonitor.getCurrentForegroundComponentName()
        
        XposedBridge.log("SleepyXposed: === Current Foreground App Info ===")
        XposedBridge.log("SleepyXposed: Package: $packageName")
        XposedBridge.log("SleepyXposed: Activity: $activityName")
        XposedBridge.log("SleepyXposed: Component: $componentName")
    }

    /**
     * Check if a specific app is currently in the foreground
     */
    fun isAppInForeground(targetPackage: String): Boolean {
        val currentPackage = ForegroundAppMonitor.getCurrentForegroundPackage()
        return currentPackage == targetPackage
    }

    /**
     * Example: Execute action only if specific app is in foreground
     */
    fun executeIfAppInForeground(targetPackage: String, action: () -> Unit) {
        if (isAppInForeground(targetPackage)) {
            XposedBridge.log("SleepyXposed: $targetPackage is in foreground, executing action")
            action()
        }
    }

    /**
     * Example: Monitor if a browser is currently active
     */
    fun isBrowserActive(): Boolean {
        val currentPackage = ForegroundAppMonitor.getCurrentForegroundPackage() ?: return false
        
        val browserPackages = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.brave.browser",
            "com.microsoft.emmx"
        )
        
        return browserPackages.any { currentPackage.contains(it) }
    }

    /**
     * Example: Get app category based on package name
     */
    fun getCurrentAppCategory(): String {
        val packageName = ForegroundAppMonitor.getCurrentForegroundPackage() ?: return "Unknown"
        
        return when {
            packageName.contains("chrome") || packageName.contains("firefox") || 
            packageName.contains("browser") -> "Browser"
            
            packageName.contains("game") -> "Game"
            
            packageName in listOf("com.facebook.katana", "com.instagram.android", 
                                  "com.twitter.android", "com.snapchat.android") -> "Social Media"
            
            packageName in listOf("com.google.android.youtube", "com.netflix.mediaclient") -> "Video"
            
            packageName.contains("systemui") || packageName == "android" -> "System"
            
            else -> "Other"
        }
    }

    /**
     * Example: Log category change when app switches
     */
    fun logCategoryChange() {
        val category = getCurrentAppCategory()
        val packageName = ForegroundAppMonitor.getCurrentForegroundPackage()
        
        XposedBridge.log("SleepyXposed: Current app category: $category ($packageName)")
    }

    /**
     * Example usage in ForegroundAppMonitor.executeCustomOperations():
     * 
     * private fun executeCustomOperations(packageName: String, activityName: String?) {
     *     // Check if a specific app is in foreground
     *     if (GetCurrentAppExample.isAppInForeground("com.android.chrome")) {
     *         XposedBridge.log("SleepyXposed: Chrome is active!")
     *     }
     *     
     *     // Log category change
     *     GetCurrentAppExample.logCategoryChange()
     *     
     *     // Execute action only for specific app
     *     GetCurrentAppExample.executeIfAppInForeground("com.example.app") {
     *         // Your custom action here
     *         XposedBridge.log("SleepyXposed: Executing custom action for com.example.app")
     *     }
     *     
     *     // Check if browser is active
     *     if (GetCurrentAppExample.isBrowserActive()) {
     *         XposedBridge.log("SleepyXposed: A browser is currently active")
     *     }
     * }
     */
}
