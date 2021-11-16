import com.signal.research.features.BaseView
import com.signal.research.network.models.CryptoPanicNews

/**
 * Created by Pragya Agrawal
 */

interface CryptoNewsContract {

    interface View : BaseView {
        fun showOrHideLoadingIndicator(showLoading: Boolean = true)
        fun onNewsLoaded(cryptoPanicNews: CryptoPanicNews)
    }

    interface Presenter {
        fun getCryptoNews(coinSymbol: String)
    }
}
