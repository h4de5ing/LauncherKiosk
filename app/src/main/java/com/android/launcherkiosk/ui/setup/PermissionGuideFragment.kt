package com.android.launcherkiosk.ui.setup

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.receiver.KioskDeviceAdminReceiver
import com.android.launcherkiosk.ui.actionButton
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.screenRoot
import com.android.launcherkiosk.ui.titleView

class PermissionGuideFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = screenRoot(context)
        root.addView(titleView(context, "权限引导"))
        root.addView(bodyView(context, "按需开启默认桌面、设备管理器、辅助服务和悬浮窗权限。"))

        root.addView(actionButton(context, "设置默认桌面") {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        })
        root.addView(actionButton(context, "启用设备管理器") {
            val component = ComponentName(context, KioskDeviceAdminReceiver::class.java)
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "用于提高 LauncherKiosk 被卸载的门槛。")
            })
        })
        root.addView(actionButton(context, "开启辅助服务") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(actionButton(context, "开启悬浮窗权限") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        })
        return root
    }
}
