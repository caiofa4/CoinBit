import com.signal.research.features.BaseView
import com.signal.research.network.models.CryptoTicker

/**
 * Created by Pranay Airan
 */

interface CoinTickerContract {

    interface View : BaseView {
        fun showOrHideLoadingIndicatorForTicker(showLoading: Boolean = true)
        fun onPriceTickersLoaded(tickerData: List<CryptoTicker>)
    }

    interface Presenter {
        fun getCryptoTickers(coinName: String)
    }
}
