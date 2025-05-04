package com.example.av2_pos_moveis.data.repository

import androidx.lifecycle.LiveData
import com.example.av2_pos_moveis.data.dao.TransactionDao
import com.example.av2_pos_moveis.data.model.Transaction

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()
    val debitTotalsByCategory: LiveData<List<TransactionDao.CategoryTotal>> = transactionDao.getDebitTotalsByCategory()
    val monthlyDebitHistory: LiveData<List<TransactionDao.MonthlyTotal>> = transactionDao.getMonthlyDebitTotals()

    fun getRecentTransactions(limit: Int): LiveData<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit)
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun calculateBalance(): Double {
        val credits = transactionDao.getTotalCredits() ?: 0.0
        val debits = transactionDao.getTotalDebits() ?: 0.0
        return credits - debits
    }

    suspend fun getTotalCreditsValue(): Double {
        return transactionDao.getTotalCredits() ?: 0.0
    }

    suspend fun getTotalDebitsValue(): Double {
        return transactionDao.getTotalDebits() ?: 0.0
    }
}