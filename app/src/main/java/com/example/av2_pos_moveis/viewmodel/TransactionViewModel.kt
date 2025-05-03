package com.example.av2_pos_moveis.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.av2_pos_moveis.data.AppDatabase
import com.example.av2_pos_moveis.data.model.Transaction
import com.example.av2_pos_moveis.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val transactions: LiveData<List<Transaction>>

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        transactions = repository.allTransactions
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun calculateBalance(callback: (Double) -> Unit) {
        viewModelScope.launch {
            val balance = repository.calculateBalance()
            withContext(Dispatchers.Main) {
                callback(balance)
            }
        }
    }
}