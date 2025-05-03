package com.example.av2_pos_moveis.repository

import androidx.lifecycle.LiveData
import com.example.av2_pos_moveis.data.dao.TransactionDao
import com.example.av2_pos_moveis.data.model.Transaction

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()

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
}