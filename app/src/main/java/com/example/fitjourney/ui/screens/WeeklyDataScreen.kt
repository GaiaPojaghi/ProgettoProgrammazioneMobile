package com.example.fitjourney.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fitjourney.ui.viewModel.StudyViewModel
import com.example.fitjourney.ui.viewModel.WeeklyDataViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDataScreen(
    studyViewModel: StudyViewModel,
    weeklyDataViewModel: WeeklyDataViewModel = remember { WeeklyDataViewModel() }
) {
    val weeklyData by weeklyDataViewModel.weeklyData.collectAsState()
    val currentFilter by weeklyDataViewModel.currentFilter.collectAsState()
    val currentPeriod by weeklyDataViewModel.currentPeriod.collectAsState()
    val isLoading by weeklyDataViewModel.isLoading.collectAsState()
    val isUserAuthenticated by weeklyDataViewModel.isUserAuthenticated.collectAsState()
    val errorMessage by weeklyDataViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        weeklyDataViewModel.loadWeeklyData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        if (!isUserAuthenticated) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accesso richiesto",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Per visualizzare le tue statistiche di studio, effettua il login.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header con sfondo blu e senza padding esterno
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF283593))
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 24.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (currentPeriod) {
                                        WeeklyDataViewModel.Period.DAILY -> "Statistiche di studio"
                                        WeeklyDataViewModel.Period.WEEKLY -> "Tendenze settimanali"
                                        WeeklyDataViewModel.Period.MONTHLY -> "Panoramica mensile"
                                    },
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (errorMessage != null) {
                                IconButton(
                                    onClick = { weeklyDataViewModel.clearError() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Chiudi messaggio errore",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Caricamento dati...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn()
                                ) {
                                    PeriodFilterRow(
                                        currentPeriod = currentPeriod,
                                        onPeriodChange = { weeklyDataViewModel.setPeriod(it) }
                                    )
                                }
                            }

                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        animationSpec = tween(durationMillis = 300, delayMillis = 100)
                                    ) + fadeIn()
                                ) {
                                    DataTypeFilterRow(
                                        currentFilter = currentFilter,
                                        onFilterChange = { weeklyDataViewModel.setFilter(it) }
                                    )
                                }
                            }

                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        animationSpec = tween(durationMillis = 300, delayMillis = 300)
                                    ) + fadeIn()
                                ) {
                                    when (currentPeriod) {
                                        WeeklyDataViewModel.Period.DAILY -> {
                                            DailyChartCard(
                                                weeklyData = weeklyData,
                                                filter = currentFilter
                                            )
                                        }
                                        WeeklyDataViewModel.Period.WEEKLY -> {
                                            WeeklyChartCard(
                                                weeklyData = weeklyData,
                                                filter = currentFilter
                                            )
                                        }
                                        WeeklyDataViewModel.Period.MONTHLY -> {
                                            MonthlyChartCard(
                                                weeklyData = weeklyData,
                                                filter = currentFilter
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        animationSpec = tween(durationMillis = 300, delayMillis = 400)
                                    ) + fadeIn()
                                ) {
                                    DetailedStatsCard(
                                        weeklyData = weeklyData,
                                        filter = currentFilter,
                                        period = currentPeriod,
                                        viewModel = weeklyDataViewModel
                                    )
                                }
                            }

                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        animationSpec = tween(durationMillis = 300, delayMillis = 500)
                                    ) + fadeIn()
                                ) {
                                    InsightsCard(
                                        weeklyData = weeklyData,
                                        period = currentPeriod
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    currentPeriod: WeeklyDataViewModel.Period,
    onPeriodChange: (WeeklyDataViewModel.Period) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(WeeklyDataViewModel.Period.values()) { period ->
            FilterChip(
                onClick = { onPeriodChange(period) },
                label = {
                    Text(
                        text = when (period) {
                            WeeklyDataViewModel.Period.DAILY -> "Giornaliero"
                            WeeklyDataViewModel.Period.WEEKLY -> "Settimanale"
                            WeeklyDataViewModel.Period.MONTHLY -> "Mensile"
                        }
                    )
                },
                selected = currentPeriod == period,
                leadingIcon = {
                    Icon(
                        imageVector = when (period) {
                            WeeklyDataViewModel.Period.DAILY -> Icons.Default.Today
                            WeeklyDataViewModel.Period.WEEKLY -> Icons.Default.DateRange
                            WeeklyDataViewModel.Period.MONTHLY -> Icons.Default.CalendarMonth
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DataTypeFilterRow(
    currentFilter: WeeklyDataViewModel.DataFilter,
    onFilterChange: (WeeklyDataViewModel.DataFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(WeeklyDataViewModel.DataFilter.values()) { filter ->
            FilterChip(
                onClick = { onFilterChange(filter) },
                label = {
                    Text(text = filter.displayName)
                },
                selected = currentFilter == filter,
                leadingIcon = {
                    Icon(
                        imageVector = filter.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DailyChartCard(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📊 Andamento Settimanale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Aumentato da 200dp a 300dp
            ) {
                if (weeklyData.isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun dato disponibile per questo periodo",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    DailyBarChart(
                        weeklyData = weeklyData,
                        filter = filter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChartCard(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📈 Tendenze Settimanali",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (weeklyData.isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun dato disponibile per questo periodo",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    WeeklyLineChart(
                        weeklyData = weeklyData,
                        filter = filter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyChartCard(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📅 Panoramica Mensile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (weeklyData.isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun dato disponibile per questo periodo",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    MonthlyBarChart(
                        weeklyData = weeklyData,
                        filter = filter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun DailyBarChart(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                setupBarChartStyle()
            }
        },
        update = { chart ->
            try {
                // Verifica che weeklyData non sia null e abbia dati
                if (weeklyData.isEmpty) {
                    chart.clear()
                    chart.invalidate()
                    return@AndroidView
                }

                val entries = when (filter) {
                    WeeklyDataViewModel.DataFilter.STUDY -> {
                        weeklyData.dailyStudyTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.BREAK -> {
                        weeklyData.dailyBreakTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.TOTAL -> {
                        weeklyData.dailyTotalTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.SESSIONS -> {
                        weeklyData.dailySessions.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value.toFloat()))
                            } ?: emptyList()
                    }
                }

                if (entries.isNotEmpty() && entries.any { it.y > 0 }) {
                    val dataSet = BarDataSet(entries, filter.displayName).apply {
                        color = when (filter) {
                            WeeklyDataViewModel.DataFilter.STUDY -> android.graphics.Color.parseColor("#4CAF50")
                            WeeklyDataViewModel.DataFilter.BREAK -> android.graphics.Color.parseColor("#2196F3")
                            WeeklyDataViewModel.DataFilter.TOTAL -> android.graphics.Color.parseColor("#FF9800")
                            WeeklyDataViewModel.DataFilter.SESSIONS -> android.graphics.Color.parseColor("#9C27B0")
                        }
                        valueTextColor = android.graphics.Color.BLACK
                        valueTextSize = 10f
                        setDrawValues(true)
                    }

                    val barData = BarData(dataSet)
                    barData.barWidth = 0.8f

                    chart.data = barData
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                        arrayOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
                    )

                    // Forza refresh del grafico
                    chart.notifyDataSetChanged()
                    chart.invalidate()

                    // Anima il grafico
                    chart.animateY(800)
                } else {
                    // Crea dati vuoti ma visibili
                    val emptyEntries = (0..6).map { BarEntry(it.toFloat(), 0.1f) }
                    val dataSet = BarDataSet(emptyEntries, "Nessun dato").apply {
                        color = android.graphics.Color.LTGRAY
                        setDrawValues(false)
                    }

                    chart.data = BarData(dataSet)
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                        arrayOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
                    )
                    chart.invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In caso di errore, mostra grafico con dati di esempio
                val fallbackEntries = (0..6).map { BarEntry(it.toFloat(), 1.0f) }
                val dataSet = BarDataSet(fallbackEntries, "Dati di esempio").apply {
                    color = android.graphics.Color.GRAY
                    setDrawValues(false)
                }
                chart.data = BarData(dataSet)
                chart.invalidate()
            }
        }
    )
}

@Composable
fun WeeklyLineChart(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                setupLineChartStyle()
            }
        },
        update = { chart ->
            try {
                if (weeklyData.isEmpty) {
                    chart.clear()
                    chart.invalidate()
                    return@AndroidView
                }

                val entries = when (filter) {
                    WeeklyDataViewModel.DataFilter.STUDY -> {
                        weeklyData.weeklyStudyTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                Entry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.BREAK -> {
                        weeklyData.weeklyBreakTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                Entry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.TOTAL -> {
                        weeklyData.weeklyTotalTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                Entry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.SESSIONS -> {
                        weeklyData.weeklySessions.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                Entry(index.toFloat(), maxOf(0f, value.toFloat()))
                            } ?: emptyList()
                    }
                }

                if (entries.isNotEmpty()) {
                    val dataSet = LineDataSet(entries, filter.displayName).apply {
                        color = when (filter) {
                            WeeklyDataViewModel.DataFilter.STUDY -> android.graphics.Color.parseColor("#4CAF50")
                            WeeklyDataViewModel.DataFilter.BREAK -> android.graphics.Color.parseColor("#2196F3")
                            WeeklyDataViewModel.DataFilter.TOTAL -> android.graphics.Color.parseColor("#FF9800")
                            WeeklyDataViewModel.DataFilter.SESSIONS -> android.graphics.Color.parseColor("#9C27B0")
                        }
                        lineWidth = 3f
                        circleRadius = 5f
                        setCircleColor(color)
                        circleHoleRadius = 2f
                        valueTextColor = android.graphics.Color.BLACK
                        valueTextSize = 10f
                        setDrawValues(true)
                        setDrawFilled(false)
                        setDrawCircles(true)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }

                    chart.data = LineData(dataSet)
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                        arrayOf("Sett 4", "Sett 3", "Sett 2", "Sett 1")
                    )

                    chart.notifyDataSetChanged()
                    chart.invalidate()

                    if (entries.any { it.y > 0 }) {
                        chart.animateY(800)
                    }
                } else {
                    chart.clear()
                    chart.invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                chart.clear()
                chart.invalidate()
            }
        }
    )
}

@Composable
fun MonthlyBarChart(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                setupBarChartStyle()
            }
        },
        update = { chart ->
            try {
                if (weeklyData.isEmpty) {
                    chart.clear()
                    chart.invalidate()
                    return@AndroidView
                }

                val entries = when (filter) {
                    WeeklyDataViewModel.DataFilter.STUDY -> {
                        weeklyData.monthlyStudyTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.BREAK -> {
                        weeklyData.monthlyBreakTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.TOTAL -> {
                        weeklyData.monthlyTotalTime.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value))
                            } ?: emptyList()
                    }
                    WeeklyDataViewModel.DataFilter.SESSIONS -> {
                        weeklyData.monthlySessions.takeIf { it.isNotEmpty() }
                            ?.mapIndexed { index, value ->
                                BarEntry(index.toFloat(), maxOf(0f, value.toFloat()))
                            } ?: emptyList()
                    }
                }

                if (entries.isNotEmpty()) {
                    val dataSet = BarDataSet(entries, filter.displayName).apply {
                        color = when (filter) {
                            WeeklyDataViewModel.DataFilter.STUDY -> android.graphics.Color.parseColor("#4CAF50")
                            WeeklyDataViewModel.DataFilter.BREAK -> android.graphics.Color.parseColor("#2196F3")
                            WeeklyDataViewModel.DataFilter.TOTAL -> android.graphics.Color.parseColor("#FF9800")
                            WeeklyDataViewModel.DataFilter.SESSIONS -> android.graphics.Color.parseColor("#9C27B0")
                        }
                        valueTextColor = android.graphics.Color.BLACK
                        valueTextSize = 10f
                        setDrawValues(true)
                    }

                    val barData = BarData(dataSet)
                    barData.barWidth = 0.8f

                    chart.data = barData
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                        arrayOf("M6", "M5", "M4", "M3", "M2", "M1")
                    )

                    chart.notifyDataSetChanged()
                    chart.invalidate()

                    if (entries.any { it.y > 0 }) {
                        chart.animateY(800)
                    }
                } else {
                    chart.clear()
                    chart.invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                chart.clear()
                chart.invalidate()
            }
        }
    )
}

private fun BarChart.setupBarChartStyle() {
    // Disabilita descrizione
    description.isEnabled = false

    // Configurazione generale
    setDrawGridBackground(false)
    setDrawBarShadow(false)
    setDrawValueAboveBar(true)
    setMaxVisibleValueCount(60)
    setPinchZoom(false)
    setDrawGridBackground(false)
    setTouchEnabled(true)

    // Configurazione asse X
    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(true)
        granularity = 1f
        isGranularityEnabled = true
        labelCount = 7
        textColor = android.graphics.Color.BLACK
        textSize = 14f
        setLabelCount(7, false)
        yOffset = 10f
    }

    // Configurazione asse Y sinistro
    axisLeft.apply {
        setDrawGridLines(true)
        setDrawAxisLine(true)
        axisMinimum = 0f
        textColor = android.graphics.Color.BLACK
        textSize = 14f
        gridColor = android.graphics.Color.LTGRAY
        gridLineWidth = 0.5f
        setStartAtZero(true)
        spaceTop = 10f
    }

    // Disabilita asse Y destro
    axisRight.isEnabled = false

    // Configurazione legenda
    legend.isEnabled = false

    // Configurazione touch
    setDragEnabled(true)
    setScaleEnabled(false)
    isDoubleTapToZoomEnabled = false

    // Margini e viewport
    setExtraOffsets(15f, 20f, 15f, 20f)
    setFitBars(true)
}

private fun LineChart.setupLineChartStyle() {
    // Disabilita descrizione
    description.isEnabled = false

    // Configurazione generale
    setDrawGridBackground(false)
    setMaxVisibleValueCount(60)
    setTouchEnabled(true)
    setPinchZoom(true)
    setDrawGridBackground(false)

    // Configurazione asse X
    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(false)
        setDrawAxisLine(true)
        granularity = 1f
        isGranularityEnabled = true
        labelCount = 4
        textColor = android.graphics.Color.BLACK
        textSize = 14f
        setLabelCount(4, false)
        yOffset = 10f
    }

    // Configurazione asse Y sinistro
    axisLeft.apply {
        setDrawGridLines(true)
        setDrawAxisLine(true)
        axisMinimum = 0f
        textColor = android.graphics.Color.BLACK
        textSize = 14f
        gridColor = android.graphics.Color.LTGRAY
        gridLineWidth = 0.5f
        setStartAtZero(true)
        spaceTop = 10f
    }

    // Disabilita asse Y destro
    axisRight.isEnabled = false

    // Configurazione legenda
    legend.isEnabled = false

    // Configurazione touch
    setDragEnabled(true)
    setScaleEnabled(true)
    isDoubleTapToZoomEnabled = false

    // Margini
    setExtraOffsets(15f, 20f, 15f, 20f)
}

@Composable
private fun DetailedStatsCard(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    filter: WeeklyDataViewModel.DataFilter,
    period: WeeklyDataViewModel.Period,
    viewModel: WeeklyDataViewModel
) {
    val dataList: List<Float> = when (filter) {
        WeeklyDataViewModel.DataFilter.STUDY -> weeklyData.dailyStudyTime
        WeeklyDataViewModel.DataFilter.BREAK -> weeklyData.dailyBreakTime
        WeeklyDataViewModel.DataFilter.TOTAL -> weeklyData.dailyTotalTime
        WeeklyDataViewModel.DataFilter.SESSIONS -> weeklyData.dailySessions.map { it.toFloat() }
    }

    // CARICA LA STREAK QUANDO CAMBIA IL FILTRO
    LaunchedEffect(filter) {
        viewModel.getCurrentStreak(filter)
    }

    val bestDayIndex = if (dataList.isNotEmpty()) {
        dataList.indices.maxByOrNull { dataList[it] } ?: -1
    } else -1

    val bestDay = if (bestDayIndex >= 0) {
        listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")
            .getOrNull(bestDayIndex) ?: "N/D"
    } else "N/D"

    val currentStreak = weeklyData.currentStreak

    // CALCOLA LA MEDIA SOLO SUI GIORNI TRASCORSI
    val daysElapsed = getDaysElapsedInWeek()
    val average = if (dataList.isNotEmpty() && daysElapsed > 0) {
        dataList.take(daysElapsed).average()
    } else 0.0

    val goalPercentage = calculateGoalPercentage(dataList.take(daysElapsed), filter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🔍 Statistiche Dettagliate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(4) { index ->
                    DetailedStatItem(
                        title = when (index) {
                            0 -> "Miglior Giorno"
                            1 -> "Streak Attuale"
                            2 -> "Media Settimanale"
                            else -> "Obiettivo"
                        },
                        value = when (index) {
                            0 -> bestDay
                            1 -> "$currentStreak giorni"
                            2 -> {
                                // I dati sono già in ore
                                if (average < 1 && average > 0) {
                                    "%.0f min".format(average * 60f)
                                } else {
                                    "%.1f h".format(average)
                                }
                            }
                            else -> "$goalPercentage%"
                        },
                        color = when (index) {
                            0 -> Color(0xFF4CAF50)
                            1 -> Color(0xFF2196F3)
                            2 -> Color(0xFFFF9800)
                            else -> Color(0xFF9C27B0)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedStatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InsightsCard(
    weeklyData: WeeklyDataViewModel.WeeklyStatistics,
    period: WeeklyDataViewModel.Period
) {
    val insights = generateInsights(weeklyData)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "💡 Suggerimenti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                insights.forEach { insight ->
                    InsightItem(text = insight.text, icon = insight.icon)
                }
            }
        }
    }
}

data class Insight(val text: String, val icon: ImageVector)

fun generateInsights(weeklyData: WeeklyDataViewModel.WeeklyStatistics): List<Insight> {
    val insights = mutableListOf<Insight>()

    // Giornate feriali vs weekend
    val weekdays = weeklyData.dailyStudyTime.take(5)
    val weekend = weeklyData.dailyStudyTime.takeLast(2)
    val weekdaysAvg = weekdays.averageOrNull()
    val weekendAvg = weekend.averageOrNull()

    if (weekdaysAvg != null && weekendAvg != null) {
        if (weekdaysAvg > weekendAvg) {
            insights.add(
                Insight(
                    text = "Le tue performance sono migliori nei giorni feriali. Continua così!",
                    icon = Icons.Default.TrendingUp
                )
            )
        } else if (weekendAvg > weekdaysAvg) {
            insights.add(
                Insight(
                    text = "Rendi il weekend produttivo: ottimi risultati anche nei giorni di riposo!",
                    icon = Icons.Default.EmojiEvents
                )
            )
        }
    }

    // Pause troppo corte o assenti
    val avgBreak = weeklyData.dailyBreakTime.averageOrNull()
    if (avgBreak != null && avgBreak < 10f) {
        insights.add(
            Insight(
                text = "Fai pause più frequenti: una mente riposata è più produttiva.",
                icon = Icons.Default.Psychology
            )
        )
    }

    // Default se mancano dati
    if (insights.isEmpty()) {
        insights.add(
            Insight(
                text = "Continua a monitorare i tuoi dati per ricevere suggerimenti personalizzati!",
                icon = Icons.Default.Info
            )
        )
    }

    return insights.take(3)
}

@Composable
private fun InsightItem(
    text: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

fun getDaysElapsedInWeek(): Int {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    return when (dayOfWeek) {
        Calendar.SUNDAY -> 7
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 1
    }
}

fun calculateGoalPercentage(data: List<Float>, filter: WeeklyDataViewModel.DataFilter): Int {
    if (data.isEmpty()) return 0

    val goalPerDay = when (filter) {
        WeeklyDataViewModel.DataFilter.STUDY -> 2f // ore (120 minuti = 2 ore)
        WeeklyDataViewModel.DataFilter.BREAK -> 0.5f  // ore (30 minuti = 0.5 ore)
        WeeklyDataViewModel.DataFilter.TOTAL -> 2.5f // ore (150 minuti = 2.5 ore)
        WeeklyDataViewModel.DataFilter.SESSIONS -> 4f // numero sessioni
    }

    val daysMet = data.count { it >= goalPerDay }
    return ((daysMet.toFloat() / data.size.toFloat()) * 100f).toInt()
}

fun List<Float>.averageOrNull(): Float? =
    if (isNotEmpty()) average().toFloat() else null

