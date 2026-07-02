package com.android.launcherkiosk.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.android.launcherkiosk.data.KioskRepository

class KioskOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var repository: KioskRepository
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = KioskRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> hideOverlay()
            else -> showTopBlocker()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    private fun showTopBlocker() {
        val settings = repository.getSettings()
        if (!settings.kioskEnabled || !settings.overlayEnabled || !Settings.canDrawOverlays(this)) return
        if (overlayView != null) {
            scheduleHide()
            return
        }

        val message = TextView(this).apply {
            text = "当前 Kiosk 模式不允许此操作"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xEE111827.toInt())
            gravity = Gravity.CENTER
            setOnClickListener { hideOverlay() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(160),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = message
        windowManager.addView(message, params)
        scheduleHide()
    }

    private fun scheduleHide() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ hideOverlay() }, 2500)
    }

    private fun hideOverlay() {
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
        }
        overlayView = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_SHOW_TOP_BLOCKER = "com.android.launcherkiosk.SHOW_TOP_BLOCKER"
        const val ACTION_HIDE = "com.android.launcherkiosk.HIDE_OVERLAY"
    }
}
