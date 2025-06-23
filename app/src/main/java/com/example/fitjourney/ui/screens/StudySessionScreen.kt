package com.example.fitjourney.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.round
import com.example.fitjourney.ui.viewModel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(navController: NavController, viewModel: StudyViewModel = viewModel()) {
    val totalSeconds = 25 * 60
    var remainingTime by remember { mutableStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    var elapsedTime by remember { mutableStateOf(0) }
    var pauseElapsedTime by remember { mutableStateOf(0) }

    // Studio timer
    LaunchedEffect(isRunning) {
        while (isRunning && remainingTime > 0) {
            delay(1000)
            remainingTime--
            elapsedTime++
        }

        if (remainingTime == 0 && elapsedTime > 0) {
            val studyMin = round(elapsedTime / 60.0).toInt()
            val breakMin = round(pauseElapsedTime / 60.0).toInt()
            viewModel.addLiveStudyTime(studyMin)
            if (pauseElapsedTime > 0) {
                viewModel.addLiveBreakTime(breakMin)
            }
            viewModel.incrementSessionCount()
        }
    }

    // Pausa timer
    LaunchedEffect(isRunning) {
        while (!isRunning) {
            delay(1000)
            pauseElapsedTime++
        }
        // Quando riprende lo studio, salva i minuti di pausa accumulati
        if (pauseElapsedTime > 0) {
            val breakMin = round(pauseElapsedTime / 60.0).toInt()
            viewModel.addLiveBreakTime(breakMin)
            pauseElapsedTime = 0
        }
    }

    val progress = 1f - (remainingTime.toFloat() / totalSeconds)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "Progress"
    )

    val minutes = remainingTime / 60
    val seconds = remainingTime % 60
    val pauseMin = pauseElapsedTime / 60
    val pauseSec = pauseElapsedTime % 60
    val studyMinutes = round(elapsedTime / 60.0).toInt()
    val breakMinutes = round(pauseElapsedTime / 60.0).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sessione di Studio",
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        isRunning = false
                        if (elapsedTime > 0) viewModel.addLiveStudyTime(studyMinutes)
                        if (pauseElapsedTime > 0) viewModel.addLiveBreakTime(breakMinutes)
                        viewModel.incrementSessionCount()
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White // ⬅️ Freccia bianca
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = Color.White, // (opzionale) fallback
                    titleContentColor = Color.White // (opzionale) fallback
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Timer studio
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val radius = size.minDimension / 2 - stroke / 2
                    drawCircle(
                        color = Color.LightGray,
                        radius = radius,
                        center = center,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF4CAF50),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Se in pausa, mostra il timer pausa
            if (!isRunning) {
                Text(
                    text = "In pausa: ${String.format("%02d:%02d", pauseMin, pauseSec)}",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Pausa" else "Riprendi", color = MaterialTheme.colorScheme.onPrimary)
                }

                Button(
                    onClick = {
                        isRunning = false
                        // ✅ CORREZIONE: Usa studyMinutes e breakMinutes invece di elapsedTime e pauseElapsedTime
                        if (elapsedTime > 0) viewModel.addLiveStudyTime(studyMinutes)
                        if (pauseElapsedTime > 0) viewModel.addLiveBreakTime(breakMinutes)
                        viewModel.incrementSessionCount()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Termina", color = Color.White)
                }
            }
        }
    }
}