package com.android.launcherkiosk.ui.admin

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

class AdminLoginFragment : Fragment() {
    private lateinit var repository: KioskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = KioskRepository(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = screenRoot(context)
        root.addView(titleView(context, "管理员验证"))
        root.addView(bodyView(context, "请输入管理员密码进入后台。"))
        val password = EditText(context).apply {
            hint = "管理员密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(password)
        root.addView(actionButton(context, "登录") {
            if (repository.verifyPassword(password.text.toString())) {
                (requireActivity() as MainActivity).showAdminPanel()
            } else {
                Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
            }
        })
        return root
    }
}
