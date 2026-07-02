package com.android.launcherkiosk.ui.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.receiver.KioskDeviceAdminReceiver
import com.android.launcherkiosk.ui.actionButton
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.screenRoot
import com.android.launcherkiosk.ui.titleView

class AdminPanelFragment : Fragment() {
    private lateinit var repository: KioskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = KioskRepository(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = screenRoot(context)
        val settings = repository.getSettings()
        root.addView(titleView(context, "管理员后台"))
        root.addView(bodyView(context, "管理白名单、密码和轻量拦截策略。"))

        root.addView(Switch(context).apply {
            text = "启用 Kiosk 模式"
            isChecked = settings.kioskEnabled
            setOnCheckedChangeListener { _, checked -> repository.setKioskEnabled(checked) }
        })
        root.addView(Switch(context).apply {
            text = "启用辅助服务检测"
            isChecked = settings.accessibilityEnabled
            setOnCheckedChangeListener { _, checked -> repository.setAccessibilityEnabled(checked) }
        })
        root.addView(Switch(context).apply {
            text = "启用悬浮窗拦截"
            isChecked = settings.overlayEnabled
            setOnCheckedChangeListener { _, checked -> repository.setOverlayEnabled(checked) }
        })

        val newPassword = EditText(context).apply {
            hint = "新管理员密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(newPassword)
        root.addView(actionButton(context, "修改密码") {
            val value = newPassword.text.toString()
            if (value.length < 4) {
                Toast.makeText(context, "密码至少 4 位", Toast.LENGTH_SHORT).show()
            } else {
                repository.setAdminPassword(value)
                newPassword.text.clear()
                Toast.makeText(context, "密码已修改", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(actionButton(context, "修改白名单") { (requireActivity() as MainActivity).showWhitelist() })
        root.addView(actionButton(context, "权限引导") { (requireActivity() as MainActivity).showPermissions() })
        root.addView(actionButton(context, "打开系统设置") { startActivity(Intent(Settings.ACTION_SETTINGS)) })
        root.addView(actionButton(context, "临时退出到系统桌面设置") { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) })
        root.addView(actionButton(context, "关闭设备管理器权限") { removeDeviceAdmin(context) })
        root.addView(actionButton(context, "返回主页") { (requireActivity() as MainActivity).showHome() })
        return root
    }

    private fun removeDeviceAdmin(context: Context) {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, KioskDeviceAdminReceiver::class.java)
        if (manager.isAdminActive(component)) {
            manager.removeActiveAdmin(component)
            Toast.makeText(context, "设备管理器权限已关闭", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "设备管理器权限未启用", Toast.LENGTH_SHORT).show()
        }
    }
}
