package com.signal.research.features.coindetails

import CoinDetailsContract
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.signal.research.CoinBitApplication
import com.signal.research.R
import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.features.coin.CoinFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.signal.research.features.transaction.CoinTransactionActivity
import kotlinx.android.synthetic.main.activity_coin_details.*

class CoinDetailsActivity : AppCompatActivity(), CoinDetailsContract.View {

    private var watchedCoin: WatchedCoin? = null
    var isCoinInfoChanged = false

    companion object {
        const val WATCHED_COIN = "WATCHED_COIN"
        private const val COIN_SYMBOL = "COIN_SYMBOL"
        private const val COIN_TRANSACTION_CODE = 100

        @JvmStatic
        fun buildLaunchIntent(context: Context, watchedCoin: WatchedCoin): Intent {
            val intent = Intent(context, CoinDetailsActivity::class.java)
            intent.putExtra(WATCHED_COIN, watchedCoin)
            return intent
        }

        @JvmStatic
        fun buildLaunchIntent(context: Context, symbol: String): Intent {
            val intent = Intent(context, CoinDetailsActivity::class.java)
            intent.putExtra(COIN_SYMBOL, symbol)
            return intent
        }
    }

    private val coinRepo by lazy {
        CryptoCompareRepository(CoinBitApplication.database)
    }

    private val coinDetailPresenter: CoinDetailPresenter by lazy {
        CoinDetailPresenter(coinRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_details)

        val toolbar = findViewById<View>(R.id.coinDetailsToolbar)
        setSupportActionBar(toolbar as Toolbar?)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.elevation = 0f

        coinDetailPresenter.attachView(this)

        lifecycle.addObserver(coinDetailPresenter)

        watchedCoin = intent.getParcelableExtra(WATCHED_COIN)

        if (watchedCoin != null) {
            onWatchedCoinLoaded(watchedCoin)
        } else {
            intent.getStringExtra(COIN_SYMBOL)?.let {
                coinDetailPresenter.getWatchedCoinFromSymbol(it)
            }
        }

        clFooterCoinDetails.setOnClickListener {
            addTransaction()
        }

        FirebaseCrashlytics.getInstance().log("CoinDetailsActivity")
    }

    private fun addTransaction() {
        watchedCoin?.coin?.let {
            val intent = Intent(this, CoinTransactionActivity::class.java)
            intent.putExtra(CoinTransactionActivity.COIN, it)
            startActivityForResult(intent, COIN_TRANSACTION_CODE)
        } ?: Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
    }

    override fun onWatchedCoinLoaded(coin: WatchedCoin?) {
        coin?.let {
            watchedCoin = it
            showOrHideLoadingIndicator(false)

            val coinDetailsFragment = CoinFragment()
            coinDetailsFragment.arguments = CoinFragment.getArgumentBundle(it)

            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.flCoinDetails, coinDetailsFragment)
            fragmentTransaction.commit()

            supportActionBar?.title = getString(
                R.string.transactionTypeWithQuantity,
                it.coin.coinName, it.coin.symbol
            )
        }
    }

    override fun showOrHideLoadingIndicator(showLoading: Boolean) {
        if (!showLoading) {
            pbLoading3.hide()
        } else {
            pbLoading3.show()
        }
    }

    override fun onNetworkError(errorMessage: String) {
        Snackbar.make(flCoinDetails, errorMessage, Snackbar.LENGTH_LONG).show()
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
