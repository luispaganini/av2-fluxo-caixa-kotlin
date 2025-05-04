package com.example.av2_pos_moveis.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.av2_pos_moveis.R
import com.example.av2_pos_moveis.data.model.Transaction
import com.example.av2_pos_moveis.databinding.FragmentStatementBinding
import com.example.av2_pos_moveis.ui.adapter.OnTransactionActionListener
import com.example.av2_pos_moveis.ui.adapter.TransactionAdapter
import com.example.av2_pos_moveis.viewmodel.TransactionViewModel
import java.util.Locale

class StatementFragment : Fragment(), OnTransactionActionListener {

    private var _binding: FragmentStatementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(this)
        binding.recyclerStatement.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.allTransactions.observe(viewLifecycleOwner, Observer { transactions ->
            transactionAdapter.submitList(transactions)
        })

        viewModel.getTotalCredits { credits ->
            binding.textRecipesTotal.text = String.format(Locale("pt", "BR"), "R$ %.2f", credits)
        }
        viewModel.getTotalDebits { debits ->
            binding.textExpensesTotal.text = String.format(Locale("pt", "BR"), "R$ %.2f", debits)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onEditClick(transaction: Transaction) {
        val bundle = Bundle().apply {
            putInt("transactionId", transaction.id)
        }
        try {
            findNavController().navigate(R.id.navigation_home_entry, bundle)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao tentar navegar para edição.", Toast.LENGTH_SHORT).show()
            android.util.Log.e("StatementNavError", "Navigation failed", e)
        }
    }

    override fun onDeleteClick(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Transação")
            .setMessage("Tem certeza que deseja excluir esta transação?\n${transaction.description}")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.delete(transaction)
                Toast.makeText(requireContext(), "Transação excluída", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}