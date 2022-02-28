package com.signal.research.features.transactiondetails

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.signal.research.CoinBitApplication
import com.signal.research.R
import com.signal.research.data.database.entities.Coin
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.epoxymodels.CoinTransactionHistoryItemView
import com.signal.research.epoxymodels.coinTransactionHistoryView
import com.signal.research.featurecomponents.transactiondetailsmodule.TransactionDetailsPresenter
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.features.transaction.CoinTransactionActivity
import kotlinx.android.synthetic.main.activity_news_list.*
import kotlinx.android.synthetic.main.activity_transaction_details.*

class TransactionDetailsActivity : AppCompatActivity(), TransactionDetailsContract.View {

    private var coin: Coin? = null
    var coinTransactions: CoinTransactionHistoryItemView.CoinTransactionHistoryModuleData? = null
    private var selectedIndex = 0

    private val coinRepo by lazy {
        CryptoCompareRepository(CoinBitApplication.database)
    }

    private val transactionDetailsPresenter: TransactionDetailsPresenter by lazy {
        TransactionDetailsPresenter(coinRepo)
    }

    companion object {
        private const val COIN_SYMBOL = "COIN_SYMBOL"
        private const val COIN = "COIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_details)

        val toolbar = findViewById<View>(R.id.transactionDetailsToolbar)
        setSupportActionBar(toolbar as Toolbar?)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        coin = intent.getParcelableExtra(COIN)
        val coinSymbol = intent.getStringExtra(COIN_SYMBOL)

        supportActionBar?.title = getString(R.string.transactionsDetailsActivityTitle, coinSymbol)

        transactionDetailsPresenter.attachView(this)

        coinSymbol?.let {
            transactionDetailsPresenter.loadRecentTransaction(it)
        }

        FirebaseCrashlytics.getInstance().log("TransactionDetailsActivity")
    }

    private fun openTransaction(coinTransaction: CoinTransaction) {
        coin?.let {
            val intent = Intent(this, CoinTransactionActivity::class.java)
            intent.putExtra(CoinTransactionActivity.COIN, it)
            intent.putExtra(CoinTransactionActivity.NEW_TRANSACTION, false)
            intent.putExtra(CoinTransactionActivity.COIN_TRANSACTION, coinTransaction)
            startActivity(intent)
        } ?: Toast.makeText(this, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
    }

    override fun showOrHideLoadingIndicator(showLoading: Boolean) {
        if (!showLoading) {
            pbLoadingTransactionDetails.hide()
        } else {
            pbLoadingTransactionDetails.show()
        }
    }

    override fun onTransactionsLoaded(coinTransactions: List<CoinTransaction>) {
        val reversedCoinTransactions = coinTransactions.reversed()
        rvTransactionDetails.withModels {
            reversedCoinTransactions.forEachIndexed { index, coinTransaction ->
                coinTransactionHistoryView {
                    id(index)
                    coinTransaction(coinTransaction)
                    moreClickListener { _ ->
                        selectedIndex = index
                        openTransaction(coinTransaction)
                    }
                }
            }
        }
    }

    override fun onNetworkError(errorMessage: String) {
        Snackbar.make(rvNewsList, errorMessage, Snackbar.LENGTH_LONG).show()
    }


}