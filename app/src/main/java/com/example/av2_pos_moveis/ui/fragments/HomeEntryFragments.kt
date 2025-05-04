package com.example.av2_pos_moveis.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.av2_pos_moveis.R
import com.example.av2_pos_moveis.data.model.Transaction
import com.example.av2_pos_moveis.databinding.FragmentHomeEntryBinding
import com.example.av2_pos_moveis.ui.adapter.OnTransactionActionListener
import com.example.av2_pos_moveis.ui.adapter.TransactionAdapter
import com.example.av2_pos_moveis.viewmodel.TransactionViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class HomeEntryFragment : Fragment(), OnTransactionActionListener {

    private var _binding: FragmentHomeEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var recentTransactionAdapter: TransactionAdapter

    private val creditCategories = listOf("Salário", "Extras")
    private val debitCategories = listOf("Alimentação", "Transporte", "Saúde", "Moradia")

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentSelectedType: String = "Débito"

    private var editTransactionId: Int = -1
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            editTransactionId = it.getInt("transactionId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggle()
        setupCategoryDropdown()
        setupDatePicker()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        if (editTransactionId != -1) {
            viewModel.loadTransactionForEdit(editTransactionId)
        } else {
            viewModel.doneEditing()
            clearInputFieldsAndResetState()
        }
    }

    private fun setupToggle() {
        binding.toggleButtonType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.buttonCredit -> currentSelectedType = "Crédito"
                    R.id.buttonDebit -> currentSelectedType = "Débito"
                }
                updateCategoryDropdown()
            }
        }
        binding.toggleButtonType.check(R.id.buttonDebit)
    }

    private fun setupCategoryDropdown() {
        val categories = if (currentSelectedType == "Crédito") creditCategories else debitCategories
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setAdapter(categoryAdapter)
        if (!isEditMode) {
            (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText("", false)
        }
    }

    private fun updateCategoryDropdown() {
        setupCategoryDropdown()
        binding.layoutCategory.error = null
    }


    private fun setupDatePicker() {
        val inputDateEditText = binding.layoutDate.editText as? AutoCompleteTextView
        inputDateEditText?.isFocusable = false
        inputDateEditText?.isClickable = true

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione a data")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val selectedDate = dateFormat.format(calendar.time)
            inputDateEditText?.setText(selectedDate)
            binding.layoutDate.error = null
        }

        val openPickerListener = { _: View ->
            if (!datePicker.isAdded) {
                datePicker.show(parentFragmentManager, "HOME_DATE_PICKER_TAG")
            }
        }
        inputDateEditText?.setOnClickListener(openPickerListener)
        binding.layoutDate.setEndIconOnClickListener(openPickerListener)
    }

    private fun setupRecyclerView() {
        recentTransactionAdapter = TransactionAdapter(this)
        binding.recyclerRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentTransactionAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonLaunch.setOnClickListener {
            saveTransaction()
        }
        binding.buttonBalanceHome.setOnClickListener {
            showBalanceDialog()
        }
    }

    private fun populateFieldsForEdit(transaction: Transaction) {
        if (transaction.type == "Crédito") {
            binding.toggleButtonType.check(R.id.buttonCredit)
            currentSelectedType = "Crédito"
        } else {
            binding.toggleButtonType.check(R.id.buttonDebit)
            currentSelectedType = "Débito"
        }
        updateCategoryDropdown()

        binding.inputDescription.setText(transaction.description ?: "")
        binding.inputAmount.setText(String.format(Locale.US, "%.2f", transaction.amount))
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText(transaction.category, false)
        (binding.layoutDate.editText as? AutoCompleteTextView)?.setText(transaction.date)
    }


    private fun saveTransaction() {
        val type = currentSelectedType
        val description = binding.inputDescription.text.toString().trim()
        val amountStr = binding.inputAmount.text.toString().replace(',', '.')
        val category = (binding.layoutCategory.editText as? AutoCompleteTextView)?.text.toString()
        val dateStr = (binding.layoutDate.editText as? AutoCompleteTextView)?.text.toString()

        var isValid = true

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.layoutAmount.error = "Insira um valor positivo válido"
            isValid = false
        } else {
            binding.layoutAmount.error = null
        }

        if (category.isEmpty()) {
            binding.layoutCategory.error = "Selecione uma categoria"
            isValid = false
        } else {
            binding.layoutCategory.error = null
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
            id = if (isEditMode) editTransactionId else 0,
            type = type,
            category = category,
            description = description.ifEmpty { null },
            amount = amount!!,
            date = dateStr
        )

        if (isEditMode) {
            viewModel.update(transaction)
            Toast.makeText(requireContext(), "Transação atualizada!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(transaction)
            Toast.makeText(requireContext(), "Transação salva!", Toast.LENGTH_SHORT).show()
        }

        viewModel.doneEditing()
        clearInputFieldsAndResetState()
    }

    private fun clearInputFieldsAndResetState() {
        isEditMode = false
        editTransactionId = -1
        binding.buttonLaunch.text = "Lançar"
        clearInputFields()
    }

    private fun clearInputFields() {
        binding.inputDescription.text?.clear()
        binding.inputAmount.text?.clear()
        (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText("", false)
        (binding.layoutDate.editText as? AutoCompleteTextView)?.setText("")
        binding.layoutDescription.error = null
        binding.layoutAmount.error = null
        binding.layoutCategory.error = null
        binding.layoutDate.error = null
        binding.toggleButtonType.check(R.id.buttonDebit)
        currentSelectedType = "Débito"
        updateCategoryDropdown()
        view?.clearFocus()
    }

    private fun showBalanceDialog() {
        viewModel.calculateBalance { balance ->
            AlertDialog.Builder(requireContext())
                .setTitle("Saldo Atual")
                .setMessage("Seu saldo é: R$ ${String.format(Locale("pt", "BR"), "%.2f", balance)}")
                .setPositiveButton("OK", null)
                .show()
        }
    }


    private fun observeViewModel() {
        viewModel.getRecentTransactions(5).observe(viewLifecycleOwner, Observer { transactions ->
            recentTransactionAdapter.submitList(transactions)
        })

        viewModel.transactionToEdit.observe(viewLifecycleOwner, Observer { transaction ->
            if (transaction != null) {
                populateFieldsForEdit(transaction)
                isEditMode = true
                binding.buttonLaunch.text = "Salvar Alterações"
            } else {
                if (isEditMode) {
                    clearInputFieldsAndResetState()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (isEditMode) {
            viewModel.doneEditing()
        }
    }

    override fun onEditClick(transaction: Transaction) {
        editTransactionId = transaction.id
        viewModel.loadTransactionForEdit(transaction.id)
    }

    override fun onDeleteClick(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Transação Recente")
            .setMessage("Tem certeza que deseja excluir esta transação de ${transaction.category}?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.delete(transaction)
                Toast.makeText(requireContext(), "Transação excluída", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}