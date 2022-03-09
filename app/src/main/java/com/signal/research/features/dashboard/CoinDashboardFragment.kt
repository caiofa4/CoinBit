package com.signal.research.features.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.carousel
import com.signal.research.CoinBitApplication
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.data.database.entities.WatchedCoin
import com.signal.research.epoxymodels.*
import com.signal.research.featurecomponents.ModuleItem
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.features.coindetails.CoinDetailsActivity
import com.signal.research.features.coindetails.CoinDetailsPagerActivity
import com.signal.research.features.coinsearch.CoinSearchActivity
import com.signal.research.network.models.CoinPrice
import com.signal.research.network.models.CryptoCompareNews
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import java.util.*
import kotlin.collections.ArrayList
import CoinDashboardContract
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.robinhood.spark.SparkView
import com.signal.research.featurecomponents.historicalchartmodule.WalletChartAdapter
import com.signal.research.utils.Formaters
import com.signal.research.utils.TRANSACTION_TYPE_BUY
import com.signal.research.utils.getTotalCost
import kotlinx.android.synthetic.main.fragment_dashboard.*
import java.math.BigDecimal

class CoinDashboardFragment : Fragment(), CoinDashboardContract.View {

    private val toCurrency: String by lazy {
        PreferencesManager.getDefaultCurrency(context)
    }

    private val currency by lazy {
        Currency.getInstance(toCurrency)
    }

    private val formatter by lazy {
        Formaters(androidResourceManager)
    }

    companion object {
        const val TAG = "CoinDashboardFragment"
        private const val COIN_SEARCH_CODE = 100
        private const val COIN_DETAILS_PAGER_CODE = 101
        private const val COIN_DETAILS_CODE = 102
    }

    private var coinDashboardList: MutableList<ModuleItem> = ArrayList()
    private var watchedCoinList: List<WatchedCoin> = emptyList()
    private var coinTransactionList: List<CoinTransaction> = emptyList()
    private var allCoinTransactionList: MutableList<CoinTransaction> = ArrayList()
    private var shouldRefresh = false
    private var costList: MutableList<Pair<String, Double>> = ArrayList()
    private var valueList: MutableList<Pair<String, Double>> = ArrayList()
    private var currentValueList: MutableList<Pair<String, Double>> = ArrayList()

    private val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(requireContext())
    }

    private val dashboardRepository by lazy {
        DashboardRepository(CoinBitApplication.database)
    }

    private val coinRepo by lazy {
        CryptoCompareRepository(CoinBitApplication.database)
    }

    private val coinDashboardPresenter: CoinDashboardPresenter by lazy {
        CoinDashboardPresenter(dashboardRepository, coinRepo)
    }

    private val coinNews: MutableList<CryptoCompareNews> = mutableListOf()

    private lateinit var rvDashboard: EpoxyRecyclerView
    private lateinit var clWallet: ConstraintLayout
    private lateinit var walletChartView: SparkView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val inflate = inflater.inflate(R.layout.fragment_dashboard, container, false)

