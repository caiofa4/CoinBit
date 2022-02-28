import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.features.BaseView
import com.signal.research.network.models.ExchangePair
import java.math.BigDecimal
import java.util.HashMap

/**
Created by Pranay Airan 2/3/18.
 */

interface CoinTransactionContract {

    interface View : BaseView {
        fun onAllSupportedExchangesLoaded(exchangeCoinMap: HashMap<String, MutableList<ExchangePair>>)
        fun onCoinPriceLoaded(prices: MutableMap<String, BigDecimal>)
        fun onTransactionAdded()
        fun onTransactionDeleted()
        fun onTransactionUpdated()    }

    interface Presenter {
        fun getAllSupportedExchanges()
        fun getPriceForPair(fromCoin: String, toCoin: String, exchange: String, timeStamp: String)
        fun addTransaction(transaction: CoinTransaction)
        fun deleteTransaction(transaction: CoinTransaction)
        fun updateTransaction(transaction: CoinTransaction, previousQuantity: BigDecimal, previousTransactionType: Int)
    }
}
