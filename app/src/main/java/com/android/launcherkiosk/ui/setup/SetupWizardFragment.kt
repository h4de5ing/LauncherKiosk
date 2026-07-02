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
        root.addView(titleView(context, "首次配置"))
        root.addView(bodyView(context, "设置管理员密码后，可进入设置页继续配置白名单和系统权限。"))

        val password = EditText(context).apply {
            hint = "管理员密码，至少 4 位"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(password)
        root.addView(actionButton(context, "保存密码") {
            val value = password.text.toString()
            if (value.length < 4) {
                Toast.makeText(context, "密码至少 4 位", Toast.LENGTH_SHORT).show()
            } else {
                repository.setAdminPassword(value)
                Toast.makeText(context, "密码已保存", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(actionButton(context, "进入设置页继续配置") {
            if (repository.getSettings().adminPasswordHash.isBlank()) {
                Toast.makeText(context, "请先设置管理员密码", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(context, AdminSettingsActivity::class.java))
            }
        })
        root.addView(actionButton(context, "完成配置并进入主页") {
            if (repository.getSettings().adminPasswordHash.isBlank()) {
                Toast.makeText(context, "请先设置管理员密码", Toast.LENGTH_SHORT).show()
                return@actionButton
            }
            repository.setSetupCompleted(true)
            (requireActivity() as MainActivity).showHome()
        })
        return root
    }
}
