import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.features.BaseView

interface TransactionDetailsContract {
    interface View : BaseView {
        fun showOrHideLoadingIndicator(showLoading: Boolean = true)
        fun onTransactionsLoaded(coinTransactions: List<CoinTransaction>)
    }

    interface Presenter {
        fun loadRecentTransaction(symbol: String)
    }
}