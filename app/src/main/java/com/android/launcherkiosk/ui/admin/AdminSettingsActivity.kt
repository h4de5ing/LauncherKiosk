package com.android.launcherkiosk.ui.admin

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.provider.Settings
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.R
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

            screen.addPreference(PreferenceCategory(activity).apply { title = getString(R.string.config) })
            screen.addPreference(action(getString(R.string.change_admin_password), getString(R.string.change_admin_password_summary)) {
                showPasswordDialog()
            })
            screen.addPreference(action(getString(R.string.app_whitelist), getString(R.string.app_whitelist_summary)) {
                openMain(MainActivity.ACTION_OPEN_WHITELIST)
            })

            screen.addPreference(PreferenceCategory(activity).apply { title = getString(R.string.permissions_and_system) })
            screen.addPreference(statusSwitch(
                title = getString(R.string.default_home_settings),
                summary = getString(R.string.default_home_summary),
                checked = policyManager.isDefaultLauncher()
            ) {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            })
            screen.addPreference(statusSwitch(
                title = getString(R.string.device_admin),
                summary = deviceAdminSummary(),
                checked = policyManager.isDeviceAdminActive()
            ) {
                requestDeviceAdmin()
            })
            screen.addPreference(statusSwitch(
                title = getString(R.string.accessibility_service),
                summary = accessibilitySummary(),
                checked = policyManager.isAccessibilityServiceEnabled()
            ) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            screen.addPreference(action(getString(R.string.system_settings), getString(R.string.system_settings_summary)) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            })
            screen.addPreference(action(getString(R.string.return_home), getString(R.string.return_home_summary)) {
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

        private fun statusSwitch(
            title: String,
            summary: String,
            checked: Boolean,
            openSettings: () -> Unit
        ): SwitchPreference {
            return SwitchPreference(activity).apply {
                this.title = title
                this.summary = summary
                isChecked = checked
                setOnPreferenceChangeListener(OnPreferenceChangeListener { preference, _ ->
                    openSettings()
                    (preference as SwitchPreference).isChecked = checked
                    false
                })
            }
        }

        private fun showPasswordDialog() {
            val input = EditText(activity).apply {
                hint = getString(R.string.admin_password_min_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            AlertDialog.Builder(activity)
                .setTitle(R.string.change_admin_password)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save) { _, _ ->
                    val value = input.text.toString()
                    if (value.length < 4) {
                        Toast.makeText(activity, R.string.password_min_error, Toast.LENGTH_SHORT).show()
                    } else {
                        repository.setAdminPassword(value)
                        Toast.makeText(activity, R.string.password_changed, Toast.LENGTH_SHORT).show()
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

        private fun deviceAdminSummary(): String {
            return if (policyManager.isDeviceAdminActive()) getString(R.string.current_enabled) else getString(R.string.current_disabled)
        }

        private fun accessibilitySummary(): String {
            return if (policyManager.isAccessibilityServiceEnabled()) getString(R.string.current_enabled) else getString(R.string.current_disabled)
        }

        private fun requestDeviceAdmin() {
            val component = ComponentName(activity, KioskDeviceAdminReceiver::class.java)
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation))
            })
        }
    }
}
