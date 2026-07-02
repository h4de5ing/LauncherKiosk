package com.android.launcherkiosk.data

data class KioskSettings(
    val setupCompleted: Boolean,
    val adminPasswordHash: String,
    val adminPasswordSalt: String,
    val kioskEnabled: Boolean,
    val accessibilityEnabled: Boolean,
    val deviceAdminEnabled: Boolean
)
