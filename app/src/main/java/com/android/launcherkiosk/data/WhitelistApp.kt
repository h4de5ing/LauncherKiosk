package com.android.launcherkiosk.data

data class WhitelistApp(
    val packageName: String,
    val appName: String,
    val enabled: Boolean,
    val sortOrder: Int
)
