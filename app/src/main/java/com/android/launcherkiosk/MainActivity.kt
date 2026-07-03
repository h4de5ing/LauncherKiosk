package com.android.launcherkiosk

import android.app.WallpaperManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.android.launcherkiosk.data.KioskRepository
import com.android.launcherkiosk.ui.home.HomeFragment
import com.android.launcherkiosk.ui.setup.SetupWizardFragment
import com.android.launcherkiosk.ui.whitelist.AppWhitelistFragment

class MainActivity : AppCompatActivity() {
    private lateinit var repository: KioskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWallpaperAndSystemBars()
        setContentView(R.layout.activity_main)
        applyContentInsets()
        repository = KioskRepository(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (current is HomeFragment || current is SetupWizardFragment) return
                supportFragmentManager.popBackStack()
            }
        })

        if (savedInstanceState == null) {
            handleStartIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStartIntent(intent)
    }

    fun showSetup() = replace(SetupWizardFragment(), clearBackStack = true)
    fun showHome() = replace(HomeFragment(), clearBackStack = true)
    fun showWhitelist(fromSetup: Boolean = false) =
        replace(AppWhitelistFragment.newInstance(fromSetup), addToBackStack = true)

    private fun handleStartIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_WHITELIST -> showWhitelist()
            else -> if (repository.getSettings().adminPasswordHash.isBlank()) showSetup() else showHome()
        }
    }

    private fun replace(
        fragment: Fragment, addToBackStack: Boolean = false, clearBackStack: Boolean = false
    ) {
        if (clearBackStack) {
            while (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStackImmediate()
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .apply { if (addToBackStack) addToBackStack(fragment::class.java.simpleName) }.commit()
    }

    private fun configureWallpaperAndSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.decorView.background = Color.TRANSPARENT.toDrawable()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // Keep the live system wallpaper visible behind this launcher window.
        WallpaperManager.getInstance(this)

        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val useDarkSystemBarIcons = nightMode != Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkSystemBarIcons
            isAppearanceLightNavigationBars = useDarkSystemBarIcons
        }
    }

    private fun applyContentInsets() {
        val container = findViewById<android.view.View>(R.id.fragment_container)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    companion object {
        const val ACTION_OPEN_WHITELIST = "com.android.launcherkiosk.OPEN_WHITELIST"
    }
}
