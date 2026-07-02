package com.android.launcherkiosk.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

class KioskRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): KioskSettings = KioskSettings(
        setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false),
        adminPasswordHash = prefs.getString(KEY_PASSWORD_HASH, "").orEmpty(),
        adminPasswordSalt = prefs.getString(KEY_PASSWORD_SALT, "").orEmpty(),
        kioskEnabled = prefs.getBoolean(KEY_KIOSK_ENABLED, true),
        accessibilityEnabled = prefs.getBoolean(KEY_ACCESSIBILITY_ENABLED, true),
        overlayEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, true),
        deviceAdminEnabled = prefs.getBoolean(KEY_DEVICE_ADMIN_ENABLED, true)
    )

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun setAdminPassword(password: String) {
        val salt = randomHex(16)
        prefs.edit()
            .putString(KEY_PASSWORD_SALT, salt)
            .putString(KEY_PASSWORD_HASH, sha256("$salt:$password"))
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val salt = prefs.getString(KEY_PASSWORD_SALT, "").orEmpty()
        val hash = prefs.getString(KEY_PASSWORD_HASH, "").orEmpty()
        return salt.isNotBlank() && hash == sha256("$salt:$password")
    }

    fun getWhitelist(): List<WhitelistApp> {
        return prefs.getStringSet(KEY_WHITELIST, emptySet()).orEmpty()
            .mapIndexed { index, item ->
                val parts = item.split("|", limit = 2)
                WhitelistApp(
                    packageName = parts[0],
                    appName = parts.getOrNull(1) ?: parts[0],
                    enabled = true,
                    sortOrder = index
                )
            }
            .sortedWith(compareBy<WhitelistApp> { it.sortOrder }.thenBy { it.appName.lowercase() })
    }

    fun setWhitelist(apps: List<WhitelistApp>) {
        val values = apps.filter { it.enabled }
            .map { "${it.packageName}|${it.appName}" }
            .toSet()
        prefs.edit().putStringSet(KEY_WHITELIST, values).apply()
    }

    fun setKioskEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KIOSK_ENABLED, enabled).apply()
    }

    fun setAccessibilityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_ENABLED, enabled).apply()
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    private fun randomHex(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return data.joinToString("") { "%02x".format(it) }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "kiosk_settings"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_WHITELIST = "whitelist"
        private const val KEY_KIOSK_ENABLED = "kiosk_enabled"
        private const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled"
    }
}
