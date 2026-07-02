package com.android.launcherkiosk

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.ui.admin.AdminLoginFragment
import com.android.launcherkiosk.ui.admin.AdminPanelFragment
import com.android.launcherkiosk.ui.home.HomeFragment
import com.android.launcherkiosk.ui.setup.PermissionGuideFragment
import com.android.launcherkiosk.ui.setup.SetupWizardFragment
import com.android.launcherkiosk.ui.whitelist.AppWhitelistFragment

class MainActivity : AppCompatActivity() {
    private lateinit var repository: KioskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repository = KioskRepository(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (current is HomeFragment || current is SetupWizardFragment) return
                supportFragmentManager.popBackStack()
            }
        })

        if (savedInstanceState == null) {
            if (repository.getSettings().setupCompleted) showHome() else showSetup()
        }
    }

    fun showSetup() = replace(SetupWizardFragment(), clearBackStack = true)
    fun showHome() = replace(HomeFragment(), clearBackStack = true)
    fun showAdminLogin() = replace(AdminLoginFragment(), addToBackStack = true)
    fun showAdminPanel() = replace(AdminPanelFragment(), addToBackStack = true)
    fun showWhitelist(fromSetup: Boolean = false) = replace(AppWhitelistFragment.newInstance(fromSetup), addToBackStack = true)
    fun showPermissions() = replace(PermissionGuideFragment(), addToBackStack = true)

    private fun replace(fragment: Fragment, addToBackStack: Boolean = false, clearBackStack: Boolean = false) {
        if (clearBackStack) {
            while (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStackImmediate()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .apply { if (addToBackStack) addToBackStack(fragment::class.java.simpleName) }
            .commit()
    }
}
