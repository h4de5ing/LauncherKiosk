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
import com.android.launcherkiosk.R
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.ui.admin.AdminSettingsActivity
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.dp
import com.android.launcherkiosk.ui.screenRoot
import com.android.launcherkiosk.util.AppIconLoader

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
        header.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(Button(context).apply {
            text = getString(R.string.settings)
            setOnClickListener { showAdminPasswordDialog() }
        })
        root.addView(header)

        val scroll = ScrollView(context)
        val grid = GridLayout(context).apply {
            columnCount = 5
            useDefaultMargins = false
        }
        scroll.addView(grid, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val apps = repository.getWhitelist()
        if (apps.isEmpty()) {
            root.addView(bodyView(context, getString(R.string.no_whitelist_apps)))
        } else {
            apps.forEach { app ->
                val item = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    val padding = context.dp(2)
                    setPadding(padding, padding, padding, padding)
                    setOnClickListener { launchApp(app.packageName) }
                }
                val icon = ImageView(context).apply {
                    setImageDrawable(AppIconLoader.loadBoundedIcon(context, app.packageName, 48))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                item.addView(icon, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
                item.addView(TextView(context).apply {
                    text = app.appName
                    gravity = Gravity.CENTER
                    maxLines = 2
                    textSize = 13f
                    setTextColor(0xFFFFFFFF.toInt())
                    setShadowLayer(3f, 0f, 1f, 0xAA000000.toInt())
                })
                grid.addView(item, GridLayout.LayoutParams().apply {
                    width = 0
                    height = context.dp(98)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                })
            }
        }
        return root
    }

    private fun showAdminPasswordDialog() {
        val context = requireContext()
        val input = EditText(context).apply {
            hint = getString(R.string.admin_password)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(context).setTitle(R.string.admin_verification).setView(input)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.open_settings) { _, _ ->
                if (repository.verifyPassword(input.text.toString())) {
                    startActivity(Intent(context, AdminSettingsActivity::class.java))
                } else {
                    Toast.makeText(context, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun launchApp(packageName: String) {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Toast.makeText(requireContext(), R.string.unable_launch_app, Toast.LENGTH_SHORT).show()
        } else {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
