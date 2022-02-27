package com.signal.research.features.transactiondetails

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.epoxymodels.CoinTransactionHistoryItemView
import com.signal.research.epoxymodels.coinTransactionHistoryView
import com.signal.research.utils.Formaters
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import kotlinx.android.synthetic.main.activity_transaction_details.*
import java.util.*

class TransactionDetailsActivity : AppCompatActivity() {

    companion object {
        private const val COIN_FULL_NAME = "COIN_FULL_NAME"
        private const val COIN_SYMBOL = "COIN_SYMBOL"
        private const val MODULE_ITEM = "MODULE_ITEM"

        @JvmStatic
        fun buildLaunchIntent(context: Context, coinName: String, coinSymbol: String): Intent {
            val intent = Intent(context, TransactionDetailsActivity::class.java)
            intent.putExtra(COIN_FULL_NAME, coinName)
            intent.putExtra(COIN_SYMBOL, coinSymbol)
            return intent
        }
    }

    private val androidResourceManager by lazy {
        AndroidResourceManagerImpl(this)
    }

    private val formatter: Formaters by lazy {
        Formaters(androidResourceManager)
    }

    private val currency: Currency by lazy {
        Currency.getInstance(PreferencesManager.getDefaultCurrency(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_details)

        val toolbar = findViewById<View>(R.id.transactionDetailsToolbar)
        setSupportActionBar(toolbar as Toolbar?)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val moduleItem = intent.getSerializableExtra(MODULE_ITEM)
        val coinSymbol = intent.getStringExtra(COIN_SYMBOL)?.trim()
        var coinTransactions: CoinTransactionHistoryItemView.CoinTransactionHistoryModuleData?

        supportActionBar?.title = getString(R.string.transactionsDetailsActivityTitle, coinSymbol)

        showOrHideLoadingIndicatorForTicker(true)

        moduleItem?.let { item ->
            coinTransactions = item as CoinTransactionHistoryItemView.CoinTransactionHistoryModuleData
            coinTransactions?.let {
                loadTransactionsOnScreen(it.coinTransactionList.reversed())
            }
        } ?: kotlin.run {
            showOrHideLoadingIndicatorForTicker(false)
        }

        FirebaseCrashlytics.getInstance().log("TransactionDetailsActivity")
    }

    fun showOrHideLoadingIndicatorForTicker(showLoading: Boolean) {
        if (!showLoading) {
            pbLoadingTransactionDetails.hide()
        } else {
            pbLoadingTransactionDetails.show()
        }
    }

    fun loadTransactionsOnScreen(coinTransactions: List<CoinTransaction>) {
        rvTransactionDetails.withModels {
            coinTransactions.forEachIndexed { index, coinTransaction ->
                coinTransactionHistoryView {
                    id(index)
                    coinTransaction(coinTransaction)
//                    tickerPrice(formatter.formatAmount(cryptoTicker.convertedVolumeUSD, currency, true))
//                    tickerVolume(formatter.formatAmount(cryptoTicker.last, currency, true))
                    moreClickListener { _ ->
                        Toast.makeText(applicationContext, "ola ${index}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        showOrHideLoadingIndicatorForTicker(false)
    }


}