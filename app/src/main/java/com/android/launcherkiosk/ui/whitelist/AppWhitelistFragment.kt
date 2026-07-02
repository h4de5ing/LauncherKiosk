package com.android.launcherkiosk.ui.whitelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.MainActivity
import com.android.launcherkiosk.data.InstalledApp
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.data.WhitelistApp
import com.android.launcherkiosk.ui.actionButton
import com.android.launcherkiosk.ui.bodyView
import com.android.launcherkiosk.ui.dp
import com.android.launcherkiosk.ui.screenRoot
import com.android.launcherkiosk.ui.titleView
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
        val root = screenRoot(context)
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
                text = app.appName
                isChecked = app.packageName in current
                compoundDrawablePadding = context.dp(12)
                setCompoundDrawablesWithIntrinsicBounds(app.icon, null, null, null)
            }
            checkedApps[app.packageName] = app to checkbox
            list.addView(checkbox)
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
                (requireActivity() as MainActivity).showAdminPanel()
            }
        })
        return root
    }

    companion object {
        private const val ARG_FROM_SETUP = "from_setup"

        fun newInstance(fromSetup: Boolean): AppWhitelistFragment = AppWhitelistFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_FROM_SETUP, fromSetup) }
        }
    }
}
