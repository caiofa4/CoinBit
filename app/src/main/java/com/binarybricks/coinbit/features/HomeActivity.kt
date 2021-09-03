package com.binarybricks.coinbit.features

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.binarybricks.coinbit.R
import com.binarybricks.coinbit.features.coinsearch.CoinDiscoveryFragment
import com.binarybricks.coinbit.features.dashboard.CoinDashboardFragment
import com.binarybricks.coinbit.features.settings.SettingsFragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun buildLaunchIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }

        const val FRAGMENT_HOME = "FRAGMENT_HOME"
        const val FRAGMENT_OTHER = "FRAGMENT_OTHER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        switchToDashboard(savedInstanceState)

        // if fragment exist reuse it
        // if not then add it

        tvHome.setOnClickListener {
            switchToDashboard(savedInstanceState)
        }
        tvSearchCoins.setOnClickListener {
            switchToSearch(savedInstanceState)
        }

//        bottomNavigation.setOnNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.actionHome -> {
//                    switchToDashboard(savedInstanceState)
//                }
//
//                R.id.actionSearch -> {
//                    switchToSearch(savedInstanceState)
//                }
//
////                R.id.actionSettings -> {
////                    switchToSettings(savedInstanceState)
////                }
//            }
//            return@setOnNavigationItemSelectedListener true
//        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                finish()
            } else if (!supportFragmentManager.fragments.isNullOrEmpty()) {
                when (supportFragmentManager.fragments[0]) {
                    is CoinDashboardFragment -> {
                        tvHome.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                        tvSearchCoins.paintFlags = 0
                        tvTitle.text = getString(R.string.market)
                    }
                    is CoinDiscoveryFragment -> {
                        tvSearchCoins.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                        tvHome.paintFlags = 0
                        tvTitle.text = getString(R.string.search)
                    }
                    //is SettingsFragment -> bottomNavigation.menu.getItem(2).isChecked = true
                }
            }
        }

        FirebaseCrashlytics.getInstance().log("HomeScreen")
    }

    private fun switchToDashboard(savedInstanceState: Bundle?) {

        val coinDashboardFragment = supportFragmentManager.findFragmentByTag(CoinDashboardFragment.TAG)
            ?: CoinDashboardFragment()

        // if we switch to home clear everything
        supportFragmentManager.popBackStack(FRAGMENT_OTHER, POP_BACK_STACK_INCLUSIVE)

        supportFragmentManager.beginTransaction()
            .replace(R.id.containerLayout, coinDashboardFragment, CoinDashboardFragment.TAG)
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .addToBackStack(FRAGMENT_HOME)
            .commit()
    }

    private fun switchToSearch(savedInstanceState: Bundle?) {

        val coinDiscoveryFragment = supportFragmentManager.findFragmentByTag(CoinDiscoveryFragment.TAG)
            ?: CoinDiscoveryFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.containerLayout, coinDiscoveryFragment, CoinDiscoveryFragment.TAG)
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .addToBackStack(FRAGMENT_OTHER)
            .commit()
    }

    private fun switchToSettings(savedInstanceState: Bundle?) {

        val settingsFragment = supportFragmentManager.findFragmentByTag(SettingsFragment.TAG)
            ?: SettingsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.containerLayout, settingsFragment, SettingsFragment.TAG)
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .addToBackStack(FRAGMENT_OTHER)
            .commit()
    }
}
