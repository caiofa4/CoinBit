package com.signal.research.featurecomponents.historicalchartmodule

import com.robinhood.spark.SparkAdapter
import com.signal.research.network.models.CryptoCompareHistoricalResponse

class WalletChartAdapter(private val historicalData: List<Double>, private val baseLineValue: String?) : SparkAdapter() {

    override fun getY(index: Int): Float {
        return historicalData[index].toFloat()
    }

    override fun getItem(index: Int): Double {
        return historicalData[index]
    }

    override fun getCount(): Int {
        return historicalData.size
    }

    override fun hasBaseLine(): Boolean {
        return true
    }

    override fun getBaseLine(): Float {
        return baseLineValue?.toFloat() ?: super.getBaseLine()
    }
}