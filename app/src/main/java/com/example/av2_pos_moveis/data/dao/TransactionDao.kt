package com.example.av2_pos_moveis.data.dao
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.av2_pos_moveis.data.model.Transaction

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

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
}