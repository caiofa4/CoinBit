package com.signal.research.features.settings

import SettingsContract
import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.features.BasePresenter
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.network.models.getCoinFromCCCoin
import com.signal.research.utils.defaultExchange
import kotlinx.coroutines.launch
import timber.log.Timber

/**
Created by Pranay Airan
 */

class SettingsPresenter(
    private val coinRepo: CryptoCompareRepository
) : BasePresenter<SettingsContract.View>(), SettingsContract.Presenter {

    override fun refreshCoinList(defaultCurrency: String) {
        launch {
            try {
                val allCoinsFromAPI = coinRepo.getAllCoinsFromAPI()
                val coinList: MutableList<WatchedCoin> = mutableListOf()
                val ccCoinList = allCoinsFromAPI.first
                ccCoinList.forEach { ccCoin ->
                    val coinInfo = allCoinsFromAPI.second[ccCoin.symbol.toLowerCase()]
                    coinList.add(getCoinFromCCCoin(ccCoin, defaultExchange, defaultCurrency, coinInfo))
                }
                Timber.d("Inserted all coins in db with size ${coinList.size}")
                currentView?.onCoinListRefreshed()
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
                currentView?.onNetworkError(ex.localizedMessage ?: "")
            }
        }
    }

    override fun refreshExchangeList() {
        launch {
            try {
                coinRepo.insertExchangeIntoList(coinRepo.getExchangeInfo())
                Timber.d("all exchanges loaded and inserted into db")
                currentView?.onExchangeListRefreshed()
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
                currentView?.onNetworkError(ex.localizedMessage ?: "")
            }
        }
    }
}
