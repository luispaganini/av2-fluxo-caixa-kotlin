package com.example.av2_pos_moveis.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.av2_pos_moveis.R
import com.example.av2_pos_moveis.data.model.Transaction
import com.example.av2_pos_moveis.databinding.ActivityMainBinding
import com.example.av2_pos_moveis.ui.adapter.OnTransactionActionListener
import com.example.av2_pos_moveis.ui.adapter.TransactionAdapter
import com.example.av2_pos_moveis.viewmodel.TransactionViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnTransactionActionListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private val creditCategories = listOf("Salário", "Extras")
    private val debitCategories = listOf("Alimentação", "Transporte", "Saúde", "Moradia")
    private val transactionTypes = listOf("Crédito", "Débito")

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var transactionToEdit: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        setupRecyclerView()
        setupDatePicker()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupDropdowns() {
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, transactionTypes)
        (binding.layoutType.editText as? AutoCompleteTextView)?.setAdapter(typeAdapter)

        (binding.layoutType.editText as? AutoCompleteTextView)?.setOnItemClickListener { parent, _, position, _ ->
            val selectedType = parent.getItemAtPosition(position).toString()
            updateCategoryDropdown(selectedType, clearSelection = true)
        }
        binding.layoutCategory.isEnabled = false
    }

    private fun updateCategoryDropdown(type: String?, clearSelection: Boolean) {
        if (type == null) {
            binding.layoutCategory.isEnabled = false
            (binding.layoutCategory.editText as? AutoCompleteTextView)?.setAdapter(null)
            return
        }

        val categories = if (type == "Crédito") creditCategories else debitCategories
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setAdapter(categoryAdapter)

        if (clearSelection) {
            (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText("", false)
        }
        binding.layoutCategory.isEnabled = true
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(this)
        binding.recyclerTransactions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupDatePicker() {
        binding.inputDate.isFocusable = false
        binding.inputDate.isClickable = true

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione a data")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val selectedDate = dateFormat.format(calendar.time)
            binding.inputDate.setText(selectedDate)
            binding.layoutDate.error = null
        }

        val openPickerListener = { _: View ->
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "DATE_PICKER_TAG")
            }
        }
        binding.inputDate.setOnClickListener(openPickerListener)
        binding.layoutDate.setEndIconOnClickListener(openPickerListener)
    }

    private fun setupClickListeners() {
        binding.buttonAdd.setOnClickListener {
            saveOrUpdateTransaction()
        }

        binding.buttonList.setOnClickListener {
            binding.recyclerTransactions.smoothScrollToPosition(0)
            Toast.makeText(this, "Rolado para o topo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonBalance.setOnClickListener {
            showBalance()
        }
    }

    private fun saveOrUpdateTransaction() {
        val type = binding.dropdownType.text.toString()
        val category = binding.dropdownCategory.text.toString()
        val amountStr = binding.inputAmount.text.toString()
            .replace("R$", "")
            .replace(" ", "")
            .replace(',', '.')
        val dateStr = binding.inputDate.text.toString()

        var isValid = true
        if (type.isEmpty()) {
            binding.layoutType.error = "Selecione um tipo"
            isValid = false
        } else {
            binding.layoutType.error = null
        }

        if (type.isNotEmpty() && category.isEmpty()) {
            binding.layoutCategory.error = "Selecione uma categoria"
            isValid = false
        } else if (type.isNotEmpty()) {
            binding.layoutCategory.error = null
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.layoutAmount.error = "Insira um valor positivo válido"
            isValid = false
        } else {
            binding.layoutAmount.error = null
        }

        if (dateStr.isEmpty()) {
            binding.layoutDate.error = "Selecione uma data"
            isValid = false
        } else {
            try {
                dateFormat.isLenient = false
                dateFormat.parse(dateStr)
                binding.layoutDate.error = null
            } catch (e: Exception) {
                binding.layoutDate.error = "Formato de data inválido"
                isValid = false
            } finally {
                dateFormat.isLenient = true
            }
        }

        if (!isValid) return

        val transaction = Transaction(
            id = transactionToEdit?.id ?: 0,
            type = type,
            category = category,
            amount = amount!!,
            date = dateStr
        )

        if (transactionToEdit == null) {
            viewModel.insert(transaction)
            Toast.makeText(this, "Transação salva!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.update(transaction)
            Toast.makeText(this, "Transação atualizada!", Toast.LENGTH_SHORT).show()
        }

        clearInputFieldsAndExitEditMode()
    }

    override fun onEditClick(transaction: Transaction) {
        enterEditMode(transaction)
    }

    override fun onDeleteClick(transaction: Transaction) {
        showDeleteConfirmation(transaction)
    }

    private fun enterEditMode(transaction: Transaction) {
        transactionToEdit = transaction

        (binding.layoutType.editText as? AutoCompleteTextView)?.setText(transaction.type, false)
        updateCategoryDropdown(transaction.type, clearSelection = false)
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText(transaction.category, false)

        binding.inputAmount.setText(String.format(Locale.US, "%.2f", transaction.amount))
        binding.inputDate.setText(transaction.date)

        binding.buttonAdd.text = "Salvar Alterações"
        binding.buttonAdd.setIconResource(R.drawable.ic_save)

        binding.buttonList.isEnabled = false
        binding.buttonBalance.isEnabled = false

        binding.layoutType.requestFocus()
        clearInputErrors()
    }

    private fun cancelEdit() {
        clearInputFieldsAndExitEditMode()
        Toast.makeText(this, "Edição cancelada", Toast.LENGTH_SHORT).show()
    }

    private fun clearInputFieldsAndExitEditMode() {
        transactionToEdit = null

        (binding.layoutType.editText as? AutoCompleteTextView)?.setText("", false)
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText("", false)
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setAdapter(null)
        binding.layoutCategory.isEnabled = false
        binding.inputAmount.text?.clear()
        binding.inputDate.text?.clear()

        binding.buttonAdd.text = "Adicionar Transação"
        binding.buttonAdd.setIconResource(R.drawable.ic_add)

        binding.buttonList.isEnabled = true
        binding.buttonBalance.isEnabled = true

        clearInputErrors()
        currentFocus?.clearFocus()
    }

    private fun clearInputErrors() {
        binding.layoutType.error = null
        binding.layoutCategory.error = null
        binding.layoutAmount.error = null
        binding.layoutDate.error = null
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Transação")
            .setMessage("Tem certeza que deseja excluir esta transação?\n\n${transaction.category} (${transaction.type})\nData: ${transaction.date}\nValor: ${String.format(Locale("pt", "BR"), "R$ %.2f", transaction.amount)}")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.delete(transaction)
                Toast.makeText(this, "Transação excluída", Toast.LENGTH_SHORT).show()
                if(transactionToEdit?.id == transaction.id) {
                    clearInputFieldsAndExitEditMode()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showBalance() {
        viewModel.calculateBalance { balance ->
            AlertDialog.Builder(this)
                .setTitle("Saldo Atual")
                .setMessage("Seu saldo é: R$ ${String.format(Locale("pt", "BR"), "%.2f", balance)}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewModel.transactions.observe(this) { transactions ->
            transactionAdapter.submitList(transactions)

            if (transactionToEdit != null && transactions.none { it.id == transactionToEdit!!.id }) {
                cancelEdit()
                Toast.makeText(this, "A transação que você estava editando foi removida.", Toast.LENGTH_LONG).show()
            }
        }
    }
}