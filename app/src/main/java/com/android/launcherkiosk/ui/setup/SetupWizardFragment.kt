package com.android.launcherkiosk.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.R
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.ui.admin.AdminSettingsActivity
import com.android.launcherkiosk.ui.actionButton
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.screenRoot
import com.android.launcherkiosk.ui.titleView

class SetupWizardFragment : Fragment() {
    private lateinit var repository: KioskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = KioskRepository(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = screenRoot(context)
        root.addView(titleView(context, getString(R.string.first_setup)))
        root.addView(bodyView(context, getString(R.string.setup_description)))

        val password = EditText(context).apply {
            hint = getString(R.string.admin_password_min_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(password)
        root.addView(actionButton(context, getString(R.string.save_password)) {
            val value = password.text.toString()
            if (value.length < 4) {
                Toast.makeText(context, R.string.password_min_error, Toast.LENGTH_SHORT).show()
            } else {
                repository.setAdminPassword(value)
                Toast.makeText(context, R.string.password_saved, Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(actionButton(context, getString(R.string.continue_settings)) {
            if (repository.getSettings().adminPasswordHash.isBlank()) {
                Toast.makeText(context, R.string.set_admin_password_first, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(context, AdminSettingsActivity::class.java))
            }
        })
        root.addView(actionButton(context, getString(R.string.finish_setup_home)) {
            if (repository.getSettings().adminPasswordHash.isBlank()) {
                Toast.makeText(context, R.string.set_admin_password_first, Toast.LENGTH_SHORT).show()
                return@actionButton
            }
            repository.setSetupCompleted(true)
            (requireActivity() as MainActivity).showHome()
        })
        return root
    }
}
