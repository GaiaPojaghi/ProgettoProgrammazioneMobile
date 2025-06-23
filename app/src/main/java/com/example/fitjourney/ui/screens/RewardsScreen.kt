package com.example.fitjourney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitjourney.ui.viewModel.StudyViewModel
import kotlinx.coroutines.delay

enum class AchievementCategory(val displayName: String) {
    STUDY_TIME("Tempo di studio"),
    SESSIONS("Sessioni"),
    SPECIAL("Medaglie speciali")
}

enum class AchievementDifficulty(val displayName: String, val color: Color) {
    EASY("Facile", Color(0xFF2E7D32)),       // Verde scuro
    MEDIUM("Medio", Color(0xFFF57C00)),     // Arancione scuro
    HARD("Difficile", Color(0xFFD84315)),    // Rosso scuro/arancione intenso
    EXTREME("Estremo", Color(0xFF6A1B9A))   // Viola scuro
}

enum class FilterType { ALL, UNLOCKED, LOCKED }

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val detailedDescription: String,
    val tips: String,
    val icon: ImageVector,
    val requirement: Int,
    val color: Color,
    val category: AchievementCategory,
    val difficulty: AchievementDifficulty
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(viewModel: StudyViewModel = viewModel()) {
    val studyData by viewModel.studyData
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    var showAchievementDialog by remember { mutableStateOf<Achievement?>(null) }
    var newUnlockedAchievement by remember { mutableStateOf<Achievement?>(null) }

    val achievements = remember {
        listOf(
            // Tempo di Studio
            Achievement("first_study", "Prima Sessione", "Completare 1 sessione di studio",
                "Avvia il timer e completa la tua prima sessione. Ottimo inizio!",
                "Parti con sessioni brevi (15-25 min).",
                Icons.Default.Star, 1, Color(0xFFFFD700), AchievementCategory.STUDY_TIME, AchievementDifficulty.EASY),
            Achievement("study_30min", "Studiatore Principiante", "Studiare per 30 minuti in totale",
                "Accumula 30 minuti di studio (anche su più sessioni).",
                "Crea una routine giornaliera.",
                Icons.Default.MenuBook, 30, Color(0xFF4CAF50), AchievementCategory.STUDY_TIME, AchievementDifficulty.EASY),
            Achievement("study_2h", "Studente Dedicato", "Studiare per 2 ore (120 minuti) in totale",
                "Accumula 120 minuti di studio.",
                "Usa la tecnica del pomodoro (25/5).",
                Icons.Default.School, 120, Color(0xFF2196F3), AchievementCategory.STUDY_TIME, AchievementDifficulty.MEDIUM),
            Achievement("study_5h", "Maratoneta dello Studio", "Studiare per 5 ore (300 minuti) in totale",
                "Accumula 300 minuti di studio.",
                "Fai pause regolari e idratati.",
                Icons.Default.EmojiEvents, 300, Color(0xFF9C27B0), AchievementCategory.STUDY_TIME, AchievementDifficulty.HARD),
            Achievement("study_10h", "Maestro della Concentrazione", "Studiare per 10 ore (600 minuti) in totale",
                "Accumula 600 minuti di studio - la medaglia più difficile per il tempo!",
                "Mantieni un equilibrio sano tra studio, sonno e attività fisica.",
                Icons.Default.Psychology, 600, Color(0xFFFF5722), AchievementCategory.STUDY_TIME, AchievementDifficulty.EXTREME),

            // Sessioni Completate
            Achievement("sessions_5", "Costanza", "Completare 5 sessioni di studio",
                "Porta a termine 5 sessioni (anche brevi).",
                "Sessioni regolari sono più efficaci di rare e lunghe.",
                Icons.Default.CheckCircle, 5, Color(0xFF4CAF50), AchievementCategory.SESSIONS, AchievementDifficulty.EASY),
            Achievement("sessions_10", "Determinazione", "Completare 10 sessioni di studio",
                "Porta a termine 10 sessioni totali.",
                "Tieni traccia dei progressi per motivarti.",
                Icons.Default.Timeline, 10, Color(0xFF2196F3), AchievementCategory.SESSIONS, AchievementDifficulty.MEDIUM),
            Achievement("sessions_25", "Perseveranza", "Completare 25 sessioni di studio",
                "Porta a termine 25 sessioni - richiede costanza nel tempo.",
                "Varia gli argomenti di studio per mantenere alta la motivazione.",
                Icons.Default.TrendingUp, 25, Color(0xFF9C27B0), AchievementCategory.SESSIONS, AchievementDifficulty.HARD),
            Achievement("sessions_50", "Campione di Disciplina", "Completare 50 sessioni di studio",
                "La medaglia più difficile per numero di sessioni!",
                "Celebra questo traguardo eccezionale!",
                Icons.Default.EmojiEvents, 50, Color(0xFFFF9800), AchievementCategory.SESSIONS, AchievementDifficulty.EXTREME),

            // Medaglie Speciali
            Achievement("focus_master", "Maestro del Focus", "Completare almeno 1 sessione",
                "Completa una sessione di studio con focus totale.",
                "Elimina tutte le distrazioni durante la sessione.",
                Icons.Default.Visibility, 1, Color(0xFF3F51B5), AchievementCategory.SPECIAL, AchievementDifficulty.EASY),
            Achievement("balanced_study", "Equilibrio Perfetto", "Studiare per almeno 2 ore con pause equilibrate",
                "Accumula almeno 120 minuti di studio e 30 minuti di pause totali.",
                "Bilancia studio e riposo per massima efficacia.",
                Icons.Default.Balance, 1, Color(0xFFE91E63), AchievementCategory.SPECIAL, AchievementDifficulty.MEDIUM)
        )
    }

    fun isAchievementUnlocked(a: Achievement): Boolean = when (a.category) {
        AchievementCategory.STUDY_TIME -> studyData.activeStudyTime >= a.requirement
        AchievementCategory.SESSIONS -> studyData.sessionsCompleted >= a.requirement
        AchievementCategory.SPECIAL -> when (a.id) {
            "balanced_study" -> studyData.activeStudyTime >= 120 && studyData.breakTime >= 30
            "focus_master" -> studyData.sessionsCompleted >= 1
            else -> false
        }
        else -> false
    }

    // Carica i dati dello studio
    LaunchedEffect(Unit) {
        viewModel.loadStudyData()
    }

    // UNICO LaunchedEffect per gestire le nuove medaglie
    LaunchedEffect(studyData.newMedalUnlocked) {
        if (studyData.newMedalUnlocked) {
            // Ottieni gli ID delle medaglie appena sbloccate
            val newlyUnlockedIds = viewModel.getNewlyUnlockedMedals()

            if (newlyUnlockedIds.isNotEmpty()) {
                // Prendi la prima medaglia appena sbloccata (o puoi implementare una logica per mostrarle tutte)
                val newAchievement = achievements.firstOrNull { it.id in newlyUnlockedIds }

                if (newAchievement != null) {
                    delay(500) // Piccola attesa per l'animazione
                    newUnlockedAchievement = newAchievement
                }
            }

            // Reset del flag DOPO aver impostato la medaglia da mostrare
            viewModel.resetNewMedalStatus()
        }
    }

    fun getProgress(a: Achievement): Float {
        val value = when (a.category) {
            AchievementCategory.STUDY_TIME -> studyData.activeStudyTime
            AchievementCategory.SESSIONS -> studyData.sessionsCompleted
            AchievementCategory.SPECIAL -> if (isAchievementUnlocked(a)) a.requirement else 0
            else -> 0
        }
        return (value.toFloat() / a.requirement).coerceAtMost(1f)
    }

    // Filtra medaglie in base a filtri e categoria
    val filteredAchievements = achievements.filter {
        val unlocked = isAchievementUnlocked(it)
        val matchesFilter = when (selectedFilter) {
            FilterType.ALL -> true
            FilterType.UNLOCKED -> unlocked
            FilterType.LOCKED -> !unlocked
        }
        val matchesCategory = selectedCategory?.let { cat -> it.category == cat } ?: true
        matchesFilter && matchesCategory
    }

    // Raggruppa per categoria (solo quelle filtrate)
    val groupedAchievements = filteredAchievements.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Medaglie e obiettivi",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF283593),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Statistiche
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("I tuoi progressi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            @Composable
                            fun Stat(title: String, value: String) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(title, fontSize = 12.sp)
                                }
                            }
                            Stat("Minuti studiati", "${studyData.activeStudyTime}")
                            Stat("Sessioni", "${studyData.sessionsCompleted}")
                            Stat("Medaglie", "${achievements.count { isAchievementUnlocked(it) }}/${achievements.size}")
                        }
                    }
                }
            }

            // Filtri stato
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterType.values()) { filter ->
                        FilterChip(
                            onClick = { selectedFilter = filter },
                            selected = selectedFilter == filter,
                            label = {
                                Text(
                                    when (filter) {
                                        FilterType.ALL -> "Tutte"
                                        FilterType.UNLOCKED -> "Ottenute"
                                        FilterType.LOCKED -> "Da ottenere"
                                    }
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Filtri categoria
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            onClick = { selectedCategory = null },
                            selected = selectedCategory == null,
                            label = { Text("Tutte le categorie") }
                        )
                    }
                    items(AchievementCategory.values()) { category ->
                        FilterChip(
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            selected = selectedCategory == category,
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }

            // Se non è selezionata nessuna categoria (Tutte le categorie),
            // mostra i titoli di categoria prima del gruppo corrispondente
            if (selectedCategory == null) {
                groupedAchievements.forEach { (category, achievementsInCategory) ->
                    item {
                        Text(
                            category.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(achievementsInCategory) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            isUnlocked = isAchievementUnlocked(achievement),
                            progress = getProgress(achievement),
                            onClick = { showAchievementDialog = achievement }
                        )
                    }
                }
            } else {
                // Se è selezionata una categoria, mostra solo le medaglie filtrate senza titoli
                items(filteredAchievements) { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        isUnlocked = isAchievementUnlocked(achievement),
                        progress = getProgress(achievement),
                        onClick = { showAchievementDialog = achievement }
                    )
                }
            }

            if (filteredAchievements.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Nessuna medaglia trovata",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Prova a cambiare i filtri",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Dialog per i dettagli della medaglia (quando clicchi su una medaglia)
        showAchievementDialog?.let { achievement ->
            AchievementDetailDialog(
                achievement = achievement,
                isUnlocked = isAchievementUnlocked(achievement),
                progress = getProgress(achievement),
                currentValue = when (achievement.category) {
                    AchievementCategory.STUDY_TIME -> studyData.activeStudyTime
                    AchievementCategory.SESSIONS -> studyData.sessionsCompleted
                    AchievementCategory.SPECIAL -> if (isAchievementUnlocked(achievement)) achievement.requirement else 0
                    else -> 0
                },
                onDismiss = { showAchievementDialog = null }
            )
        }

        // Popup celebrativo per nuove medaglie
        newUnlockedAchievement?.let { achievement ->
            ConfettiCelebration(
                achievement = achievement,
                onDismiss = { newUnlockedAchievement = null }
            )
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement,
    isUnlocked: Boolean,
    progress: Float,
    onClick: () -> Unit
) {
    val alpha = if (isUnlocked) 1f else 0.4f
    val bgColor = if (isUnlocked) achievement.color.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
    val iconColor = if (isUnlocked) achievement.color else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(achievement.icon, contentDescription = null, tint = iconColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        achievement.title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Card(
                        colors = CardDefaults.cardColors(achievement.difficulty.color.copy(alpha = 0.2f))
                    ) {
                        Text(
                            achievement.difficulty.displayName,
                            fontSize = 10.sp,
                            color = achievement.difficulty.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    achievement.description,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (!isUnlocked && progress > 0) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = achievement.color
                    )
                    val currentValue = (progress * achievement.requirement).toInt()
                    Text(
                        "$currentValue / ${achievement.requirement}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (isUnlocked) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sbloccato",
                    tint = achievement.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AchievementDetailDialog(
    achievement: Achievement,
    isUnlocked: Boolean,
    progress: Float,
    currentValue: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        achievement.icon,
                        contentDescription = null,
                        tint = achievement.color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            achievement.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            achievement.difficulty.displayName,
                            fontSize = 12.sp,
                            color = achievement.difficulty.color
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(achievement.detailedDescription)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Suggerimento: ${achievement.tips}",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                if (!isUnlocked) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = achievement.color
                    )
                    Text(
                        "$currentValue / ${achievement.requirement}",
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
}

@Composable
fun ConfettiCelebration(achievement: Achievement, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉 Complimenti! 🎉", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Icon(achievement.icon, contentDescription = null, tint = achievement.color, modifier = Modifier.size(48.dp))
                Text(achievement.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(achievement.description, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text("Evviva!")
                }
            }
        }
    }
}