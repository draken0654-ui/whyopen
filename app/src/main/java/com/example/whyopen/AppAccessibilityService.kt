package com.example.whyopen

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AppAccessibilityService : AccessibilityService() {

    private lateinit var settingsManager: SettingsManager
    
    companion object {
        private var lastTriggeredPackage: String? = null
        private var lastTriggerTime: Long = 0
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // 1. If WhyOpen overlay is already active, do not trigger anything else
            if (OverlayService.isOverlayShowing) {
                return
            }

            // 2. Ignore self, system UI, and common system windows
            if (packageName == this.packageName || 
                packageName == "com.android.systemui" || 
                packageName == "android" ||
                packageName == "com.google.android.permissioncontroller") {
                return
            }
            
            // 3. Detect Home/Launcher to reset state
            if (packageName.contains("launcher") || packageName.contains("home") || packageName.contains("trebuchet")) {
                settingsManager.clearAllSessions()
                lastTriggeredPackage = null
                return
            }

            // 4. Debounce: Prevent re-triggering for the same app within 1.5 seconds
            val currentTime = System.currentTimeMillis()
            if (packageName == lastTriggeredPackage && (currentTime - lastTriggerTime) < 1500) {
                return
            }

            // 5. Main trigger logic
            if (settingsManager.isAppBlocked(packageName)) {
                if (!settingsManager.isSessionActive(packageName)) {
                    Log.d("WhyOpen", "Triggering intentionality check for: $packageName")
                    
                    lastTriggeredPackage = packageName
                    lastTriggerTime = currentTime
                    
                    settingsManager.incrementBlockedAttempt(packageName)
                    
                    val intent = Intent(this, OverlayService::class.java).apply {
                        putExtra("PACKAGE_NAME", packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startService(intent)
                }
            }
        }
    }

    override fun onInterrupt() {}
}
