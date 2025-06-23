package com.example.fitjourney.data

/**
 * Data class che rappresenta i dati di una sessione di studio giornaliera.
 * Contiene tempo di studio, pause, sessioni completate e obiettivi.
 * Include anche flag per dati temporanei e metodi di calcolo utili.
 */
data class StudyData(
    val activeStudyTime: Int = 0,      // Tempo di studio attivo in minuti
    val breakTime: Int = 0,            // Tempo di pausa in minuti
    val totalTime: Int = 0,            // Tempo totale (studio + pausa), se vuoi usarlo esplicitamente
    val sessionsCompleted: Int = 0,    // Numero di sessioni di studio completate
    val studyGoalMinutes: Int = 180,   // Obiettivo studio in minuti (default 3 ore)
    val breakGoalMinutes: Int = 60,    // Obiettivo pause in minuti (default 1 ora)
    val totalGoalMinutes: Int = 480,   // Obiettivo tempo totale in minuti (default 8 ore)
    val lastUpdated: String = "",      // Timestamp ultimo aggiornamento (stringa ISO o simile)
    val isTemporary: Boolean = false,   // Flag che indica se i dati sono temporanei (es. utente non loggato)
    val newMedalUnlocked: Boolean = false // Flag che indica se ci sono state nuove medaglie sbloccate
) {
    /**
     * Tempo totale calcolato come somma di studio attivo + pausa.
     * Può essere usato se vuoi un valore sempre aggiornato.
     */
    val calculatedTotalTime: Int
        get() = activeStudyTime + breakTime

    /**
     * Indica se le pause sono eccessive rispetto al tempo di studio.
     * (Esempio: se pause > 50% del tempo di studio)
     */
    val isBreakExcessive: Boolean
        get() = breakTime > 0 && activeStudyTime > 0 &&
                (breakTime.toFloat() / activeStudyTime.toFloat()) > 0.5f

    val studyProgress: Float
        get() = if (studyGoalMinutes > 0) (activeStudyTime.toFloat() / studyGoalMinutes).coerceAtMost(1f) else 0f

    val breakProgress: Float
        get() = if (breakGoalMinutes > 0) (breakTime.toFloat() / breakGoalMinutes).coerceAtMost(1f) else 0f

    val totalProgress: Float
        get() = if (totalGoalMinutes > 0) (calculatedTotalTime.toFloat() / totalGoalMinutes).coerceAtMost(1f) else 0f
}
