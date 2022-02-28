package com.signal.research.features.coindetails

import CoinDetailsPagerContract
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.signal.research.CoinBitApplication
import com.signal.research.R
import com.signal.research.data.database.entities.WatchedCoin
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.signal.research.features.coin.CoinFragment
import com.signal.research.features.transaction.CoinTransactionActivity
import kotlinx.android.synthetic.main.activity_pager_coin_details.*

class CoinDetailsPagerActivity : AppCompatActivity(), CoinDetailsPagerContract.View {

    private var watchedCoin: WatchedCoin? = null
    var isCoinInfoChanged = false

    private val allCoinDetailsRepository by lazy {
        CoinDetailsPagerRepository(CoinBitApplication.database)
    }

    private val coinDetailPagerPresenter: CoinDetailPagerPresenter by lazy {
        CoinDetailPagerPresenter(allCoinDetailsRepository)
    }

    companion object {
        const val WATCHED_COIN = "WATCHED_COIN"
        private const val COIN_TRANSACTION_CODE = 100

        @JvmStatic
        fun buildLaunchIntent(context: Context, watchedCoin: WatchedCoin): Intent {
            val intent = Intent(context, CoinDetailsPagerActivity::class.java)
            intent.putExtra(WATCHED_COIN, watchedCoin)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager_coin_details)

        coinDetailsPagerToolbar.elevation = 0f

        val toolbar = findViewById<View>(R.id.coinDetailsPagerToolbar)
        setSupportActionBar(toolbar as Toolbar?)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        watchedCoin = intent.getParcelableExtra(WATCHED_COIN)

        coinDetailPagerPresenter.attachView(this)

        lifecycle.addObserver(coinDetailPagerPresenter)

        showOrHideLoadingIndicator(true)

        coinDetailPagerPresenter.loadWatchedCoins()

        clFooterCoinDetailsPager.setOnClickListener {
            addTransaction()
        }

        FirebaseCrashlytics.getInstance().log("CoinDetailsPagerActivity")
    }

    private fun addTransaction() {
        watchedCoin?.coin?.let {
            val intent = Intent(this, CoinTransactionActivity::class.java)
            intent.putExtra(CoinTransactionActivity.COIN, it)
            startActivityForResult(intent, COIN_TRANSACTION_CODE)
        } ?: Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
    }

    override fun onWatchedCoinsLoaded(watchedCoinList: List<WatchedCoin>?) {
        supportActionBar?.title = getString(
            R.string.transactionTypeWithQuantity,
            watchedCoin?.coin?.coinName, watchedCoin?.coin?.symbol
        )

        watchedCoin?.let {
            val coinDetailsFragment = CoinFragment()
            coinDetailsFragment.arguments = CoinFragment.getArgumentBundle(it)

            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.vpCoins, coinDetailsFragment)
            fragmentTransaction.commit()

            supportActionBar?.title = getString(
                R.string.transactionTypeWithQuantity,
                it.coin.coinName, it.coin.symbol
            )
        }



        //val allCoinsPagerAdapter = CoinDetailsPagerAdapter(watchedCoinList, supportFragmentManager)
        //vpCoins.adapter = allCoinsPagerAdapter

        showOrHideLoadingIndicator(false)

//        watchedCoinList?.forEachIndexed { index, watch ->
//            if (watchedCoin?.coin?.name == watch.coin.name) {
//                vpCoins.currentItem = index
//            }
//        }
//
//        vpCoins.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
//            override fun onPageScrollStateChanged(state: Int) {
//            }
//
//            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//            }
//
//            override fun onPageSelected(position: Int) {
//                supportActionBar?.title = watchedCoinList?.get(position)?.coin?.coinName
//            }
//        })
    }

    override fun onNetworkError(errorMessage: String) {
        //Snackbar.make(rvCoinDetails, errorMessage, Snackbar.LENGTH_LONG).show()
    }

    private fun showOrHideLoadingIndicator(showLoading: Boolean) {
        if (showLoading) {
            pbLoading2.visibility = View.VISIBLE
        } else {
            pbLoading2.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (isCoinInfoChanged) {
            setResult(Activity.RESULT_OK)
        }

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (COIN_TRANSACTION_CODE == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                //isCoinInfoChanged = true
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
