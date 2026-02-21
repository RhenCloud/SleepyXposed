package com.rhencloud.sleepyxposed

import android.app.ActivityManager
import android.content.ComponentName
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ForegroundAppMonitor : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "SleepyXposed"
        private const val SYSTEM_SERVER = "android"
        private var lastForegroundPackage: String? = null
        private var currentForegroundPackage: String? = null
        private var currentForegroundActivity: String? = null
        
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
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // We only hook into the system server process
        if (lpparam.packageName != SYSTEM_SERVER) {
            return
        }

        try {
            hookActivityTaskManagerService(lpparam)
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Failed to hook: ${e.message}")
        }
    }

    private fun hookActivityTaskManagerService(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook the method that handles activity resume in the system
        // This is called when an activity comes to the foreground
        val activityRecordClass = XposedHelpers.findClass(
            "com.android.server.wm.ActivityRecord",
            lpparam.classLoader
        )

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

                                // Execute custom operations here
                                executeCustomOperations(packageName, activityName)
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

    /**
     * Execute custom operations when foreground app switches.
     * This is where you can add your custom logic.
     */
    private fun executeCustomOperations(packageName: String, activityName: String?) {
        // Example operations:
        // 1. Log the switch
        XposedBridge.log("$TAG: [OPERATION] New foreground app: $packageName")
        
        // 2. You can add more operations here such as:
        // - Send broadcast
        // - Update shared preferences
        // - Trigger specific actions based on package name
        // - Record app usage statistics
        // - Implement app-specific behaviors
        
        // Example: Log when specific apps come to foreground
        when (packageName) {
            "com.android.chrome" -> {
                XposedBridge.log("$TAG: [OPERATION] Browser opened")
            }
            "com.android.systemui" -> {
                // Usually system UI, might want to ignore
            }
            else -> {
                XposedBridge.log("$TAG: [OPERATION] App switch detected: $packageName")
            }
        }
    }
    
    /**
     * Utility method to get application label/display name from package name.
     * This requires a Context object and PackageManager.
     * 
     * Example usage from within a hook that has access to Context:
     * val displayName = getAppDisplayName(context, "com.android.chrome")
     * 
     * @param context Android Context
     * @param packageName Package name of the app
     * @return Display name of the app, or package name if not found
     */
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
