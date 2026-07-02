package com.android.launcherkiosk.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.data.KioskRepository
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
        root.addView(bodyView(context, "完成管理员密码、白名单和权限引导后进入受控主页。"))

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
        root.addView(actionButton(context, "选择白名单应用") { (requireActivity() as MainActivity).showWhitelist(fromSetup = true) })
        root.addView(actionButton(context, "打开权限引导") { (requireActivity() as MainActivity).showPermissions() })
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
