package com.example.fitjourney.ui.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.fitjourney.data.StudyData
import com.example.fitjourney.util.viewModel.BaseViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class StudyViewModel : BaseViewModel() {

    private val _studyData = mutableStateOf(StudyData())
    private var previousStudyData = StudyData()
    val studyData: State<StudyData> = _studyData

    init {
        loadStudyData()
    }

    private fun updateStudyData(newData: StudyData) {
        previousStudyData = _studyData.value.copy() // Copia profonda dello stato corrente
        _studyData.value = newData
    }

    fun simulateStudySession() {
        val newData = _studyData.value.copy(
            activeStudyTime = _studyData.value.activeStudyTime + 25,
            sessionsCompleted = _studyData.value.sessionsCompleted + 1
        )
        updateStudyData(newData)
        checkForNewMedals()
        saveStudyData()
    }

    fun simulateBreak() {
        val newData = _studyData.value.copy(
            breakTime = _studyData.value.breakTime + 5
        )
        updateStudyData(newData)
        checkForNewMedals()
        saveStudyData()
    }

    fun simulateProgress() {
        val newData = _studyData.value.copy(
            activeStudyTime = _studyData.value.activeStudyTime + 25,
            breakTime = _studyData.value.breakTime + 8,
            sessionsCompleted = _studyData.value.sessionsCompleted + 1
        )
        updateStudyData(newData)
        checkForNewMedals()
        saveStudyData()
    }

    fun updateStudyGoal(newGoalMinutes: Int) {
        _studyData.value = _studyData.value.copy(
            studyGoalMinutes = newGoalMinutes.coerceIn(15, 720)
        )
        saveStudyData()
    }

    fun updateBreakGoal(newGoalMinutes: Int) {
        _studyData.value = _studyData.value.copy(
            breakGoalMinutes = newGoalMinutes.coerceIn(5, 240)
        )
        saveStudyData()
    }

    fun updateTotalGoal(newGoalMinutes: Int) {
        _studyData.value = _studyData.value.copy(
            totalGoalMinutes = newGoalMinutes.coerceIn(60, 960)
        )
        saveStudyData()
    }

    fun addLiveStudyTime(minutes: Int) {
        if (minutes <= 0) return
        val newData = _studyData.value.copy(
            activeStudyTime = _studyData.value.activeStudyTime + minutes
        )
        updateStudyData(newData)
        checkForNewMedals()
        saveStudyData()
    }

    fun incrementSessionCount() {
        val newData = _studyData.value.copy(
            sessionsCompleted = _studyData.value.sessionsCompleted + 1
        )
        updateStudyData(newData)
        checkForNewMedals()
        saveStudyData()
    }

    fun addLiveBreakTime(minutes: Int) {
        if (minutes <= 0) return
        val newData = _studyData.value.copy(
            breakTime = _studyData.value.breakTime + minutes
        )
        updateStudyData(newData)
        checkForNewMedals()
        saveStudyData()
    }

    // Funzione per controllare se sono state sbloccate nuove medaglie
    private fun checkForNewMedals() {
        val oldUnlocked = calculateUnlockedMedals(previousStudyData)
        val newUnlocked = calculateUnlockedMedals(_studyData.value)

        // Debug: stampa per verificare la logica
        println("DEBUG: Medaglie precedenti: $oldUnlocked")
        println("DEBUG: Medaglie attuali: $newUnlocked")
        println("DEBUG: Nuove medaglie: ${newUnlocked - oldUnlocked}")

        if (newUnlocked.size > oldUnlocked.size) {
            _studyData.value = _studyData.value.copy(newMedalUnlocked = true)
            println("DEBUG: Flag newMedalUnlocked impostato a true")
        }
    }

    fun getNewlyUnlockedMedals(): Set<String> {
        val oldUnlocked = calculateUnlockedMedals(previousStudyData)
        val newUnlocked = calculateUnlockedMedals(_studyData.value)
        val newly = newUnlocked - oldUnlocked

        println("DEBUG getNewlyUnlockedMedals: $newly")
        return newly
    }

    private fun saveStudyData() {
        val userId = getCurrentUserId() ?: return
        val date = getTodayDateString()
        val now = getCurrentTimestamp()
        val data = _studyData.value.copy(lastUpdated = now)

        val studyMap = hashMapOf(
            "activeStudyTime" to data.activeStudyTime,
            "breakTime" to data.breakTime,
            "totalTime" to data.calculatedTotalTime,
            "sessionsCompleted" to data.sessionsCompleted,
            "studyGoalMinutes" to data.studyGoalMinutes,
            "breakGoalMinutes" to data.breakGoalMinutes,
            "totalGoalMinutes" to data.totalGoalMinutes,
            "lastUpdated" to data.lastUpdated,
            "isTemporary" to data.isTemporary,
            "newMedalUnlocked" to data.newMedalUnlocked
        )

        firestore.collection("users")
            .document(userId)
            .collection("studyData")
            .document(date)
            .set(studyMap)
            .addOnSuccessListener {
                println("Dati studio salvati per $date")
            }
            .addOnFailureListener { e ->
                println("Errore nel salvataggio dati studio: ${e.message}")
            }
    }

    fun loadStudyData() {
        val userId = getCurrentUserId() ?: return
        val date = getTodayDateString()

        firestore.collection("users")
            .document(userId)
            .collection("studyData")
            .document(date)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val loadedData = StudyData(
                        activeStudyTime = document.getLong("activeStudyTime")?.toInt() ?: 0,
                        breakTime = document.getLong("breakTime")?.toInt() ?: 0,
                        totalTime = document.getLong("totalTime")?.toInt() ?: 0,
                        sessionsCompleted = document.getLong("sessionsCompleted")?.toInt() ?: 0,
                        studyGoalMinutes = document.getLong("studyGoalMinutes")?.toInt() ?: 180,
                        breakGoalMinutes = document.getLong("breakGoalMinutes")?.toInt() ?: 60,
                        totalGoalMinutes = document.getLong("totalGoalMinutes")?.toInt() ?: 480,
                        lastUpdated = document.getString("lastUpdated") ?: "",
                        isTemporary = document.getBoolean("isTemporary") ?: false,
                        newMedalUnlocked = document.getBoolean("newMedalUnlocked") ?: false
                    )

                    // IMPORTANTE: Imposta anche lo stato precedente uguale a quello caricato
                    // per evitare falsi positivi al primo caricamento
                    previousStudyData = loadedData.copy()
                    _studyData.value = loadedData

                    println("Dati studio caricati per $date")
                } else {
                    // Se non esistono dati per oggi, inizializza con valori predefiniti
                    val defaultData = StudyData()
                    previousStudyData = defaultData.copy()
                    _studyData.value = defaultData
                }
            }
            .addOnFailureListener { e ->
                println("Errore caricamento dati studio: ${e.message}")
            }
    }

    fun resetNewMedalStatus() {
        println("DEBUG: Reset newMedalUnlocked flag")
        _studyData.value = _studyData.value.copy(newMedalUnlocked = false)
        saveStudyData() // Salva lo stato aggiornato
    }

    // Funzione per calcolare quali medaglie sono sbloccate in base ai dati forniti
    private fun calculateUnlockedMedals(data: StudyData): Set<String> {
        val unlocked = mutableSetOf<String>()

        // Prima sessione
        if (data.sessionsCompleted >= 1) unlocked += "first_study"

        // Tempo di studio
        if (data.activeStudyTime >= 30) unlocked += "study_30min"
        if (data.activeStudyTime >= 120) unlocked += "study_2h"
        if (data.activeStudyTime >= 300) unlocked += "study_5h"
        if (data.activeStudyTime >= 600) unlocked += "study_10h"

        // Sessioni completate
        if (data.sessionsCompleted >= 5) unlocked += "sessions_5"
        if (data.sessionsCompleted >= 10) unlocked += "sessions_10"
        if (data.sessionsCompleted >= 25) unlocked += "sessions_25"
        if (data.sessionsCompleted >= 50) unlocked += "sessions_50"

        // Medaglie speciali
        if (data.sessionsCompleted >= 1) unlocked += "focus_master"
        if (data.activeStudyTime >= 120 && data.breakTime >= 30) unlocked += "balanced_study"

        return unlocked
    }

    // Funzione helper per ottenere il conteggio delle medaglie sbloccate
    fun getUnlockedMedalsCount(): Int {
        return calculateUnlockedMedals(_studyData.value).size
    }

    // Funzione helper per controllare se una specifica medaglia è sbloccata
    fun isMedalUnlocked(medalId: String): Boolean {
        return calculateUnlockedMedals(_studyData.value).contains(medalId)
    }
}