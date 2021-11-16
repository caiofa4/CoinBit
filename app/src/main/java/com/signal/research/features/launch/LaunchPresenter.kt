package com.signal.research.features.launch

import LaunchContract
import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.features.BasePresenter
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.features.getTop5CoinsToWatch
import com.signal.research.network.models.CCCoin
import com.signal.research.network.models.CoinInfo
import com.signal.research.network.models.getCoinFromCCCoin
import com.signal.research.utils.defaultExchange
import kotlinx.coroutines.launch
import timber.log.Timber

/**
Created by Pranay Airan
 */

class LaunchPresenter(
    private val coinRepo: CryptoCompareRepository
) : BasePresenter<LaunchContract.View>(), LaunchContract.Presenter {

    private var coinList: ArrayList<CCCoin>? = null
    private var coinInfoMap: Map<String, CoinInfo>? = null

    override fun loadAllCoins() {
        launch {
            try {
                val allCoinsFromAPI = coinRepo.getAllCoinsFromAPI(coinList, coinInfoMap)
                coinList = allCoinsFromAPI.first
                coinInfoMap = allCoinsFromAPI.second
                currentView?.onCoinsLoaded()
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }

        loadExchangeFromAPI()
    }

    private fun loadExchangeFromAPI() {
        launch {
            try {
                coinRepo.insertExchangeIntoList(coinRepo.getExchangeInfo())
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }

    override fun getAllSupportedCoins(defaultCurrency: String) {
        launch {
            try {
                val allCoinsFromAPI = coinRepo.getAllCoinsFromAPI(coinList, coinInfoMap)
                val coinList: MutableList<WatchedCoin> = mutableListOf()
                val ccCoinList = allCoinsFromAPI.first

                ccCoinList.forEach { ccCoin ->
                    val coinInfo = allCoinsFromAPI.second[ccCoin.symbol.toLowerCase()]
                    coinList.add(getCoinFromCCCoin(ccCoin, defaultExchange, defaultCurrency, coinInfo))
                }

                coinRepo.insertCoinsInWatchList(coinList)

                val top5CoinsToWatch = getTop5CoinsToWatch()
                top5CoinsToWatch.forEach { coinId ->
                    coinRepo.updateCoinWatchedStatus(true, coinId)
                }

                Timber.d("Loaded all the coins and inserted in DB")
                currentView?.onAllSupportedCoinsLoaded()
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }
}
