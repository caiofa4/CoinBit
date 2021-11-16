package com.signal.research.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.signal.research.data.database.dao.CoinTransactionDao
import com.signal.research.data.database.dao.ExchangeDao
import com.signal.research.data.database.dao.WatchedCoinDao
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.data.database.entities.Exchange
import com.signal.research.data.database.entities.WatchedCoin

/**
 * Created by Pragya Agrawal
 */
@Database(entities = [Exchange::class, WatchedCoin::class, CoinTransaction::class], version = 1, exportSchema = false)
@TypeConverters(BigDecimalConverter::class)
abstract class CoinBitDatabase : RoomDatabase() {

    abstract fun exchangeDao(): ExchangeDao
    abstract fun watchedCoinDao(): WatchedCoinDao
    abstract fun coinTransactionDao(): CoinTransactionDao
}
