package com.signal.research.featurecomponents.historicalchartmodule

import com.robinhood.spark.SparkAdapter
import com.signal.research.network.models.CryptoCompareHistoricalResponse

class WalletChartAdapter (private val walletData: List<Double>) : SparkAdapter() {

    override fun getCount(): Int {
        return walletData.size
    }

    override fun getItem(index: Int): Any {
        return walletData[index]
    }

    override fun getY(index: Int): Float {
        return walletData[index].toFloat()
    }
}