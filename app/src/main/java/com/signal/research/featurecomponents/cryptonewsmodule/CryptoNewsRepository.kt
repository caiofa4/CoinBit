package com.signal.research.featurecomponents.cryptonewsmodule

import com.signal.research.data.CoinBitCache
import com.signal.research.network.api.API
import com.signal.research.network.api.cryptoCompareRetrofit
import com.signal.research.network.models.CryptoPanicNews

/**
 * Created by Pragya Agrawal
 * Repository that interact with crypto api to get news.
 */

class CryptoNewsRepository {

    /**
     * Get the top news for specific coin from cryptopanic
     */
    suspend fun getCryptoPanicNews(coinSymbol: String): CryptoPanicNews {

        return if (CoinBitCache.newsMap.containsKey(coinSymbol)) {
            CoinBitCache.newsMap[coinSymbol]!!
        } else {
            cryptoCompareRetrofit.create(API::class.java)
                .getCryptoNewsForCurrency(coinSymbol, "important", true)
        }
    }
}
