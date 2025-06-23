package com.example.fitjourney.ui.viewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.fitjourney.util.viewModel.BaseViewModel
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class WeeklyDataViewModel : BaseViewModel() {

    private val _weeklyData = MutableStateFlow(WeeklyStatistics())
    val weeklyData: StateFlow<WeeklyStatistics> = _weeklyData

    private val _currentFilter = MutableStateFlow(DataFilter.STUDY)
    val currentFilter: StateFlow<DataFilter> = _currentFilter

    private val _currentPeriod = MutableStateFlow(Period.DAILY)
    val currentPeriod: StateFlow<Period> = _currentPeriod

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUserAuthenticated = MutableStateFlow(false)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    enum class DataFilter(val displayName: String, val icon: ImageVector) {
        STUDY("Studio", Icons.Default.School),
        BREAK("Pause", Icons.Default.Coffee),
        TOTAL("Totale", Icons.Default.Timer),
        SESSIONS("Sessioni", Icons.Default.PlayArrow)
    }

    enum class Period(val displayName: String, val icon: ImageVector) {
        DAILY("Giornaliero", Icons.Default.Today),
        WEEKLY("Settimanale", Icons.Default.DateRange),
        MONTHLY("Mensile", Icons.Default.CalendarMonth)
    }

    data class WeeklyStatistics(
        val totalStudyTime: Int = 0,
        val totalBreakTime: Int = 0,
        val totalTime: Int = 0,
        val totalSessions: Int = 0,
        val dailyStudyTime: List<Float> = emptyList(),
        val dailyBreakTime: List<Float> = emptyList(),
        val dailyTotalTime: List<Float> = emptyList(),
        val dailySessions: List<Int> = emptyList(),
        val weeklyStudyTime: List<Float> = emptyList(),
        val weeklyBreakTime: List<Float> = emptyList(),
        val weeklyTotalTime: List<Float> = emptyList(),
        val weeklySessions: List<Int> = emptyList(),
        val monthlyStudyTime: List<Float> = emptyList(),
        val monthlyBreakTime: List<Float> = emptyList(),
        val monthlyTotalTime: List<Float> = emptyList(),
        val monthlySessions: List<Int> = emptyList(),
        val weeklyStudyGoal: Int = 1260,
        val weeklyBreakGoal: Int = 420,
        val studyGoalProgress: Float = 0f,
        val breakGoalProgress: Float = 0f,
        val averageStudyPerDay: Float = 0f,
        val averageBreakPerDay: Float = 0f,
        val mostProductiveDay: String = "",
        val weekStartDate: String = "",
        val weekEndDate: String = "",
        val trendPercentage: Float = 0f,
        val bestWeek: String = "",
        val bestMonth: String = "",
        val isEmpty: Boolean = true,
        val currentStreak: Int = 0
    )

    init {
        checkUserAuthentication()
    }

    private fun checkUserAuthentication() {
        val userId = getCurrentUserId()
        _isUserAuthenticated.value = userId != null

        if (userId != null) {
            loadWeeklyData()
        } else {
            _errorMessage.value = "Effettua il login per visualizzare le tue statistiche di studio"
            resetToEmptyState()
        }
    }

    private fun resetToEmptyState() {
        _weeklyData.value = WeeklyStatistics(isEmpty = true)
        _isLoading.value = false
    }

    fun setFilter(filter: DataFilter) {
        if (!_isUserAuthenticated.value) {
            _errorMessage.value = "Effettua il login per visualizzare le statistiche"
            return
        }
        _currentFilter.value = filter
    }

    fun setPeriod(period: Period) {
        if (!_isUserAuthenticated.value) {
            _errorMessage.value = "Effettua il login per visualizzare le statistiche"
            return
        }
        _currentPeriod.value = period
        loadWeeklyData()
    }

    fun loadWeeklyData() {
        val userId = getCurrentUserId()
        if (userId == null) {
            _isUserAuthenticated.value = false
            _errorMessage.value = "Sessione scaduta. Effettua nuovamente il login"
            resetToEmptyState()
            return
        }

        _isUserAuthenticated.value = true
        _errorMessage.value = null
        _isLoading.value = true

        when (_currentPeriod.value) {
            Period.DAILY -> loadDailyData(userId)
            Period.WEEKLY -> loadWeeklyData(userId)
            Period.MONTHLY -> loadMonthlyData(userId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun getMondayOfCurrentWeek(calendar: Calendar): Calendar {
        val mondayCalendar = calendar.clone() as Calendar
        val dayOfWeek = mondayCalendar.get(Calendar.DAY_OF_WEEK)
        // Calendar: DOM=1, LUN=2, ..., SAB=7
        val diff = if (dayOfWeek == Calendar.MONDAY) 0 else (dayOfWeek + 6) % 7
        mondayCalendar.add(Calendar.DAY_OF_MONTH, -diff)
        return mondayCalendar
    }

    private fun loadDailyData(userId: String) {
        // Tutto come prima, ma senza la funzione finalizeWeeklyData() annidata

        // Variabili temporanee per raccogliere i dati
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.timeZone = calendar.timeZone

        val weekStart = getMondayOfCurrentWeek(calendar)
        val weekStartDate = dateFormat.format(weekStart.time)

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_MONTH, 6)
        val weekEndDate = dateFormat.format(weekEnd.time)

        // Map temporaneo per salvare i dati
        val tempStudyTimeMap = mutableMapOf<String, Float>()
        val tempBreakTimeMap = mutableMapOf<String, Float>()
        val tempTotalTimeMap = mutableMapOf<String, Float>()
        val tempSessionsMap = mutableMapOf<String, Int>()

        var daysProcessed = 0

        for (i in 0..6) {
            val currentDay = weekStart.clone() as Calendar
            currentDay.add(Calendar.DAY_OF_MONTH, i)
            val currentDate = dateFormat.format(currentDay.time)

            firestore.collection("users")
                .document(userId)
                .collection("studyData")
                .document(currentDate)
                .get()
                .addOnSuccessListener { document ->
                    val studyTimeMinutes = document.getLong("activeStudyTime")?.toFloat()?.div(60) ?: 0f
                    val breakTimeMinutes = document.getLong("breakTime")?.toFloat()?.div(60) ?: 0f
                    val sessions = document.getLong("sessionsCompleted")?.toInt() ?: 0

                    tempStudyTimeMap[currentDate] = studyTimeMinutes
                    tempBreakTimeMap[currentDate] = breakTimeMinutes
                    tempTotalTimeMap[currentDate] = studyTimeMinutes + breakTimeMinutes
                    tempSessionsMap[currentDate] = sessions

                    daysProcessed++
                    if (daysProcessed == 7) {
                        finalizeWeeklyData(
                            tempStudyTimeMap,
                            tempBreakTimeMap,
                            tempTotalTimeMap,
                            tempSessionsMap,
                            weekStart,
                            weekStartDate,
                            weekEndDate
                        )
                    }
                }
                .addOnFailureListener {
                    tempStudyTimeMap[currentDate] = 0f
                    tempBreakTimeMap[currentDate] = 0f
                    tempTotalTimeMap[currentDate] = 0f
                    tempSessionsMap[currentDate] = 0

                    daysProcessed++
                    if (daysProcessed == 7) {
                        finalizeWeeklyData(
                            tempStudyTimeMap,
                            tempBreakTimeMap,
                            tempTotalTimeMap,
                            tempSessionsMap,
                            weekStart,
                            weekStartDate,
                            weekEndDate
                        )
                        _errorMessage.value = "Errore nel caricamento di alcuni dati"
                    }
                }
        }
    }

    private fun finalizeWeeklyData(
        tempStudyTimeMap: Map<String, Float>,
        tempBreakTimeMap: Map<String, Float>,
        tempTotalTimeMap: Map<String, Float>,
        tempSessionsMap: Map<String, Int>,
        weekStart: Calendar,
        weekStartDate: String,
        weekEndDate: String
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyStudyTime = mutableListOf<Float>()
        val dailyBreakTime = mutableListOf<Float>()
        val dailyTotalTime = mutableListOf<Float>()
        val dailySessions = mutableListOf<Int>()

        var totalStudy = 0
        var totalBreak = 0
        var totalSessions = 0

        for (i in 0..6) {
            val day = weekStart.clone() as Calendar
            day.add(Calendar.DAY_OF_MONTH, i)
            val dateKey = dateFormat.format(day.time)

            val study = tempStudyTimeMap[dateKey] ?: 0f
            val brk = tempBreakTimeMap[dateKey] ?: 0f
            val tot = tempTotalTimeMap[dateKey] ?: 0f
            val sess = tempSessionsMap[dateKey] ?: 0

            dailyStudyTime.add(study)
            dailyBreakTime.add(brk)
            dailyTotalTime.add(tot)
            dailySessions.add(sess)

            totalStudy += study.toInt()
            totalBreak += brk.toInt()
            totalSessions += sess
        }

        updateWeeklyStatistics(
            totalStudy = totalStudy,
            totalBreak = totalBreak,
            totalSessions = totalSessions,
            dailyStudyTime = dailyStudyTime,
            dailyBreakTime = dailyBreakTime,
            dailyTotalTime = dailyTotalTime,
            dailySessions = dailySessions,
            weekStartDate = weekStartDate,
            weekEndDate = weekEndDate
        )
        _isLoading.value = false
    }

    // Classe di supporto per i dati della settimana (definita a livello di classe)
    data class WeekDataHelper(
        var studyTime: Float = 0f,
        var breakTime: Float = 0f,
        var sessions: Int = 0,
        var daysProcessed: Int = 0
    )

    private fun loadWeeklyData(userId: String) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Usa una struttura dati ordinata per mantenere l'ordine delle settimane
        val weeklyResults = mutableMapOf<Int, WeekDataHelper>()
        var weeksCompleted = 0

        for (weekOffset in 0..3) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.add(Calendar.WEEK_OF_YEAR, -weekOffset)

            // Imposta correttamente l'inizio della settimana
            weekCalendar.firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = weekCalendar.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            weekCalendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

            // Inizializza i dati per questa settimana
            weeklyResults[weekOffset] = WeekDataHelper()

            // Usa Tasks.whenAllComplete per aspettare che tutti i giorni siano processati
            val dayTasks = mutableListOf<Task<DocumentSnapshot>>()

            for (dayOffset in 0..6) {
                val dayCalendar = weekCalendar.clone() as Calendar
                dayCalendar.add(Calendar.DAY_OF_MONTH, dayOffset)
                val currentDate = dateFormat.format(dayCalendar.time)

                val task = firestore.collection("users")
                    .document(userId)
                    .collection("studyData")
                    .document(currentDate)
                    .get()

                dayTasks.add(task)
            }

            // Processa tutti i giorni di questa settimana
            Tasks.whenAllComplete(dayTasks).addOnCompleteListener { _ ->
                val weekData = weeklyResults[weekOffset]!!

                // Itera sui task originali, non sul risultato di whenAllComplete
                for (task in dayTasks) {
                    if (task.isSuccessful) {
                        val document = task.result
                        val studyTimeMinutes = document.getLong("activeStudyTime")?.toFloat()?.div(60) ?: 0f
                        val breakTimeMinutes = document.getLong("breakTime")?.toFloat()?.div(60) ?: 0f
                        val sessions = document.getLong("sessionsCompleted")?.toInt() ?: 0

                        weekData.studyTime += studyTimeMinutes
                        weekData.breakTime += breakTimeMinutes
                        weekData.sessions += sessions
                    }
                    weekData.daysProcessed++
                }

                weeksCompleted++

                // Quando tutte le 4 settimane sono complete, aggiorna l'UI
                if (weeksCompleted == 4) {
                    processWeeklyResults(weeklyResults)
                }
            }
        }
    }

    private fun processWeeklyResults(weeklyResults: Map<Int, WeekDataHelper>) {
        // Ordina i risultati per weekOffset (0, 1, 2, 3)
        val sortedResults = weeklyResults.toSortedMap()

        val weeklyStudyTime = mutableListOf<Float>()
        val weeklyBreakTime = mutableListOf<Float>()
        val weeklyTotalTime = mutableListOf<Float>()
        val weeklySessions = mutableListOf<Int>()

        var totalStudy = 0
        var totalBreak = 0
        var totalSessions = 0

        sortedResults.forEach { (weekOffset, weekData) ->
            weeklyStudyTime.add(weekData.studyTime)
            weeklyBreakTime.add(weekData.breakTime)
            weeklyTotalTime.add(weekData.studyTime + weekData.breakTime)
            weeklySessions.add(weekData.sessions)

            // I dati della settimana corrente (weekOffset == 0)
            if (weekOffset == 0) {
                totalStudy = weekData.studyTime.toInt()
                totalBreak = weekData.breakTime.toInt()
                totalSessions = weekData.sessions
            }
        }

        // Inverti le liste per avere l'ordine corretto (settimana più vecchia prima)
        updateWeeklyStatisticsForWeeklyView(
            totalStudy = totalStudy,
            totalBreak = totalBreak,
            totalSessions = totalSessions,
            weeklyStudyTime = weeklyStudyTime.reversed(),
            weeklyBreakTime = weeklyBreakTime.reversed(),
            weeklyTotalTime = weeklyTotalTime.reversed(),
            weeklySessions = weeklySessions.reversed()
        )

        _isLoading.value = false

        // Controlla se non ci sono dati
        if (totalStudy == 0 && totalBreak == 0 && totalSessions == 0) {
            val hasAnyData = weeklyStudyTime.any { it > 0 } ||
                    weeklyBreakTime.any { it > 0 } ||
                    weeklySessions.any { it > 0 }

            if (!hasAnyData) {
                _errorMessage.value = "Nessun dato trovato per questo periodo"
            }
        }
    }

    private fun loadMonthlyData(userId: String) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val monthlyStudyTime = mutableListOf<Float>()
        val monthlyBreakTime = mutableListOf<Float>()
        val monthlyTotalTime = mutableListOf<Float>()
        val monthlySessions = mutableListOf<Int>()

        var monthsProcessed = 0
        var totalStudy = 0
        var totalBreak = 0
        var totalSessions = 0

        for (monthOffset in 0..5) {
            val monthCalendar = Calendar.getInstance()
            monthCalendar.add(Calendar.MONTH, -monthOffset)
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

            val lastDayOfMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            var monthStudy = 0f
            var monthBreak = 0f
            var monthSessions = 0
            var daysInMonthProcessed = 0

            for (day in 1..lastDayOfMonth) {
                val dayCalendar = monthCalendar.clone() as Calendar
                dayCalendar.set(Calendar.DAY_OF_MONTH, day)
                val currentDate = dateFormat.format(dayCalendar.time)

                firestore.collection("users")
                    .document(userId)
                    .collection("studyData")
                    .document(currentDate)
                    .get()
                    .addOnSuccessListener { document ->
                        val studyTimeMinutes = document.getLong("activeStudyTime")?.toFloat()?.div(60) ?: 0f
                        val breakTimeMinutes = document.getLong("breakTime")?.toFloat()?.div(60) ?: 0f
                        val sessions = document.getLong("sessionsCompleted")?.toInt() ?: 0

                        monthStudy += studyTimeMinutes
                        monthBreak += breakTimeMinutes
                        monthSessions += sessions

                        daysInMonthProcessed++

                        if (daysInMonthProcessed == lastDayOfMonth) {
                            monthlyStudyTime.add(monthStudy)
                            monthlyBreakTime.add(monthBreak)
                            monthlyTotalTime.add(monthStudy + monthBreak)
                            monthlySessions.add(monthSessions)

                            if (monthOffset == 0) {
                                totalStudy = monthStudy.toInt()
                                totalBreak = monthBreak.toInt()
                                totalSessions = monthSessions
                            }

                            monthsProcessed++

                            if (monthsProcessed == 6) {
                                updateWeeklyStatisticsForMonthlyView(
                                    totalStudy = totalStudy,
                                    totalBreak = totalBreak,
                                    totalSessions = totalSessions,
                                    monthlyStudyTime = monthlyStudyTime.reversed(),
                                    monthlyBreakTime = monthlyBreakTime.reversed(),
                                    monthlyTotalTime = monthlyTotalTime.reversed(),
                                    monthlySessions = monthlySessions.reversed()
                                )
                                _isLoading.value = false
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        daysInMonthProcessed++
                        if (daysInMonthProcessed == lastDayOfMonth) {
                            monthlyStudyTime.add(monthStudy)
                            monthlyBreakTime.add(monthBreak)
                            monthlyTotalTime.add(monthStudy + monthBreak)
                            monthlySessions.add(monthSessions)

                            monthsProcessed++

                            if (monthsProcessed == 6) {
                                updateWeeklyStatisticsForMonthlyView(
                                    totalStudy = totalStudy,
                                    totalBreak = totalBreak,
                                    totalSessions = totalSessions,
                                    monthlyStudyTime = monthlyStudyTime.reversed(),
                                    monthlyBreakTime = monthlyBreakTime.reversed(),
                                    monthlyTotalTime = monthlyTotalTime.reversed(),
                                    monthlySessions = monthlySessions.reversed()
                                )
                                _isLoading.value = false

                                if (totalStudy == 0 && totalBreak == 0 && totalSessions == 0) {
                                    _errorMessage.value = "Nessun dato trovato per questo periodo"
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun updateWeeklyStatistics(
        totalStudy: Int,
        totalBreak: Int,
        totalSessions: Int,
        dailyStudyTime: List<Float>,
        dailyBreakTime: List<Float>,
        dailyTotalTime: List<Float>,
        dailySessions: List<Int>,
        weekStartDate: String,
        weekEndDate: String
    ) {
        val weeklyStudyGoal = 1260
        val weeklyBreakGoal = 420

        val studyProgress = if (weeklyStudyGoal > 0) {
            totalStudy.toFloat() / weeklyStudyGoal.toFloat()
        } else 0f

        val breakProgress = if (weeklyBreakGoal > 0) {
            totalBreak.toFloat() / weeklyBreakGoal.toFloat()
        } else 0f

        val averageStudyPerDay = if (dailyStudyTime.isNotEmpty()) {
            dailyStudyTime.average().toFloat()
        } else 0f

        val averageBreakPerDay = if (dailyBreakTime.isNotEmpty()) {
            dailyBreakTime.average().toFloat()
        } else 0f

        val dayNames = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")
        val maxStudyIndex = dailyStudyTime.withIndex().maxByOrNull { it.value }?.index ?: 0
        val mostProductiveDay = if (dailyStudyTime.getOrNull(maxStudyIndex) ?: 0f > 0) dayNames[maxStudyIndex] else "Nessuno"

        val hasData = totalStudy > 0 || totalBreak > 0 || totalSessions > 0

        _weeklyData.value = _weeklyData.value.copy(
            totalStudyTime = totalStudy,
            totalBreakTime = totalBreak,
            totalTime = totalStudy + totalBreak,
            totalSessions = totalSessions,
            dailyStudyTime = dailyStudyTime,
            dailyBreakTime = dailyBreakTime,
            dailyTotalTime = dailyTotalTime,
            dailySessions = dailySessions,
            weeklyStudyGoal = weeklyStudyGoal,
            weeklyBreakGoal = weeklyBreakGoal,
            studyGoalProgress = studyProgress.coerceIn(0f, 1f),
            breakGoalProgress = breakProgress.coerceIn(0f, 1f),
            averageStudyPerDay = averageStudyPerDay,
            averageBreakPerDay = averageBreakPerDay,
            mostProductiveDay = mostProductiveDay,
            weekStartDate = weekStartDate,
            weekEndDate = weekEndDate,
            isEmpty = !hasData
        )
    }

    private fun updateWeeklyStatisticsForWeeklyView(
        totalStudy: Int,
        totalBreak: Int,
        totalSessions: Int,
        weeklyStudyTime: List<Float>,
        weeklyBreakTime: List<Float>,
        weeklyTotalTime: List<Float>,
        weeklySessions: List<Int>
    ) {
        val currentWeekStudy = weeklyStudyTime.lastOrNull() ?: 0f
        val previousWeekStudy = weeklyStudyTime.getOrNull(weeklyStudyTime.size - 2) ?: 0f
        val trendPercentage = if (previousWeekStudy > 0) {
            ((currentWeekStudy - previousWeekStudy) / previousWeekStudy) * 100
        } else 0f

        val bestWeekIndex = weeklyStudyTime.withIndex().maxByOrNull { it.value }?.index ?: 0
        val bestWeek = "Settimana ${4 - bestWeekIndex}"

        val hasData = totalStudy > 0 || totalBreak > 0 || totalSessions > 0

        _weeklyData.value = _weeklyData.value.copy(
            totalStudyTime = totalStudy,
            totalBreakTime = totalBreak,
            totalTime = totalStudy + totalBreak,
            totalSessions = totalSessions,
            weeklyStudyTime = weeklyStudyTime,
            weeklyBreakTime = weeklyBreakTime,
            weeklyTotalTime = weeklyTotalTime,
            weeklySessions = weeklySessions,
            trendPercentage = trendPercentage,
            bestWeek = bestWeek,
            isEmpty = !hasData
        )
    }

    private fun updateWeeklyStatisticsForMonthlyView(
        totalStudy: Int,
        totalBreak: Int,
        totalSessions: Int,
        monthlyStudyTime: List<Float>,
        monthlyBreakTime: List<Float>,
        monthlyTotalTime: List<Float>,
        monthlySessions: List<Int>
    ) {
        val currentMonthStudy = monthlyStudyTime.lastOrNull() ?: 0f
        val previousMonthStudy = monthlyStudyTime.getOrNull(monthlyStudyTime.size - 2) ?: 0f
        val trendPercentage = if (previousMonthStudy > 0) {
            ((currentMonthStudy - previousMonthStudy) / previousMonthStudy) * 100
        } else 0f

        val monthNames = listOf("6 mesi fa", "5 mesi fa", "4 mesi fa", "3 mesi fa", "2 mesi fa", "Mese corrente")
        val bestMonthIndex = monthlyStudyTime.withIndex().maxByOrNull { it.value }?.index ?: 0
        val bestMonth = monthNames[bestMonthIndex]

        val hasData = totalStudy > 0 || totalBreak > 0 || totalSessions > 0

        _weeklyData.value = _weeklyData.value.copy(
            totalStudyTime = totalStudy,
            totalBreakTime = totalBreak,
            totalTime = totalStudy + totalBreak,
            totalSessions = totalSessions,
            monthlyStudyTime = monthlyStudyTime,
            monthlyBreakTime = monthlyBreakTime,
            monthlyTotalTime = monthlyTotalTime,
            monthlySessions = monthlySessions,
            trendPercentage = trendPercentage,
            bestMonth = bestMonth,
            isEmpty = !hasData
        )
    }

    fun getCurrentStreak(filter: DataFilter) {
        val userId = getCurrentUserId()
        if (userId == null) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        var streak = 0
        var checkDate = calendar.clone() as Calendar

        // Controlla fino a 60 giorni indietro
        val maxDaysToCheck = 60
        var daysChecked = 0

        fun checkNextDay() {
            if (daysChecked >= maxDaysToCheck) {
                updateStreakInStatistics(streak)
                return
            }

            val currentDate = dateFormat.format(checkDate.time)

            firestore.collection("users")
                .document(userId)
                .collection("studyData")
                .document(currentDate)
                .get()
                .addOnSuccessListener { document ->
                    val dayValue = when (filter) {
                        DataFilter.STUDY -> document.getLong("activeStudyTime")?.toFloat()?.div(60) ?: 0f
                        DataFilter.BREAK -> document.getLong("breakTime")?.toFloat()?.div(60) ?: 0f
                        DataFilter.TOTAL -> {
                            val study = document.getLong("activeStudyTime")?.toFloat()?.div(60) ?: 0f
                            val pause = document.getLong("breakTime")?.toFloat()?.div(60) ?: 0f
                            study + pause
                        }
                        DataFilter.SESSIONS -> document.getLong("sessionsCompleted")?.toFloat() ?: 0f
                    }

                    if (dayValue > 0) {
                        streak++
                        checkDate.add(Calendar.DAY_OF_MONTH, -1) // Vai al giorno precedente
                        daysChecked++
                        checkNextDay() // Continua ricorsivamente
                    } else {
                        // Streak interrotta
                        updateStreakInStatistics(streak)
                    }
                }
                .addOnFailureListener {
                    // In caso di errore, considera il giorno come 0 e interrompi la streak
                    updateStreakInStatistics(streak)
                }
        }

        checkNextDay()
    }

    private fun updateStreakInStatistics(streak: Int) {
        _weeklyData.value = _weeklyData.value.copy(
            currentStreak = streak
        )
    }
}