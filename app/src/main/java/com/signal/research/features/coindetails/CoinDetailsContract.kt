import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.features.BaseView

/**
Created by Pranay Airan
 */

interface CoinDetailsContract {

    interface View : BaseView {
        fun showOrHideLoadingIndicator(showLoading: Boolean = true)
        fun onWatchedCoinLoaded(coin: WatchedCoin?)
    }

    interface Presenter {
        fun getWatchedCoinFromSymbol(symbol: String)
    }
}
