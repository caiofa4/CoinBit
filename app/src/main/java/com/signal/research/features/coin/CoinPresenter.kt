package com.signal.research.features.coin

import CoinContract
import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.featurecomponents.historicalchartmodule.ChartRepository
import com.signal.research.features.BasePresenter
import com.signal.research.features.CryptoCompareRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

/**
Created by Pranay Airan
 */

class CoinPresenter(
    private val coinRepo: CryptoCompareRepository,
    private val chartRepo: ChartRepository
) : BasePresenter<CoinContract.View>(), CoinContract.Presenter {

    /**
     * Get the current price of a coinSymbol say btc or eth
     */
    override fun loadCurrentCoinPrice(watchedCoin: WatchedCoin, toCurrency: String) {
        launch {
            try {
                currentView?.onCoinPriceLoaded(coinRepo.getCoinPriceFull(watchedCoin.coin.symbol, toCurrency), watchedCoin)
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }

    override fun loadRecentTransaction(symbol: String) {
        launch {
            coinRepo.getRecentTransaction(symbol)
                ?.catch {
                    Timber.e(it.localizedMessage)
                }
                ?.collect { coinTransactionsList ->
                    coinTransactionsList?.let {
                        currentView?.onRecentTransactionLoaded(it)
                    }
                }
        }
    }

    override fun updateCoinWatchedStatus(watched: Boolean, coinID: String, coinSymbol: String) {

        launch {
            try {
                coinRepo.updateCoinWatchedStatus(watched, coinID)
                Timber.d("Coin status updated")
                currentView?.onCoinWatchedStatusUpdated(watched, coinSymbol)
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
                currentView?.onNetworkError(ex.localizedMessage ?: "Error")
            }
        }
    }

    /**
     * Load historical data for the coin to show the chart.
     */
    override fun loadHistoricalData(period: String, fromCurrency: String, toCurrency: String) {
        launch {
            try {
                currentView?.onHistoricalDataLoaded(period, chartRepo.getCryptoHistoricalData(period, fromCurrency, toCurrency))
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }
}
