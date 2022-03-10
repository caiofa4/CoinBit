import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.features.BaseView
import com.signal.research.network.models.CoinPrice
import com.signal.research.network.models.CryptoCompareHistoricalResponse
import com.signal.research.network.models.CryptoCompareNews

/**
Created by Pranay Airan
 */

interface CoinDashboardContract {

    interface View : BaseView {
        fun onWatchedCoinsAndTransactionsLoaded(watchedCoinList: List<WatchedCoin>, coinTransactionList: List<CoinTransaction>)
        fun onCoinPricesLoaded(coinPriceListMap: HashMap<String, CoinPrice>)
        fun onTopCoinsByTotalVolumeLoaded(topCoins: List<CoinPrice>)
        fun onCoinNewsLoaded(coinNews: List<CryptoCompareNews>)
        fun onAllCoinTransactionsLoaded(coinTransactionList: List<CoinTransaction>)
        fun onHistoricalDataLoaded(coinSymbol: String, period: String, numberOfCoins: Int, historicalDataPair: Pair<List<CryptoCompareHistoricalResponse.Data>, CryptoCompareHistoricalResponse.Data?>)
    }

    interface Presenter {
        fun loadWatchedCoinsAndTransactions()
        fun loadCoinsPrices(fromCurrencySymbol: String, toCurrencySymbol: String)
        fun getTopCoinsByTotalVolume24hours(toCurrencySymbol: String)
        fun getLatestNewsFromCryptoCompare()
        fun loadAllCoinTransactions()
        fun loadHistoricalData(period: String, fromCurrency: String, toCurrency: String, numberOfCoins: Int)
    }
}