//        val toolbar = inflate.toolbar
//        toolbar?.title = getString(R.string.market)
//
//        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        coinDashboardPresenter.attachView(this)

        lifecycle.addObserver(coinDashboardPresenter)

        // empty existing list
        coinDashboardList = ArrayList()

        initializeUI(inflate)

        // get top coins
        coinDashboardPresenter.getTopCoinsByTotalVolume24hours(PreferencesManager.getDefaultCurrency(context))

        // get prices for watched coin
        coinDashboardPresenter.loadWatchedCoinsAndTransactions()

        //get all transaction history to make wallet evolution
        coinDashboardPresenter.loadAllCoinTransactions()

        FirebaseCrashlytics.getInstance().log("CoinDashboardFragment")

        return inflate
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefresh) {
            clearList()
            coinDashboardPresenter.loadWatchedCoinsAndTransactions()
            shouldRefresh = false
        }
    }

    private fun initializeUI(inflatedView: View) {

        rvDashboard = inflatedView.rvCoinDashboard
        clWallet = inflatedView.clWallet
        walletChartView = inflatedView.walletChartView

        inflatedView.swipeDashboardContainer.setOnRefreshListener {

            updateCoinDashboard()
            inflatedView.swipeDashboardContainer.isRefreshing = false
        }

        clWallet.setOnClickListener {
            val v = if (llHidden.visibility == View.GONE) View.VISIBLE else View.GONE

            TransitionManager.beginDelayedTransition(cvWallet, AutoTransition())
            llHidden.visibility = v
        }
    }

    private fun updateCoinDashboard() {
        coinDashboardList.clear()

        // get top coins
        coinDashboardPresenter.getTopCoinsByTotalVolume24hours(PreferencesManager.getDefaultCurrency(context))

        coinDashboardPresenter.loadWatchedCoinsAndTransactions()
    }

    override fun onWatchedCoinsAndTransactionsLoaded(watchedCoinList: List<WatchedCoin>, coinTransactionList: List<CoinTransaction>) {

        this.watchedCoinList = watchedCoinList
        this.coinTransactionList = coinTransactionList

        setupDashBoardAdapter(watchedCoinList, coinTransactionList)

        getAllWatchedCoinsPrice()
    }

    private fun setupDashBoardAdapter(watchedCoinList: List<WatchedCoin>, coinTransactionList: List<CoinTransaction>) {

        coinDashboardList.add(AddWalletItemView.AddWalletModuleItem)

        coinDashboardList.add(AddCoinItemView.AddCoinModuleItem)

        watchedCoinList.forEach { watchedCoin ->
            coinDashboardList.add(
                CoinItemView.DashboardCoinModuleData(
                    false, watchedCoin,
                    null, coinTransactionList
                )
            )
        }

        coinDashboardList.add(GenericFooterItemView.FooterModuleData(getString(R.string.crypto_compare), getString(R.string.crypto_compare_url)))

        showDashboardData(coinDashboardList)
    }

    private fun getAllWatchedCoinsPrice() {

        // get news
        coinDashboardPresenter.getLatestNewsFromCryptoCompare()

        // cryptocompare support only 100 coins in 1 shot. For safety we will support 95 and paginate
        val chunkWatchedList: List<List<WatchedCoin>> = watchedCoinList.chunked(95)

        chunkWatchedList.forEach {
            // we have all the watched coins now get price for all the coins
            var fromSymbol = ""
            it.forEachIndexed { index, watchedCoin ->
                if (index != it.size - 1) {
                    fromSymbol = fromSymbol + watchedCoin.coin.symbol + ","
                } else {
                    fromSymbol += watchedCoin.coin.symbol
                }
            }
            coinDashboardPresenter.loadCoinsPrices(fromSymbol, PreferencesManager.getDefaultCurrency(context))
        }
    }

    override fun onCoinPricesLoaded(coinPriceListMap: HashMap<String, CoinPrice>) {

        coinDashboardList.forEachIndexed { index, item ->
            if (item is CoinItemView.DashboardCoinModuleData && coinPriceListMap.contains(item.watchedCoin.coin.symbol.uppercase())) {
                coinDashboardList[index] = item.copy(coinPrice = coinPriceListMap[item.watchedCoin.coin.symbol.uppercase()])
            } else if (item is DashboardHeaderItemView.DashboardHeaderModuleData) {
                coinDashboardList[index] = item.copy(coinPriceListMap = coinPriceListMap)
            }
        }

        // update dashboard card
        showDashboardData(coinDashboardList)
    }

    override fun onTopCoinsByTotalVolumeLoaded(topCoins: List<CoinPrice>) {

        val topCardList = mutableListOf<TopCardItemView.TopCardsModuleData>()
        topCoins.forEach {
            val percentageDouble = it.changePercentage24Hour?.toDouble() ?: 0.0
            val priceDouble = it.price?.toDouble() ?: 0.0
            val absoluteChange = priceDouble * (percentageDouble/100)

            topCardList.add(
                TopCardItemView.TopCardsModuleData(
                        "${it.fromSymbol}/${it.toSymbol}",
                        "%.2f".format(it.price?.toDouble()),
                        "%.2f".format(it.changePercentage24Hour?.toDouble()),
                        "%.2f".format(absoluteChange),
                        it.marketCap ?: "0",
                        it.fromSymbol ?: "",
                        it.imageUrl ?: ""
                )
            )
        }
        coinDashboardList.add(0, TopCardList(topCardList))
        showDashboardData(coinDashboardList)
    }

    private data class TopCardList(val topCardList: List<TopCardItemView.TopCardsModuleData>) : ModuleItem

    override fun onCoinNewsLoaded(coinNews: List<CryptoCompareNews>) {
        this.coinNews.clear()
        this.coinNews.addAll(coinNews)

        if (coinDashboardList.size > 0) {
            if (coinDashboardList[1] is ShortNewsItemView.ShortNewsModuleData) {
                coinDashboardList[1] = ShortNewsItemView.ShortNewsModuleData(coinNews[0])
            } else {
                coinDashboardList.add(1, ShortNewsItemView.ShortNewsModuleData(coinNews[0]))
            }
        } else {
            coinDashboardList.add(ShortNewsItemView.ShortNewsModuleData(coinNews[0]))
        }
        showDashboardData(coinDashboardList)
    }

    override fun onAllCoinTransactionsLoaded(coinTransactionList: List<CoinTransaction>) {
        this.allCoinTransactionList = coinTransactionList.toMutableList()
        updateWallet()
    }

    private fun showDashboardData(coinList: List<ModuleItem>) {
        //isResumed is used to avoid update screen if fragment is not the one showing on screen
        if (isResumed) {
            costList = ArrayList()
            currentValueList = ArrayList()
            rvDashboard.withModels {
                coinList.forEachIndexed { _, moduleItem ->
                    when (moduleItem) {
                        is TopCardList -> {
                            val topCards = mutableListOf<TopCardItemViewModel_>()
                            moduleItem.topCardList.forEach {
                                topCards.add(
                                    TopCardItemViewModel_()
                                        .id(it.pair).topCardData(it).itemClickListener(object :
                                            TopCardItemView.OnTopItemClickedListener {
                                            override fun onItemClicked(coinSymbol: String) {
                                                startActivityForResult(
                                                    CoinDetailsActivity.buildLaunchIntent(
                                                        requireContext(),
                                                        coinSymbol
                                                    ), COIN_DETAILS_CODE
                                                )
                                            }
                                        })
                                )
                            }
                            carousel {
                                id("topCardList")
                                models(topCards)
                                numViewsToShowOnScreen(2.25F)
                                Carousel.setDefaultGlobalSnapHelperFactory(null)
                            }
                            rvDashboard.smoothScrollToPosition(0)
                        }
//                    is ShortNewsItemView.ShortNewsModuleData -> shortNewsItemView {
//                        id("shortNews")
//                        newsDate(moduleItem.news.title ?: "")
//                        itemClickListener { _ ->
//                            moduleItem.news.url?.let {
//                                openCustomTab(it, requireContext())
//                                CoinBitCache.updateCryptoCompareNews(moduleItem.news)
//                                coinDashboardPresenter.getLatestNewsFromCryptoCompare()
//                            }
//                        }
//                    }
                        is CoinItemView.DashboardCoinModuleData -> coinItemView {
                            id(moduleItem.watchedCoin.coin.id)
                            dashboardCoinModuleData(moduleItem)
                            itemClickListener(object : CoinItemView.OnCoinItemClickListener {
                                override fun onCoinClicked(watchedCoin: WatchedCoin) {
                                    val intent = Intent(context, CoinDetailsPagerActivity::class.java)
                                    intent.putExtra(CoinDetailsPagerActivity.WATCHED_COIN, watchedCoin)
                                    startActivityForResult(intent, COIN_DETAILS_PAGER_CODE)
                                    //startActivityForResult(CoinDetailsPagerActivity.buildLaunchIntent(requireContext(), watchedCoin), COIN_DETAILS_CODE)
                                }
                            })

                            saveCoinValue(moduleItem.coinPrice)
                            //getCostAndValue(coinItemView)
                        }
                        is AddWalletItemView.AddWalletModuleItem -> addWalletItemView {
                            id("wallet")
                        }
                        is AddCoinItemView.AddCoinModuleItem -> addCoinItemView {
                            id("add coin")
                            addCoinClickListener { _ ->
                                startActivityForResult(
                                    CoinSearchActivity.buildLaunchIntent(
                                        requireContext()
                                    ), COIN_SEARCH_CODE
                                )
                            }
                        }
//                    is GenericFooterItemView.FooterModuleData -> genericFooterItemView {
//                        id("footer")
//                        footerContent(moduleItem)
//                    }
                    }
                }
            }
        } else {
            shouldRefresh = true
        }
        updateWallet()
    }

    private fun saveCoinValue(coinPrice: CoinPrice?) {
        coinPrice?.let { coin ->
            coin.fromSymbol?.let { symbol ->
                coin.price?.let { price ->
                    if (!this.valueList.any { it.first == symbol }) {
                        this.valueList.add(Pair(symbol, price.toDouble()))
                    }
                }
            }
        }
    }

    private fun shouldShowWallet(): Boolean {
        var symbol = ""
        var amount = 0
        this.allCoinTransactionList.sortBy { it.coinSymbol }
        this.allCoinTransactionList.forEach {
            if (it.coinSymbol != symbol) {
                if (amount != 0) {
                    return true
                }
                symbol = it.coinSymbol
            }
            amount = if (it.transactionType == TRANSACTION_TYPE_BUY) {
                amount + it.quantity.toInt()
            } else {
                amount - it.quantity.toInt()
            }
        }
        if (amount > 0) {
            return true
        }
        return false
    }

    private fun getWalletCost(): List<CoinTransaction> {
        val walletTransactions = mutableListOf<CoinTransaction>()
        for (coinTransaction in this.allCoinTransactionList) {
            if (walletTransactions.any {it.coinSymbol == coinTransaction.coinSymbol}) {
                val walletTransaction = walletTransactions.first { it.coinSymbol == coinTransaction.coinSymbol }
                if (walletTransaction.transactionType == TRANSACTION_TYPE_BUY) {
                    walletTransaction.quantity = walletTransaction.quantity + coinTransaction.quantity
                    walletTransaction.cost =
                        (walletTransaction.cost.toDouble() + coinTransaction.cost.toDouble()).toString()
                } else {
                    walletTransaction.quantity = walletTransaction.quantity - coinTransaction.quantity
                    walletTransaction.cost =
                        (walletTransaction.cost.toDouble() - coinTransaction.cost.toDouble()).toString()
                }
            } else {
                val transaction = coinTransaction.copy()
                walletTransactions.add(transaction)
            }
        }
        return walletTransactions
    }

    private fun updateWallet() {
        var totalCost = 0.0
        var totalValue = 0.0

        if (shouldShowWallet() && this.valueList.size > 0) {
            val walletTransactions = getWalletCost()
            setChartData()
            walletTransactions.forEach { coinTransaction ->
                totalCost += coinTransaction.cost.toDouble()
                val currentValue = this.valueList.first {it.first == coinTransaction.coinSymbol}
                totalValue += (coinTransaction.quantity.toDouble() * currentValue.second)
            }

            cvWallet.visibility = View.VISIBLE
            tvCostValue.text = formatter.formatAmount(totalCost.toString(), currency)
            tvWalletValue.text =  formatter.formatAmount(totalValue.toString(), currency)
            context?.let {
                if (totalCost > totalValue) {
                    tvWalletValue.setTextColor(ContextCompat.getColor(it, R.color.colorLoss))
                } else if (totalCost < totalValue) {
                    tvWalletValue.setTextColor(ContextCompat.getColor(it, R.color.colorGain))
                } else {
                    tvWalletValue.setTextColor(ContextCompat.getColor(it, R.color.primaryTextColor))
                }
            }
        } else {
            cvWallet.visibility = View.GONE
        }
    }

    private fun setChartData() {
        var list: MutableList<Double> = mutableListOf()
        list.add(0.0)
        list.add(35.0)
        list.add(15.0)
        list.add(57.0)
        list.add(35.0)
        list.add(86.0)
        list.add(104.0)
        list.add(65.0)
        list.add(123.0)
        list.add(0.0)
        list.add(35.0)
        list.add(15.0)
        list.add(57.0)
        list.add(35.0)
        list.add(86.0)
        list.add(104.0)
        list.add(65.0)
        list.add(123.0)
        list.add(0.0)
        list.add(35.0)
        list.add(15.0)
        list.add(57.0)
        list.add(35.0)
        list.add(86.0)
        list.add(104.0)
        list.add(65.0)
        list.add(123.0)
        list.add(0.0)
        list.add(35.0)
        list.add(15.0)
        list.add(57.0)
        list.add(35.0)
        list.add(86.0)
        list.add(104.0)
        list.add(65.0)
        list.add(123.0)

        walletChartView.adapter = WalletChartAdapter(list)
    }

    private fun addTotalCost(cost: Double, symbol: String) {
        costList.add(Pair(symbol, cost))
    }

    private fun addTotalValue(value: Double, symbol: String) {
        currentValueList.add(Pair(symbol, value))
    }

    private fun getCostAndValue(dashboardCoinModuleData: CoinItemView.DashboardCoinModuleData) {
        val coin = dashboardCoinModuleData.watchedCoin.coin
        val purchaseQuantity = dashboardCoinModuleData.watchedCoin.purchaseQuantity
        if (purchaseQuantity > BigDecimal.ZERO) {
            val coinPrice = dashboardCoinModuleData.coinPrice
            coinPrice?.let {
                val currentWorth = purchaseQuantity.multiply(BigDecimal(it.price)).toDouble()
                addTotalValue(currentWorth, coin.symbol)
            }
            val cost = getTotalCost(dashboardCoinModuleData.coinTransactionList, coin.symbol).toDouble()
            addTotalCost(cost, coin.symbol)

            //updateWallet()
        }
    }

    private fun clearList() {
        val moduleItem = coinDashboardList.first()
        coinDashboardList = ArrayList()
        coinDashboardList.add(moduleItem)
    }

    override fun onNetworkError(errorMessage: String) {
        Snackbar.make(rvDashboard, errorMessage, Snackbar.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (COIN_SEARCH_CODE == requestCode || COIN_DETAILS_CODE == requestCode || COIN_DETAILS_PAGER_CODE == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                clearList()
                coinDashboardPresenter.loadWatchedCoinsAndTransactions()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        (activity as AppCompatActivity).setSupportActionBar(null)
        super.onDestroyView()
    }
}
