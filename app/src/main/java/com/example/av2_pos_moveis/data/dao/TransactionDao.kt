package com.example.av2_pos_moveis.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.av2_pos_moveis.data.model.Transaction

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("""
        SELECT SUBSTR(date, 7, 4) || '-' || SUBSTR(date, 4, 2) as yearMonth, SUM(amount) as totalAmount
        FROM transactions
        WHERE type = 'Débito'
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
    """)
    fun getMonthlyDebitTotals(): LiveData<List<MonthlyTotal>>

    data class CategoryTotal(val category: String, val total: Double)

    data class MonthlyTotal(val yearMonth: String, val totalAmount: Double)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Crédito'")
    suspend fun getTotalCredits(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Débito'")
    suspend fun getTotalDebits(): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'Débito' GROUP BY category ORDER BY total DESC")
    fun getDebitTotalsByCategory(): LiveData<List<CategoryTotal>>

}