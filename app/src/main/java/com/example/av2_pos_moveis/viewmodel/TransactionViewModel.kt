package com.example.av2_pos_moveis.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.av2_pos_moveis.data.AppDatabase
import com.example.av2_pos_moveis.data.dao.TransactionDao
import com.example.av2_pos_moveis.data.model.Transaction
import com.example.av2_pos_moveis.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: LiveData<List<Transaction>>
    val debitTotalsByCategory: LiveData<List<TransactionDao.CategoryTotal>>
    val monthlyDebitHistory: LiveData<List<TransactionDao.MonthlyTotal>>

    private val _transactionToEdit = MutableLiveData<Transaction?>()
    val transactionToEdit: LiveData<Transaction?> get() = _transactionToEdit

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)
        allTransactions = repository.allTransactions
        debitTotalsByCategory = repository.debitTotalsByCategory
        monthlyDebitHistory = repository.monthlyDebitHistory
    }

    fun getRecentTransactions(limit: Int): LiveData<List<Transaction>> {
        return repository.getRecentTransactions(limit)
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

    fun loadTransactionForEdit(id: Int) {
        if (id == -1) {
            _transactionToEdit.postValue(null)
            return
        }
        viewModelScope.launch {
            _transactionToEdit.postValue(repository.getTransactionById(id))
        }
    }

    fun doneEditing() {
        _transactionToEdit.postValue(null)
    }

    fun calculateBalance(callback: (Double) -> Unit) {
        viewModelScope.launch {
            val balance = repository.calculateBalance()
            withContext(Dispatchers.Main) {
                callback(balance)
            }
        }
    }

    fun getTotalCredits(callback: (Double) -> Unit) {
        viewModelScope.launch {
            val credits = repository.getTotalCreditsValue()
            withContext(Dispatchers.Main) {
                callback(credits)
            }
        }
    }

    fun getTotalDebits(callback: (Double) -> Unit) {
        viewModelScope.launch {
            val debits = repository.getTotalDebitsValue()
            withContext(Dispatchers.Main) {
                callback(debits)
            }
        }
    }
}