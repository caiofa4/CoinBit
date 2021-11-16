package com.signal.research.features.coindetails

import CoinDetailsPagerContract
import com.signal.research.features.BasePresenter
import kotlinx.coroutines.launch
import timber.log.Timber

/**
Created by Pranay Airan
 */

class CoinDetailPagerPresenter(private val coinDetailsPagerRepository: CoinDetailsPagerRepository) :
    BasePresenter<CoinDetailsPagerContract.View>(), CoinDetailsPagerContract.Presenter {
    override fun loadWatchedCoins() {
        launch {
            try {
                currentView?.onWatchedCoinsLoaded(coinDetailsPagerRepository.loadWatchedCoins())
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
            }
        }
    }
}
