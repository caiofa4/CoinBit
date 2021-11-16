package com.signal.research.features.transaction

import CoinTransactionContract
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.features.BasePresenter
import com.signal.research.features.CryptoCompareRepository
import kotlinx.coroutines.launch
import timber.log.Timber

/**
Created by Pranay Airan
 */

class CoinTransactionPresenter(
    private val coinRepo: CryptoCompareRepository
) : BasePresenter<CoinTransactionContract.View>(), CoinTransactionContract.Presenter {

    override fun getAllSupportedExchanges() {
        launch {
            try {
                currentView?.onAllSupportedExchangesLoaded(coinRepo.getAllSupportedExchanges())
                Timber.d("All Exchange Loaded")
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }

    // to coins is , separated multiple coin list.
    override fun getPriceForPair(fromCoin: String, toCoin: String, exchange: String, timeStamp: String) {
        if (exchange.isNotEmpty()) {
            launch {
                try {
                    currentView?.onCoinPriceLoaded(coinRepo.getCoinPriceForTimeStamp(fromCoin, toCoin, exchange, timeStamp))
                } catch (ex: Exception) {
                    Timber.e(ex.localizedMessage)
                }
            }
        }
    }

    override fun addTransaction(transaction: CoinTransaction) {
        launch {
            try {
                coinRepo.insertTransaction(transaction)
                Timber.d("Coin Transaction Added")
                currentView?.onTransactionAdded()
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }
}
