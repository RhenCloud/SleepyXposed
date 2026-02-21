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

                        if (packageName != null && packageName != lastForegroundPackage) {
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
}
