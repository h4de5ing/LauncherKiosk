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
import com.android.launcherkiosk.R
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
        root.addView(titleView(context, getString(R.string.admin_panel)))
        root.addView(bodyView(context, getString(R.string.admin_panel_description)))

        root.addView(Switch(context).apply {
            text = getString(R.string.enable_kiosk_mode)
            isChecked = settings.kioskEnabled
            setOnCheckedChangeListener { _, checked -> repository.setKioskEnabled(checked) }
        })
        root.addView(Switch(context).apply {
            text = getString(R.string.enable_accessibility_detection)
            isChecked = settings.accessibilityEnabled
            setOnCheckedChangeListener { _, checked -> repository.setAccessibilityEnabled(checked) }
        })
        val newPassword = EditText(context).apply {
            hint = getString(R.string.new_admin_password)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(newPassword)
        root.addView(actionButton(context, getString(R.string.change_password)) {
            val value = newPassword.text.toString()
            if (value.length < 4) {
                Toast.makeText(context, R.string.password_min_error, Toast.LENGTH_SHORT).show()
            } else {
                repository.setAdminPassword(value)
                newPassword.text.clear()
                Toast.makeText(context, R.string.password_changed, Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(actionButton(context, getString(R.string.change_whitelist)) { (requireActivity() as MainActivity).showWhitelist() })
        root.addView(actionButton(context, getString(R.string.open_settings_page)) {
            startActivity(Intent(context, AdminSettingsActivity::class.java))
        })
        root.addView(actionButton(context, getString(R.string.open_system_settings)) { startActivity(Intent(Settings.ACTION_SETTINGS)) })
        root.addView(actionButton(context, getString(R.string.temporary_exit_home_settings)) { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) })
        root.addView(actionButton(context, getString(R.string.disable_device_admin)) { removeDeviceAdmin(context) })
        root.addView(actionButton(context, getString(R.string.return_home)) { (requireActivity() as MainActivity).showHome() })
        return root
    }

    private fun removeDeviceAdmin(context: Context) {
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, KioskDeviceAdminReceiver::class.java)
        if (manager.isAdminActive(component)) {
            manager.removeActiveAdmin(component)
            Toast.makeText(context, R.string.device_admin_disabled, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.device_admin_not_enabled, Toast.LENGTH_SHORT).show()
        }
    }
}
