package com.android.launcherkiosk.ui.whitelist

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.data.InstalledApp
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.data.WhitelistApp
import com.android.launcherkiosk.ui.admin.AdminSettingsActivity
import com.android.launcherkiosk.ui.actionButton
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.dp
import com.android.launcherkiosk.ui.screenRoot
import com.android.launcherkiosk.ui.titleView
import com.android.launcherkiosk.util.AppIconLoader
import com.android.launcherkiosk.util.InstalledAppLoader

class AppWhitelistFragment : Fragment() {
    private lateinit var repository: KioskRepository
    private val checkedApps = linkedMapOf<String, Pair<InstalledApp, CheckBox>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = KioskRepository(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = screenRoot(context).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(titleView(context, "应用白名单"))
        root.addView(bodyView(context, "选择允许用户从 Kiosk 主页启动的应用。"))

        val current = repository.getWhitelist().map { it.packageName }.toSet()
        val apps = InstalledAppLoader(context).loadLaunchableApps()
        val scroll = ScrollView(context)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        apps.forEach { app ->
            val checkbox = CheckBox(context).apply {
                isChecked = app.packageName in current
            }
            checkedApps[app.packageName] = app to checkbox
            list.addView(appRow(app, checkbox))
        }

        root.addView(actionButton(context, "保存白名单") {
            val selected = checkedApps.values.mapIndexedNotNull { index, pair ->
                val app = pair.first
                val checkbox = pair.second
                if (!checkbox.isChecked) null else WhitelistApp(app.packageName, app.appName, true, index)
            }
            repository.setWhitelist(selected)
            Toast.makeText(context, "白名单已保存", Toast.LENGTH_SHORT).show()
            if (arguments?.getBoolean(ARG_FROM_SETUP) == true) {
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                startActivity(android.content.Intent(context, AdminSettingsActivity::class.java))
            }
        })
        return root
    }

    private fun appRow(app: InstalledApp, checkbox: CheckBox): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val horizontal = context.dp(4)
            val vertical = context.dp(8)
            setPadding(horizontal, vertical, horizontal, vertical)
            minimumHeight = context.dp(64)
            setOnClickListener { checkbox.isChecked = !checkbox.isChecked }

            addView(ImageView(context).apply {
                setImageDrawable(AppIconLoader.loadBoundedIcon(context, app.packageName, ICON_SIZE_DP))
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = false
            }, LinearLayout.LayoutParams(context.dp(ICON_SIZE_DP), context.dp(ICON_SIZE_DP)))

            addView(TextView(context).apply {
                text = app.appName
                textSize = 16f
                setTextColor(0xFF111827.toInt())
                maxLines = 2
                setPadding(context.dp(14), 0, context.dp(8), 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))

            addView(checkbox, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
        }
    }

    companion object {
        private const val ARG_FROM_SETUP = "from_setup"
        private const val ICON_SIZE_DP = 44

        fun newInstance(fromSetup: Boolean): AppWhitelistFragment = AppWhitelistFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_FROM_SETUP, fromSetup) }
        }
    }
}
