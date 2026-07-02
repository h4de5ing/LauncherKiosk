package com.android.launcherkiosk.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.android.launcherkiosk.R
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.util.DebugUtils
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class KioskAccessibilityService : AccessibilityService() {
    private lateinit var repository: KioskRepository
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val overlays = linkedMapOf<String, OverlayHandle>()
    private val backDispatcher = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "kiosk-a11y-back").apply { isDaemon = true }
    }
    private val lastBackDispatchAt = AtomicLong(0L)

    override fun onCreate() {
        super.onCreate()
        repository = KioskRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val settings = repository.getSettings()
        if (!settings.kioskEnabled || !settings.accessibilityEnabled) {
            clearOverlays()
            return
        }

        val root = rootInActiveWindow ?: run {
            clearOverlays()
            return
        }
        val packageName = root.packageName?.toString().orEmpty()
        if (!packageName.contains("systemui", ignoreCase = true)) {
            clearOverlays()
            return
        }

        val targets = buildMap {
            BLOCK_TARGETS.forEach { target ->
                resolveOverlaySpec(root, target)?.let { put(target.key, it) }
            }
        }
        syncOverlays(targets)
    }

    override fun onInterrupt() {
        clearOverlays()
    }

    override fun onDestroy() {
        clearOverlays()
        backDispatcher.shutdownNow()
        super.onDestroy()
    }

    private fun syncOverlays(targets: Map<String, OverlaySpec>) {
        overlays.keys.filterNot(targets::containsKey).toList().forEach(::removeOverlay)
        targets.forEach { (key, spec) ->
            val existing = overlays[key]
            if (existing == null) {
                addOverlay(spec)
            } else if (existing.spec.bounds != spec.bounds) {
                updateOverlay(existing, spec)
            }
        }
    }

    private fun resolveOverlaySpec(root: AccessibilityNodeInfo, target: BlockTarget): OverlaySpec? {
        val resolved = target.viewIds.asSequence()
            .flatMap { viewId ->
                root.findAccessibilityNodeInfosByViewId(viewId).asSequence()
                    .filter { it.isVisibleToUser }
                    .mapNotNull { node ->
                        val bounds = resolveBounds(node, target.expandFromTop)
                        if (bounds.width() <= 0 || bounds.height() <= 0) null else bounds
                    }
            }
            .maxByOrNull { it.width() * it.height() }
            ?: return null
        return OverlaySpec(target, resolved)
    }

    private fun resolveBounds(node: AccessibilityNodeInfo, expandFromTop: Boolean): Rect {
        val bounds = Rect().also(node::getBoundsInScreen)
        return if (expandFromTop) {
            Rect(0, 0, resources.displayMetrics.widthPixels, bounds.bottom)
        } else {
            bounds
        }
    }

    private fun addOverlay(spec: OverlaySpec) {
        runCatching {
            val view = createMaskView()
            windowManager.addView(view, createLayoutParams(spec.bounds))
            overlays[spec.target.key] = OverlayHandle(view, spec)
        }
    }

    private fun updateOverlay(handle: OverlayHandle, spec: OverlaySpec) {
        runCatching {
            windowManager.updateViewLayout(handle.view, createLayoutParams(spec.bounds))
            overlays[spec.target.key] = handle.copy(spec = spec)
        }
    }

    private fun removeOverlay(key: String) {
        val handle = overlays.remove(key) ?: return
        runCatching { windowManager.removeView(handle.view) }
    }

    private fun clearOverlays() {
        overlays.keys.toList().forEach(::removeOverlay)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createMaskView(): View {
        return View(this).apply {
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setBackgroundColor(if (DebugUtils.isDebuggable(this@KioskAccessibilityService)) DEBUG_MASK_COLOR else Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    Toast.makeText(this@KioskAccessibilityService, R.string.blocked_operation, Toast.LENGTH_SHORT).show()
                    dispatchBackAsync()
                }
                true
            }
        }
    }

    private fun dispatchBackAsync() {
        val now = SystemClock.elapsedRealtime()
        while (true) {
            val previous = lastBackDispatchAt.get()
            if (now - previous < BACK_THROTTLE_MS) return
            if (lastBackDispatchAt.compareAndSet(previous, now)) break
        }

        backDispatcher.schedule(
            {
                runCatching {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            },
            BACK_DISPATCH_DELAY_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun createLayoutParams(bounds: Rect): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            bounds.width(),
            bounds.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left
            y = bounds.top
        }
    }

    private data class BlockTarget(
        val key: String,
        val viewIds: List<String>,
        val expandFromTop: Boolean = false
    )

    private data class OverlaySpec(
        val target: BlockTarget,
        val bounds: Rect
    )

    private data class OverlayHandle(
        val view: View,
        val spec: OverlaySpec
    )

    companion object {
        private const val DEBUG_MASK_COLOR = 0x55FF0000
        private const val BACK_DISPATCH_DELAY_MS = 120L
        private const val BACK_THROTTLE_MS = 600L

        private val BLOCK_TARGETS = listOf(
            BlockTarget(
                key = "quick_settings_container",
                viewIds = listOf(
                    "com.android.systemui:id/quick_settings_container",
                    "com.android.systemui:id/quick_settings_panel"
                ),
                expandFromTop = true
            ),
            BlockTarget(
                key = "qs_footer_actions",
                viewIds = listOf(
                    "com.android.systemui:id/qs_footer_actions",
                    "com.android.systemui:id/qs_footer_actions_container",
                    "com.android.systemui:id/qs_footer_actions_edit_container"
                )
            ),
            BlockTarget(
                key = "settings_button",
                viewIds = listOf(
                    "com.android.systemui:id/settings_button",
                    "com.android.systemui:id/settings",
                    "com.android.systemui:id/gear_icon"
                )
            ),
            BlockTarget(
                key = "manage_text",
                viewIds = listOf("com.android.systemui:id/manage_text")
            )
        )
    }
}
