package com.example.av2_pos_moveis.ui.adapter

import com.example.av2_pos_moveis.databinding.ItemDistributionBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.av2_pos_moveis.R
import com.example.av2_pos_moveis.data.dao.TransactionDao
import java.util.Locale

class DistributionAdapter : ListAdapter<TransactionDao.CategoryTotal, DistributionAdapter.DistributionViewHolder>(DistributionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DistributionViewHolder {
        val binding = ItemDistributionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DistributionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DistributionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DistributionViewHolder(private val binding: ItemDistributionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionDao.CategoryTotal) {
            binding.textDistributionCategory.text = item.category
            binding.textDistributionAmount.text = String.format(Locale("pt", "BR"), "R$ %.2f", item.total)
            val iconResId = getIconForCategory(item.category)
            binding.iconDistributionCategory.setImageResource(iconResId)
        }

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

    class DistributionDiffCallback : DiffUtil.ItemCallback<TransactionDao.CategoryTotal>() {
        override fun areItemsTheSame(oldItem: TransactionDao.CategoryTotal, newItem: TransactionDao.CategoryTotal): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: TransactionDao.CategoryTotal, newItem: TransactionDao.CategoryTotal): Boolean {
            return oldItem == newItem
        }
    }
}