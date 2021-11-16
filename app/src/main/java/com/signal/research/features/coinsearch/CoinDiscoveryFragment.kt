package com.signal.research.features.coinsearch

import CoinDiscoveryContract
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.carousel
import com.signal.research.CoinBitApplication
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.epoxymodels.*
import com.signal.research.featurecomponents.ModuleItem
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.features.coindetails.CoinDetailsActivity
import com.signal.research.network.models.CoinPair
import com.signal.research.network.models.CoinPrice
import com.signal.research.network.models.CryptoCompareNews
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.fragment_discovery.view.*

class CoinDiscoveryFragment : Fragment(), CoinDiscoveryContract.View {

    companion object {
        const val TAG = "CoinDiscoveryFragment"
    }

    private var nextMenuItem: MenuItem? = null

    private var coinDiscoveryList: ArrayList<ModuleItem> = ArrayList()

    private val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(requireContext())
    }

    private val coinRepo by lazy {
        CryptoCompareRepository(CoinBitApplication.database)
    }

    private val coinDiscoveryPresenter: CoinDiscoveryPresenter by lazy {
        CoinDiscoveryPresenter(coinRepo)
    }

    private lateinit var rvDashboard: EpoxyRecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val inflate = inflater.inflate(R.layout.fragment_discovery, container, false)

//        val toolbar = inflate.toolbar
//        toolbar?.title = getString(R.string.discover)
//
//        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        rvDashboard = inflate.findViewById<EpoxyRecyclerView>(R.id.rvDashboard)

        coinDiscoveryPresenter.attachView(this)

        lifecycle.addObserver(coinDiscoveryPresenter)

        // empty existing list
        coinDiscoveryList = ArrayList()

        // get top coin by market cap
        coinDiscoveryPresenter.getTopCoinListByMarketCap(PreferencesManager.getDefaultCurrency(context))

        // get coins by volume
        coinDiscoveryPresenter.getTopCoinListByPairVolume()

        setHasOptionsMenu(true)

        FirebaseCrashlytics.getInstance().log("CoinDiscoveryFragment")

        rvDashboard.setItemSpacingDp(8)

        inflate.swipeContainer.setOnRefreshListener {

            coinDiscoveryList.clear()

            // get top coin by market cap
            coinDiscoveryPresenter.getTopCoinListByMarketCap(PreferencesManager.getDefaultCurrency(context))

            // get coins by volume
            coinDiscoveryPresenter.getTopCoinListByPairVolume()

            inflate.swipeContainer.isRefreshing = false
        }

        return inflate
    }

    override fun onTopCoinsByTotalVolumeLoaded(topCoins: List<CoinPrice>) {

        val topCardList = mutableListOf<TopCardItemView.TopCardsModuleData>()
        topCoins.forEach {
            val percentageDouble = it.changePercentage24Hour?.toDouble() ?: 0.0
            val priceDouble = it.price?.toDouble() ?: 0.0
            val absoluteChange = String.format("%.2f", priceDouble * (percentageDouble/100)).toDouble()

            topCardList.add(
                TopCardItemView.TopCardsModuleData(
                        "${it.fromSymbol}/${it.toSymbol}",
                        "%.2f".format(it.price?.toDouble()) ?: "0",
                        "%.2f".format(it.changePercentage24Hour?.toDouble()) ?: "0",
                        "%.2f".format(absoluteChange) ?: "0",
                        it.marketCap ?: "0",
                        it.fromSymbol ?: "",
                        it.imageUrl ?: ""
                )
            )
        }

        if (coinDiscoveryList.size > 1) {
            coinDiscoveryList.add(0, LabelItemView.LabelModuleData(getString(R.string.top_volume)))
            coinDiscoveryList.add(1, TopCardList(topCardList))
        } else {
            coinDiscoveryList.add(LabelItemView.LabelModuleData(getString(R.string.top_volume)))
            coinDiscoveryList.add(TopCardList(topCardList))
        }

        // call load
        showCoins(coinDiscoveryList)
    }

    override fun onTopCoinListByPairVolumeLoaded(topPair: List<CoinPair>) {
        if (coinDiscoveryList.size > 3) {
            coinDiscoveryList.add(2, LabelItemView.LabelModuleData(getString(R.string.top_pair)))
            coinDiscoveryList.add(3, ChipGroupItemView.ChipGroupModuleData(topPair))
        } else {
            coinDiscoveryList.add(LabelItemView.LabelModuleData(getString(R.string.top_pair)))
            coinDiscoveryList.add(ChipGroupItemView.ChipGroupModuleData(topPair))
        }

        // call load
        showCoins(coinDiscoveryList)

        // get news
        coinDiscoveryPresenter.getCryptoCurrencyNews()
    }

    override fun onCoinNewsLoaded(coinNews: List<CryptoCompareNews>) {
        coinDiscoveryList.add(LabelItemView.LabelModuleData(getString(R.string.recent_news)))
        coinNews.forEach { news ->
            coinDiscoveryList.add(ExpandedNewsItemView.DiscoveryNewsModuleData(news))
        }

        // call load
        showCoins(coinDiscoveryList)
    }

    private fun showCoins(coinDiscoveryList: List<ModuleItem>) {
        rvDashboard.withModels {
            coinDiscoveryList.forEachIndexed { index, moduleItem ->
                when (moduleItem) {
                    is LabelItemView.LabelModuleData -> labelItemView {
                        id("labelItemView")
                        label(moduleItem.coinLabel)
                    }
                    is TopCardList -> {
                        val topCards = mutableListOf<TopCardItemViewModel_>()
                        moduleItem.topCardList.forEach {
                            topCards.add(
                                TopCardItemViewModel_()
                                    .id(it.pair).topCardData(it).itemClickListener(object : TopCardItemView.OnTopItemClickedListener {
                                        override fun onItemClicked(coinSymbol: String) {
                                            context?.startActivity(
                                                CoinDetailsActivity.buildLaunchIntent(requireContext(), coinSymbol)
                                            )
                                        }
                                    })
                            )
                        }
                        carousel {
                            id("topCardList")
                            models(topCards)
                            numViewsToShowOnScreen(2.5F)
                            Carousel.setDefaultGlobalSnapHelperFactory(null)
                        }
                    }
                    is ExpandedNewsItemView.DiscoveryNewsModuleData -> expandedNewsItemView {
                        id(moduleItem.coinNews.id)
                        news(moduleItem)
                    }
                    is ChipGroupItemView.ChipGroupModuleData -> {
                        chipGroupItemView {
                            id("chipGroup")
                            chipData(moduleItem)
                            chipClickListener(object : ChipGroupItemView.OnChipItemClickedListener {
                                override fun onChipClicked(coinSymbol: String) {
                                    context?.startActivity(
                                        CoinDetailsActivity.buildLaunchIntent(requireContext(), coinSymbol)
                                    )
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    private data class TopCardList(val topCardList: List<TopCardItemView.TopCardsModuleData>) : ModuleItem

    override fun onNetworkError(errorMessage: String) {
        Snackbar.make(rvDashboard, errorMessage, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)

        nextMenuItem = menu.findItem(R.id.action_search)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                context?.let {
                    startActivity(CoinSearchActivity.buildLaunchIntent(it))
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        (activity as AppCompatActivity).setSupportActionBar(null)
        super.onDestroyView()
    }
}
