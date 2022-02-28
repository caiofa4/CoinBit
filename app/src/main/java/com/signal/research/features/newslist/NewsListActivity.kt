package com.signal.research.features.newslist

import CryptoNewsContract
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.signal.research.R
import com.signal.research.epoxymodels.newsItemView
import com.signal.research.featurecomponents.cryptonewsmodule.CryptoNewsPresenter
import com.signal.research.featurecomponents.cryptonewsmodule.CryptoNewsRepository
import com.signal.research.network.models.CryptoPanicNews
import com.signal.research.utils.Formaters
import com.signal.research.utils.openCustomTab
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_coin_ticker_list.*
import kotlinx.android.synthetic.main.activity_news_list.*
import kotlinx.android.synthetic.main.activity_news_list.pbLoading
import kotlinx.android.synthetic.main.activity_news_list.tvFooter

/**
 * Created by Pragya Agrawal
 * Activity showing all news items
 */
class NewsListActivity : AppCompatActivity(), CryptoNewsContract.View {

    companion object {
        private const val COIN_FULL_NAME = "COIN_FULL_NAME"
        private const val COIN_SYMBOL = "COIN_SYMBOL"

        @JvmStatic
        fun buildLaunchIntent(context: Context, coinName: String, coinSymbol: String): Intent {
            val intent = Intent(context, NewsListActivity::class.java)
            intent.putExtra(COIN_FULL_NAME, coinName)
            intent.putExtra(COIN_SYMBOL, coinSymbol)
            return intent
        }
    }

    private val cryptoNewsRepository by lazy {
        CryptoNewsRepository()
    }
    private val cryptoNewsPresenter: CryptoNewsPresenter by lazy {
        CryptoNewsPresenter(cryptoNewsRepository)
    }

    private val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(this)
    }

    private val formatter: Formaters by lazy {
        Formaters(androidResourceManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_list)

        val toolbar = findViewById<View>(R.id.toolbar)
        setSupportActionBar(toolbar as Toolbar?)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coinFullName = intent.getStringExtra(COIN_FULL_NAME)?.trim()
        val coinSymbol = intent.getStringExtra(COIN_SYMBOL)?.trim()

        supportActionBar?.title = getString(R.string.newsActivityTitle, coinFullName)

        cryptoNewsPresenter.attachView(this)

        lifecycle.addObserver(cryptoNewsPresenter)

        if (coinSymbol != null) {
            cryptoNewsPresenter.getCryptoNews(coinSymbol)
        }

        FirebaseCrashlytics.getInstance().log("NewsListActivity")
    }

    override fun showOrHideLoadingIndicator(showLoading: Boolean) {
        if (!showLoading) {
            pbLoading.hide()
        } else {
            pbLoading.show()
        }
    }

    override fun onNewsLoaded(cryptoPanicNews: CryptoPanicNews) {
        rvNewsList.withModels {
            cryptoPanicNews.results?.forEachIndexed { index, result ->
                newsItemView {
                    id(index)
                    title(result.title)
                    newsDate(formatter.parseAndFormatIsoDate(result.created_at, true))
                    itemClickListener { _ ->
                        openCustomTab(result.url, this@NewsListActivity)
                    }
                }
            }
        }
        tvFooter.setOnClickListener {
            openCustomTab(getString(R.string.crypto_panic_url), this)
        }
    }

    override fun onNetworkError(errorMessage: String) {
        Snackbar.make(rvNewsList, errorMessage, Snackbar.LENGTH_LONG).show()
    }
}
