package com.example.av2_pos_moveis.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.av2_pos_moveis.R
import com.example.av2_pos_moveis.data.dao.TransactionDao
import com.example.av2_pos_moveis.databinding.FragmentReportBinding
import com.example.av2_pos_moveis.ui.adapter.DistributionAdapter
import com.example.av2_pos_moveis.viewmodel.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by activityViewModels()
    private lateinit var distributionAdapter: DistributionAdapter
    private var chartTextColor: Int = Color.BLACK

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    private val monthYearFormatInput = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val monthYearFormatOutput = SimpleDateFormat("MMM/yy", Locale("pt", "BR"))


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartTextColor = ContextCompat.getColor(requireContext(), R.color.chart_text_color)

        pieChart = binding.chartExpensesByCategory
        barChart = binding.chartMonthlyHistory

        setupDistributionList()
        setupCharts()
        observeViewModel()
    }

    private fun setupDistributionList(){
        distributionAdapter = DistributionAdapter()
        binding.recyclerDistribution.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = distributionAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.calculateBalance { balance ->
            binding.textTotalBalance.text = String.format(Locale("pt", "BR"), "R$ %.2f", balance)
        }

        viewModel.debitTotalsByCategory.observe(viewLifecycleOwner, Observer { categoryTotals ->
            distributionAdapter.submitList(categoryTotals)
            updatePieChartData(categoryTotals)
        })

        // Observa os dados REAIS do histórico mensal
        viewModel.monthlyDebitHistory.observe(viewLifecycleOwner, Observer { historyData ->
            updateBarChartData(historyData)
        })
    }

    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Despesas"
            setCenterTextColor(chartTextColor)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            setEntryLabelColor(chartTextColor)
            setEntryLabelTextSize(10f)
        }
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = chartTextColor
                valueFormatter = IndexAxisValueFormatter()
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = chartTextColor
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "R$ ${String.format(Locale("pt", "BR"), "%.0f", value)}"
                    }
                }
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1500)
        }
    }


    private fun updatePieChartData(data: List<TransactionDao.CategoryTotal>){
        if (data.isNullOrEmpty()) {
            pieChart.clear()
            pieChart.centerText = "Sem Despesas"
            pieChart.invalidate()
            return
        }
        pieChart.centerText = "Despesas"

        val entries = ArrayList<PieEntry>()
        data.forEach {
            entries.add(PieEntry(it.total.toFloat(), it.category))
        }

        val dataSet = PieDataSet(entries, "Categorias")
        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0f, 40f)
        dataSet.selectionShift = 5f

        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(11f)
        pieData.setValueTextColor(chartTextColor)

        pieChart.data = pieData
        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    private fun updateBarChartData(monthlyData: List<TransactionDao.MonthlyTotal>) {
        if (monthlyData.isNullOrEmpty()) {
            barChart.clear()
            barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        monthlyData.forEachIndexed { index, monthlyTotal ->
            entries.add(BarEntry(index.toFloat(), monthlyTotal.totalAmount.toFloat()))
            labels.add(formatYearMonthLabel(monthlyTotal.yearMonth))
        }

        (barChart.xAxis.valueFormatter as IndexAxisValueFormatter).values = labels.toTypedArray()
        barChart.xAxis.labelCount = labels.size

        val dataSet = BarDataSet(entries, "Histórico Mensal")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = chartTextColor
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R$ ${String.format(Locale("pt", "BR"), "%.0f", value)}"
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        barData.setValueTextSize(10f)

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.invalidate()
    }

    private fun formatYearMonthLabel(yearMonth: String): String {
        return try {
            val date = monthYearFormatInput.parse(yearMonth)
            if (date != null) monthYearFormatOutput.format(date) else yearMonth
        } catch (e: ParseException) {
            yearMonth
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}