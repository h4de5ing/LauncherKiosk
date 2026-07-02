package com.android.launcherkiosk.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.android.launcherkiosk.receiver.KioskDeviceAdminReceiver
import com.android.launcherkiosk.service.KioskAccessibilityService

class KioskPolicyManager(private val context: Context) {
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, KioskDeviceAdminReceiver::class.java)

    fun isDeviceAdminActive(): Boolean = devicePolicyManager.isAdminActive(adminComponent)

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expected = "${context.packageName}/${KioskAccessibilityService::class.java.name}"
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
