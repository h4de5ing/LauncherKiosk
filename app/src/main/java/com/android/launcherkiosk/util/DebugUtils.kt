package com.android.launcherkiosk.util

import android.content.Context
import android.content.pm.ApplicationInfo
import com.android.launcherkiosk.BuildConfig

object DebugUtils {
    fun isDebuggable(context: Context): Boolean = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 || BuildConfig.DEBUG
}
