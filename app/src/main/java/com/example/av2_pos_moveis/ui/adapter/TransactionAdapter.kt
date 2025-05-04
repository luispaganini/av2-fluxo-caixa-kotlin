package com.example.av2_pos_moveis.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.av2_pos_moveis.R
import com.example.av2_pos_moveis.data.model.Transaction
import com.example.av2_pos_moveis.databinding.ItemTransactionBinding
import java.util.Locale

interface OnTransactionActionListener {
    fun onEditClick(transaction: Transaction)
    fun onDeleteClick(transaction: Transaction)
}

class TransactionAdapter(
    private val listener: OnTransactionActionListener
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentTransaction = getItem(position)
        holder.bind(currentTransaction)
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val listener: OnTransactionActionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.textDate.text = transaction.date
            binding.textCategory.text = transaction.category


            val categoryIconResId = getIconForCategory(transaction.category)
            binding.iconCategory.setImageResource(categoryIconResId)

            val formattedAmount = String.format(Locale("pt", "BR"), "R$ %.2f", transaction.amount)

            if (transaction.type == "Crédito") {
                binding.textAmount.text = "+ $formattedAmount"
                binding.textAmount.setTextColor(ContextCompat.getColor(binding.root.context, R.color.credit_color))
            } else {
                binding.textAmount.text = "- $formattedAmount"
                binding.textAmount.setTextColor(ContextCompat.getColor(binding.root.context, R.color.debit_color))
            }

            binding.buttonEditItem.setOnClickListener {
                listener.onEditClick(transaction)
            }
            binding.buttonDeleteItem.setOnClickListener {
                listener.onDeleteClick(transaction)
            }
        }

        @DrawableRes
        private fun getIconForCategory(category: String): Int {
            return when (category.lowercase(Locale.getDefault())) {
                "salário" -> R.drawable.ic_work
                "alimentação" -> R.drawable.ic_restaurant
                "transporte" -> R.drawable.ic_direction_bus
                "saúde" -> R.drawable.ic_favorite
                "moradia" -> R.drawable.ic_home
                else -> R.drawable.ic_help_outline
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}