import com.signal.research.features.BaseView
import com.signal.research.network.models.CoinPair
import com.signal.research.network.models.CoinPrice
import com.signal.research.network.models.CryptoCompareNews

/**
Created by Pranay Airan
 */

interface CoinDiscoveryContract {

    interface View : BaseView {
        fun onTopCoinsByTotalVolumeLoaded(topCoins: List<CoinPrice>)
        fun onTopCoinListByPairVolumeLoaded(topPair: List<CoinPair>)
        fun onCoinNewsLoaded(coinNews: List<CryptoCompareNews>)
    }

    interface Presenter {
        fun getTopCoinListByMarketCap(toCurrencySymbol: String)
        fun getTopCoinListByPairVolume()
        fun getCryptoCurrencyNews()
    }
}
