package com.signal.research.featurecomponents.cointickermodule

import com.signal.research.data.CoinBitCache
import com.signal.research.data.database.CoinBitDatabase
import com.signal.research.data.database.entities.Exchange
import com.signal.research.network.api.api
import com.signal.research.network.models.CryptoTicker
import com.signal.research.utils.getCoinTickerFromJson

/**
 * Created by Pranay Airan
 * Repository that interact with coin gecko api to get ticker info.
 */

class CoinTickerRepository(
    private val coinBitDatabase: CoinBitDatabase?
) {

    /**
     * Get the ticker info from coin gecko
     */
    suspend fun getCryptoTickers(coinName: String): List<CryptoTicker>? {

        return if (CoinBitCache.ticker.containsKey(coinName)) {
            CoinBitCache.ticker[coinName]
        } else {
            val exchangeList = loadExchangeList()
            val coinTickerFromJson = getCoinTickerFromJson(api.getCoinTicker(coinName), exchangeList)
            if (coinTickerFromJson.isNotEmpty()) {
                CoinBitCache.ticker[coinName] = coinTickerFromJson
                coinTickerFromJson
            } else {
                null
            }
        }
    }

    /**
     * Get list of all exchanges, this is needed for logo
     */
    private suspend fun loadExchangeList(): List<Exchange>? {
        coinBitDatabase?.let {
            return it.exchangeDao().getAllExchanges()
        }
        return null
    }
}
