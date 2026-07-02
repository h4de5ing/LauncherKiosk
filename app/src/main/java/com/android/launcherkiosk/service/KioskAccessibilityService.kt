package com.android.launcherkiosk.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.android.launcherkiosk.data.KioskRepository

class KioskAccessibilityService : AccessibilityService() {
    private lateinit var repository: KioskRepository

    override fun onCreate() {
        super.onCreate()
        repository = KioskRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val settings = repository.getSettings()
        if (!settings.kioskEnabled || !settings.accessibilityEnabled) return

        when {
            pkg == PACKAGE_SETTINGS -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                showOverlay()
            }

            isSystemUiPackage(pkg) -> {
                showOverlay()
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun showOverlay() {
        if (!repository.getSettings().overlayEnabled) return
        startService(Intent(this, KioskOverlayService::class.java).setAction(KioskOverlayService.ACTION_SHOW_TOP_BLOCKER))
    }

    private fun isSystemUiPackage(packageName: String): Boolean {
        return packageName in setOf(
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }

    companion object {
        private const val PACKAGE_SETTINGS = "com.android.settings"
    }
}
