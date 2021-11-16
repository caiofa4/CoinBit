package com.signal.research.features.coindetails

import CoinDetailsContract
import com.signal.research.features.BasePresenter
import com.signal.research.features.CryptoCompareRepository
import kotlinx.coroutines.launch
import timber.log.Timber

/**
Created by Pranay Airan
 */

class CoinDetailPresenter(
    private val coinRepo: CryptoCompareRepository
) : BasePresenter<CoinDetailsContract.View>(),
    CoinDetailsContract.Presenter {

    override fun getWatchedCoinFromSymbol(symbol: String) {

        currentView?.showOrHideLoadingIndicator(true)

        launch {
            try {
                val singleCoin = coinRepo.getSingleCoin(symbol)
                Timber.d("watched coin loaded")
                currentView?.showOrHideLoadingIndicator(false)
                if (singleCoin != null) {
                    currentView?.onWatchedCoinLoaded(singleCoin.first())
                } else {
                    currentView?.onWatchedCoinLoaded(null)
                }
            } catch (ex: Exception) {
                Timber.e(ex.localizedMessage)
                currentView?.onNetworkError(ex.localizedMessage)
            }
        }
    }
}
