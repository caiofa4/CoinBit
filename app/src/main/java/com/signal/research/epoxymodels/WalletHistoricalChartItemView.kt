package com.signal.research.epoxymodels

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.DashPathEffect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ContentLoadingProgressBar
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.OnViewRecycled
import com.robinhood.spark.SparkView
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.featurecomponents.ModuleItem
import com.signal.research.featurecomponents.historicalchartmodule.HistoricalChartAdapter
import com.signal.research.featurecomponents.historicalchartmodule.WalletChartAdapter
import com.signal.research.network.*
import com.signal.research.network.models.CryptoCompareHistoricalResponse
import com.signal.research.utils.Formaters
import com.signal.research.utils.chartAnimationDuration
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import kotlinx.android.synthetic.main.historical_chart_module.view.*
import java.util.*

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WalletHistoricalChartItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val tvErrorGraph: TextView
    private val pbChartLoading: ContentLoadingProgressBar
    private val tvPortfolioChangedValue: TextView
    private val tvPortfolioChangedPercentage: TextView
    private val tvPortfolioArrow: TextView
    private val tvPortfolioChangedDate: TextView
    private val historicalChartView: SparkView

    private var historicalWalletValueList: List<Double>? = null
    private var coinPrice: String? = null

    private var selectedPeriod = HOUR

    private var onHistoricalChardRangeSelectionListener: OnHistoricalChardRangeSelectionListener? = null

    val toCurrency: String by lazy {
        PreferencesManager.getDefaultCurrency(context.applicationContext)
    }

    private val currency by lazy {
        Currency.getInstance(toCurrency)
    }

    val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(context)
    }

    private val formatter by lazy {
        Formaters(androidResourceManager)
    }

    init {
        View.inflate(context, R.layout.historical_chart_module, this)
        tvErrorGraph = findViewById(R.id.tvErrorGraph)
        pbChartLoading = findViewById(R.id.pbChartLoading)
        tvPortfolioChangedValue = findViewById(R.id.tvPortfolioHistoricalChangedValue)
        tvPortfolioChangedPercentage = findViewById(R.id.tvPortfolioHistoricalChangedPercentage)
        tvPortfolioArrow = findViewById(R.id.tvPortfolioArrow)
        tvPortfolioChangedDate = findViewById(R.id.tvPortfolioChangedDate)
        historicalChartView = findViewById(R.id.historicalChartView)
    }

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun setChartData(historicalChartModuleData: HistoricalChartModuleData) {
        this.coinPrice = historicalChartModuleData.coinPrice
        animateCoinPrice(this.coinPrice)

        historicalWalletValueList = historicalChartModuleData.historicalWalletValueList

        historicalWalletValueList?.let {
            showOrHideChartLoadingIndicator(false)

            setupChart(it)

            if (historicalChartModuleData.period != ALL) {
                showPercentageGainOrLoss(it)
            } else {
                tvPortfolioChangedValue.text = ""
                showPositiveGainColor()
            }
            showChartPeriodText(historicalChartModuleData.period)

            addChartScrubListener()
            addRangeSelectorListener(historicalChartModuleData.fromCurrency)
        } ?: kotlin.run {
            showOrHideChartLoadingIndicator(true)
        }
    }

    @CallbackProp
    fun onChartRangeSelected(onHistoricalChardRangeSelectionListener: OnHistoricalChardRangeSelectionListener?) {
        this.onHistoricalChardRangeSelectionListener = onHistoricalChardRangeSelectionListener
    }

    private fun setupChart(dataListPair: List<Double>) {
        historicalChartView.adapter = WalletChartAdapter(dataListPair, coinPrice)

        // historicalChartView.fillType=SparkView.FillType.DOWN

        val baseLinePaint = historicalChartView.baseLinePaint
        val dashPathEffect = DashPathEffect(floatArrayOf(10.0f, 2.0f), 0f)
        baseLinePaint.pathEffect = dashPathEffect
    }

    private fun showPercentageGainOrLoss(historicalData: List<Double>?) {
        if (historicalData != null) {
            val lastClosingPrice = historicalData.first().toFloat() // we always get's oldest first in api
            val currentClosingPrice = historicalData.last().toFloat()
            val gain = currentClosingPrice - lastClosingPrice
            val percentageChange: Float = (gain / lastClosingPrice) * 100

            tvPortfolioChangedValue.text =
                androidResourceManager.getString(R.string.gain, formatter.formatAmount(gain.toString(), currency))
            tvPortfolioChangedPercentage.text =
                androidResourceManager.getString(R.string.gainPercentage, percentageChange)
            tvPortfolioChangedValue.visibility = View.VISIBLE

            if (gain > 0) {
                showPositiveGainColor()
                // RxPubSub.publish(HistoricalChartBusData(true, gain))
            } else {
                showNegativeGainColor()
                // RxPubSub.publish(HistoricalChartBusData(false, gain))
            }
        }
    }

    private fun showPositiveGainColor() {
        tvPortfolioChangedPercentage.setTextColor(androidResourceManager.getColor(R.color.colorGain))
        tvPortfolioArrow.setTextColor(androidResourceManager.getColor(R.color.colorGain))
        tvPortfolioArrow.text = androidResourceManager.getString(R.string.portfolio_up)

        historicalChartView.lineColor = androidResourceManager.getColor(R.color.colorGain)
    }

    private fun showNegativeGainColor() {
        tvPortfolioChangedPercentage.setTextColor(androidResourceManager.getColor(R.color.colorLoss))
        tvPortfolioArrow.setTextColor(androidResourceManager.getColor(R.color.colorLoss))
        tvPortfolioArrow.text = androidResourceManager.getString(R.string.portfolio_down)

        historicalChartView.lineColor = androidResourceManager.getColor(R.color.colorLoss)
    }

    private fun showChartPeriodText(period: String) {
        val periodText = when (period) {
            HOUR -> androidResourceManager.getString(R.string.past_hour)
            HOURS24 -> androidResourceManager.getString(R.string.past_day)
            WEEK -> androidResourceManager.getString(R.string.past_week)
            MONTH -> androidResourceManager.getString(R.string.past_month)
            MONTH3 -> androidResourceManager.getString(R.string.past_month_3)
            YEAR -> androidResourceManager.getString(R.string.past_year)
            ALL -> androidResourceManager.getString(R.string.all_time)
            else -> androidResourceManager.getString(R.string.past_hour)
        }
        tvPortfolioChangedDate.text = periodText
    }

    private fun addChartScrubListener() {
        historicalChartView.setScrubListener { value ->
            if (value == null) { // reset the quantity
                animateCoinPrice(this.coinPrice)
                showPercentageGainOrLoss(historicalWalletValueList)
                showChartPeriodText(selectedPeriod)
            } else {
                val historicalData = value as CryptoCompareHistoricalResponse.Data
                tvPortfolioChangedValue.visibility = View.GONE
                tvPortfolioChangedDate.text = formatter.formatDate(historicalData.time, 1000)
                animateCoinPrice(historicalData.close)
            }
        }
    }

    private fun animateCoinPrice(amount: String?) {
        if (amount != null) {
            Log.i("tagTesteanimateCoinPrice", amount)
            val chartCoinPriceAnimation = ValueAnimator.ofFloat(tvChartCoinPrice.tag.toString().toFloat(), amount.toFloat())
            chartCoinPriceAnimation.duration = chartAnimationDuration
            chartCoinPriceAnimation.addUpdateListener { updatedAnimation ->
                val animatedValue = updatedAnimation.animatedValue as Float
                tvChartCoinPrice.text = formatter.formatAmount(amount, currency)
                tvChartCoinPrice.tag = animatedValue
            }
            chartCoinPriceAnimation.start()
        }
    }

    private fun addRangeSelectorListener(fromCurrency: String) {
        rgPeriodSelector.setOnCheckedChangeListener { _, id ->
            val period = when (id) {
                R.id.rbPeriod12H -> HOUR
                R.id.rbPeriod1D -> HOURS24
                R.id.rbPeriod1W -> WEEK
                R.id.rbPeriod1M -> MONTH
                R.id.rbPeriod3M -> MONTH3
                R.id.rbPeriod1Y -> YEAR
                R.id.rbPeriodAll -> ALL
                else -> HOUR
            }
            findViewById<RadioButton>(id)
                .setTextColor(androidResourceManager.getColor(R.color.primaryTextColor))
            selectedPeriod = period

            onHistoricalChardRangeSelectionListener?.onRangeSelected(period, fromCurrency, toCurrency)
            showOrHideChartLoadingIndicator(true)
        }
    }

    interface OnHistoricalChardRangeSelectionListener {
        fun onRangeSelected(period: String, fromCurrency: String, toCurrency: String)
    }

    @OnViewRecycled
    fun cleanup() {
        historicalChartView.adapter = null
        historicalWalletValueList = null
        coinPrice = null
    }

    private fun showOrHideChartLoadingIndicator(showLoading: Boolean) {
        tvErrorGraph.visibility = View.GONE
        if (showLoading) pbChartLoading.show() else pbChartLoading.hide()
    }

    data class HistoricalChartModuleData(
        val coinPrice: String,
        val period: String,
        val fromCurrency: String,
        val historicalWalletValueList: List<Double>
    ) : ModuleItem
}
