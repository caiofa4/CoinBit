package com.signal.research.data.database.dao

import androidx.room.*
import com.signal.research.data.database.entities.CoinTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Created by Pragya Agrawal
 *
 * Add queries to read/update coinSymbol transaction data from database.
 */
@Dao
interface CoinTransactionDao {

    @Query("select * from cointransaction")
    fun getAllCoinTransaction(): Flow<List<CoinTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(coinTransaction: CoinTransaction)

    @Delete
    suspend fun deleteTransaction(coinTransaction: CoinTransaction)

    @Update
    suspend fun updateTransaction(coinTransaction: CoinTransaction)

    @Query("SELECT * FROM cointransaction WHERE coinSymbol = :coinSymbol ORDER BY transactionTime ASC")
    fun getTransactionsForCoin(coinSymbol: String): Flow<List<CoinTransaction>>
}
