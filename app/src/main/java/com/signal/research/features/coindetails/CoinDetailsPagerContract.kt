import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.features.BaseView

/**
Created by Pranay Airan
 */

interface CoinDetailsPagerContract {

    interface View : BaseView {
        fun onWatchedCoinsLoaded(watchedCoinList: List<WatchedCoin>?)
    }

    interface Presenter {
        fun loadWatchedCoins()
    }
}
