package com.android.launcherkiosk.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.ui.admin.AdminSettingsActivity
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.dp
import com.android.launcherkiosk.ui.screenRoot

class HomeFragment : Fragment() {
    private lateinit var repository: KioskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = KioskRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val root = screenRoot(context)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(Button(context).apply {
            text = "设置"
            setOnClickListener { showAdminPasswordDialog() }
        })
        root.addView(header)

        val scroll = ScrollView(context)
        val grid = GridLayout(context).apply {
            columnCount = 3
            useDefaultMargins = true
        }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val apps = repository.getWhitelist()
        if (apps.isEmpty()) {
            root.addView(bodyView(context, "当前没有白名单应用，请进入管理员后台配置。"))
        } else {
            apps.forEach { app ->
                val item = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    val padding = context.dp(8)
                    setPadding(padding, padding, padding, padding)
                    setOnClickListener { launchApp(app.packageName) }
                }
                val icon = ImageView(context).apply {
                    setImageDrawable(runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull())
                }
                item.addView(icon, LinearLayout.LayoutParams(context.dp(54), context.dp(54)))
                item.addView(TextView(context).apply {
                    text = app.appName
                    gravity = Gravity.CENTER
                    maxLines = 2
                    textSize = 13f
                })
                grid.addView(item, ViewGroup.LayoutParams(context.dp(104), context.dp(118)))
            }
        }
        return root
    }

    private fun showAdminPasswordDialog() {
        val context = requireContext()
        val input = EditText(context).apply {
            hint = "管理员密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(context).setTitle("管理员验证").setView(input)
            .setNegativeButton("取消", null).setPositiveButton("进入设置") { _, _ ->
                if (repository.verifyPassword(input.text.toString())) {
                    startActivity(Intent(context, AdminSettingsActivity::class.java))
                } else {
                    Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun launchApp(packageName: String) {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Toast.makeText(requireContext(), "无法启动该应用", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
