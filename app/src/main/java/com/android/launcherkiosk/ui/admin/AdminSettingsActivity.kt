package com.android.launcherkiosk.ui.admin

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.provider.Settings
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.policy.KioskPolicyManager
import com.android.launcherkiosk.receiver.KioskDeviceAdminReceiver

class AdminSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                .replace(android.R.id.content, AdminSettingsFragment())
                .commit()
        }
    }

    class AdminSettingsFragment : PreferenceFragment() {
        private lateinit var repository: KioskRepository
        private lateinit var policyManager: KioskPolicyManager

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            repository = KioskRepository(activity)
            policyManager = KioskPolicyManager(activity)
            buildSettings()
        }

        override fun onResume() {
            super.onResume()
            buildSettings()
        }

        private fun buildSettings() {
            val screen = preferenceManager.createPreferenceScreen(activity)
            val settings = repository.getSettings()

            screen.addPreference(PreferenceCategory(activity).apply { title = "策略开关" })
            screen.addPreference(SwitchPreference(activity).apply {
                title = "启用 Kiosk 模式"
                summary = "关闭后辅助服务遮罩策略不再拦截"
                isChecked = settings.kioskEnabled
                setOnPreferenceChangeListener { _, value ->
                    repository.setKioskEnabled(value as Boolean)
                    true
                }
            })
            screen.addPreference(SwitchPreference(activity).apply {
                title = "启用辅助服务检测"
                summary = "检测设置页、SystemUI 等风险入口"
                isChecked = settings.accessibilityEnabled
                setOnPreferenceChangeListener { _, value ->
                    repository.setAccessibilityEnabled(value as Boolean)
                    true
                }
            })

            screen.addPreference(PreferenceCategory(activity).apply { title = "配置" })
            screen.addPreference(action("应用白名单", "选择主页允许启动的应用") {
                openMain(MainActivity.ACTION_OPEN_WHITELIST)
            })
            screen.addPreference(action("修改管理员密码", "更新进入设置页所需的密码") {
                showPasswordDialog()
            })

            screen.addPreference(PreferenceCategory(activity).apply { title = "权限与系统" })
            screen.addPreference(action("权限状态", permissionSummary()) {
                buildSettings()
            })
            screen.addPreference(action("默认桌面设置", "将 LauncherKiosk 设为默认桌面") {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            })
            screen.addPreference(action("启用设备管理器", deviceAdminSummary()) {
                requestDeviceAdmin()
            })
            screen.addPreference(action("开启辅助服务", accessibilitySummary()) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            screen.addPreference(action("系统设置", "打开 Android 系统设置") {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            })
            screen.addPreference(action("关闭设备管理器权限", deviceAdminSummary()) {
                removeDeviceAdmin()
            })
            screen.addPreference(action("返回主页", "退出管理员设置") {
                openMain(null)
                activity.finish()
            })

            preferenceScreen = screen
        }

        private fun action(title: String, summary: String, block: () -> Unit): Preference {
            return Preference(activity).apply {
                this.title = title
                this.summary = summary
                setOnPreferenceClickListener {
                    block()
                    true
                }
            }
        }

        private fun showPasswordDialog() {
            val input = EditText(activity).apply {
                hint = "新管理员密码，至少 4 位"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            AlertDialog.Builder(activity)
                .setTitle("修改管理员密码")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存") { _, _ ->
                    val value = input.text.toString()
                    if (value.length < 4) {
                        Toast.makeText(activity, "密码至少 4 位", Toast.LENGTH_SHORT).show()
                    } else {
                        repository.setAdminPassword(value)
                        Toast.makeText(activity, "密码已修改", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        private fun openMain(action: String?) {
            val intent = Intent(activity, MainActivity::class.java).apply {
                if (action != null) this.action = action
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }

        private fun permissionSummary(): String {
            return "设备管理器: ${enabledText(policyManager.isDeviceAdminActive())}  " +
                "辅助服务: ${enabledText(policyManager.isAccessibilityServiceEnabled())}"
        }

        private fun deviceAdminSummary(): String {
            return if (policyManager.isDeviceAdminActive()) "当前已启用，点击后关闭" else "当前未启用"
        }

        private fun accessibilitySummary(): String {
            return if (policyManager.isAccessibilityServiceEnabled()) "当前已启用" else "当前未启用"
        }

        private fun enabledText(enabled: Boolean): String = if (enabled) "已启用" else "未启用"

        private fun removeDeviceAdmin() {
            val manager = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(activity, KioskDeviceAdminReceiver::class.java)
            if (manager.isAdminActive(component)) {
                manager.removeActiveAdmin(component)
                Toast.makeText(activity, "设备管理器权限已关闭", Toast.LENGTH_SHORT).show()
                buildSettings()
            } else {
                Toast.makeText(activity, "设备管理器权限未启用", Toast.LENGTH_SHORT).show()
            }
        }

        private fun requestDeviceAdmin() {
            val component = ComponentName(activity, KioskDeviceAdminReceiver::class.java)
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "用于提高 LauncherKiosk 被卸载的门槛。")
            })
        }
    }
}
