package com.signal.research.featurecomponents.transactiondetailsmodule

import TransactionDetailsContract
import com.signal.research.features.BasePresenter
import kotlinx.coroutines.launch
import com.signal.research.features.CryptoCompareRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class TransactionDetailsPresenter(private val coinRepo: CryptoCompareRepository) :
    BasePresenter<TransactionDetailsContract.View>(), TransactionDetailsContract.Presenter {

    override fun loadRecentTransaction(symbol: String) {
        currentView?.showOrHideLoadingIndicator(true)

        launch {
            coinRepo.getRecentTransaction(symbol)
                ?.catch {
                    Timber.e(it.localizedMessage)
                    currentView?.showOrHideLoadingIndicator(false)
                }
                ?.collect { coinTransactionsList ->
                    coinTransactionsList.let {
                        currentView?.onTransactionsLoaded(it)
                        currentView?.showOrHideLoadingIndicator(false)
                    }
                }
        }
    }

}