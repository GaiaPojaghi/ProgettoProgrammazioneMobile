package com.example.fitjourney.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fitjourney.data.UserData
import com.example.fitjourney.ui.viewModel.AuthViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val userData by viewModel.userData

    var isEditing by remember { mutableStateOf(false) }
    var editedUserData by remember(userData) { mutableStateOf(userData) }
    var showImagePicker by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var passwordForReauth by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALY) }

    LaunchedEffect(userData) {
        editedUserData = userData
    }

    fun saveImagePermanently(sourceUri: Uri): String? {
        return try {
            val fileName = "profile_image_${System.currentTimeMillis()}.jpg"
            val internalDir = File(context.filesDir, "profile_images")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            val destinationFile = File(internalDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteOldImage(imagePath: String) {
        try {
            if (imagePath.startsWith("/") && File(imagePath).exists()) {
                File(imagePath).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val permanentPath = saveImagePermanently(selectedUri)
            permanentPath?.let { path ->
                if (editedUserData.photoUrl.isNotEmpty() && editedUserData.photoUrl.startsWith("/")) {
                    deleteOldImage(editedUserData.photoUrl)
                }
                editedUserData = editedUserData.copy(photoUrl = path)
                isEditing = true
            } ?: run {
                Toast.makeText(context, "Errore nel salvare l'immagine", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { tempUri ->
                val permanentPath = saveImagePermanently(tempUri)
                permanentPath?.let { path ->
                    if (editedUserData.photoUrl.isNotEmpty() && editedUserData.photoUrl.startsWith("/")) {
                        deleteOldImage(editedUserData.photoUrl)
                    }
                    editedUserData = editedUserData.copy(photoUrl = path)
                    isEditing = true
                } ?: run {
                    Toast.makeText(context, "Errore nel salvare l'immagine", Toast.LENGTH_SHORT).show()
                }

                try {
                    File(tempUri.path ?: "").delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val imageFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            tempImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
            tempImageUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(context, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
        }
    }

    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                editedUserData = editedUserData.copy(dataNascita = dateFormatter.format(calendar.time))
                isEditing = true
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LaunchedEffect(viewModel.isLoggedIn()) {
        if (viewModel.isLoggedIn()) {
            viewModel.loadUserData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Profilo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF283593))
        )

        if (viewModel.isLoggedIn()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Immagine profilo a sinistra
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(3.dp, Color(0xFF283593), CircleShape)
                                    .clickable { showImagePicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (editedUserData.photoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = if (editedUserData.photoUrl.startsWith("/")) {
                                            File(editedUserData.photoUrl)
                                        } else {
                                            editedUserData.photoUrl
                                        },
                                        contentDescription = "Foto profilo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Aggiungi foto",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifica foto",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(
                                            Color(0xFF283593),
                                            CircleShape
                                        )
                                        .padding(4.dp),
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Dettagli utente a destra
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${userData.nome} ${userData.cognome}".takeIf { it.isNotBlank() }
                                        ?: viewModel.getCurrentUserName() ?: "Utente",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF283593)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = userData.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF607D8B)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Informazioni Personali",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF283593)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = editedUserData.nome,
                                onValueChange = {
                                    editedUserData = editedUserData.copy(nome = it)
                                    isEditing = true
                                },
                                label = { Text("Nome") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF283593),
                                    focusedLabelColor = Color(0xFF283593)
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Nome")
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editedUserData.cognome,
                                onValueChange = {
                                    editedUserData = editedUserData.copy(cognome = it)
                                    isEditing = true
                                },
                                label = { Text("Cognome") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF283593),
                                    focusedLabelColor = Color(0xFF283593)
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.PersonOutline, contentDescription = "Cognome")
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editedUserData.dataNascita,
                                onValueChange = { },
                                label = { Text("Data di nascita") },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isSaving) showDatePicker() },
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF283593),
                                    focusedLabelColor = Color(0xFF283593)
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = "Data di nascita")
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Seleziona data",
                                        modifier = Modifier.clickable { if (!isSaving) showDatePicker() }
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Sesso",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF283593),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = editedUserData.sesso == "Maschio",
                                            onClick = {
                                                if (!isSaving) {
                                                    editedUserData = editedUserData.copy(sesso = "Maschio")
                                                    isEditing = true
                                                }
                                            },
                                            enabled = !isSaving,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF283593)
                                            )
                                        )
                                        Text(
                                            text = "Maschio",
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clickable {
                                                    if (!isSaving) {
                                                        editedUserData = editedUserData.copy(sesso = "Maschio")
                                                        isEditing = true
                                                    }
                                                }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = editedUserData.sesso == "Femmina",
                                            onClick = {
                                                if (!isSaving) {
                                                    editedUserData = editedUserData.copy(sesso = "Femmina")
                                                    isEditing = true
                                                }
                                            },
                                            enabled = !isSaving,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF283593)
                                            )
                                        )
                                        Text(
                                            text = "Femmina",
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clickable {
                                                    if (!isSaving) {
                                                        editedUserData = editedUserData.copy(sesso = "Femmina")
                                                        isEditing = true
                                                    }
                                                }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editedUserData.email,
                                onValueChange = {
                                    editedUserData = editedUserData.copy(email = it)
                                    isEditing = true
                                },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF283593),
                                    focusedLabelColor = Color(0xFF283593)
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = "Email")
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editedUserData.telefono,
                                onValueChange = {
                                    editedUserData = editedUserData.copy(telefono = it)
                                    isEditing = true
                                },
                                label = { Text("Telefono") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF283593),
                                    focusedLabelColor = Color(0xFF283593)
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = "Telefono")
                                }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isSaving = true
                                val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
                                val timeoutRunnable = Runnable {
                                    if (isSaving) {
                                        isSaving = false
                                        Toast.makeText(context, "Timeout: operazione troppo lenta. Riprova.", Toast.LENGTH_LONG).show()
                                    }
                                }
                                timeoutHandler.postDelayed(timeoutRunnable, 30000)

                                viewModel.updateUserData(
                                    editedUserData,
                                    onSuccess = {
                                        timeoutHandler.removeCallbacks(timeoutRunnable)
                                        isSaving = false
                                        isEditing = false
                                        Toast.makeText(context, "Modifiche salvate con successo!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { exception ->
                                        timeoutHandler.removeCallbacks(timeoutRunnable)
                                        isSaving = false
                                        Toast.makeText(context, "Errore nel salvare le modifiche: ${exception.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isEditing && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Salva",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(if (isSaving) "Salvataggio..." else "Salva", color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                editedUserData = userData
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isEditing && !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF283593)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Annulla",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Annulla")
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "Logout effettuato", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF283593)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Logout", color = Color.White)
                    }
                }

                item {
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving && !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminazione...", color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina Account",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Elimina Account", color = Color.White)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF283593)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Effettua l'accesso",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF283593)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { navController.navigate("login") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF283593)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Accedi", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { navController.navigate("register") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF283593)
                            )
                        ) {
                            Text("Registrati")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Elimina Account",
                    color = Color(0xFFE53935)
                )
            },
            text = {
                Text("Sei sicuro di voler eliminare definitivamente il tuo account? Questa azione non può essere annullata e tutti i tuoi dati verranno persi.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isDeleting = true

                        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
                        val timeoutRunnable = Runnable {
                            if (isDeleting) {
                                isDeleting = false
                                Toast.makeText(context, "Timeout: operazione troppo lenta. Riprova.", Toast.LENGTH_LONG).show()
                            }
                        }
                        timeoutHandler.postDelayed(timeoutRunnable, 30000)

                        viewModel.deleteAccount(
                            onSuccess = {
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isDeleting = false
                                Toast.makeText(context, "Account eliminato con successo. Arrivederci!", Toast.LENGTH_LONG).show()
                                navController.navigate("home") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            },
                            onFailure = { exception ->
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isDeleting = false
                                Toast.makeText(context, "Errore nell'eliminazione account: ${exception.message}", Toast.LENGTH_LONG).show()
                            },
                            onNeedReauth = {
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isDeleting = false
                                showReauthDialog = true
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                passwordForReauth = ""
                passwordError = ""
            },
            title = { Text("Conferma Password") },
            text = {
                Column {
                    Text("Per eliminare l'account, conferma la tua password:")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordForReauth,
                        onValueChange = {
                            passwordForReauth = it
                            passwordError = ""
                        },
                        label = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = passwordError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF283593),
                            focusedLabelColor = Color(0xFF283593)
                        )
                    )
                    if (passwordError.isNotEmpty()) {
                        Text(
                            text = passwordError,
                            color = Color(0xFFE53935),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passwordForReauth.isBlank()) {
                            passwordError = "La password è richiesta"
                            return@TextButton
                        }

                        showReauthDialog = false
                        isDeleting = true

                        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
                        val timeoutRunnable = Runnable {
                            if (isDeleting) {
                                isDeleting = false
                                Toast.makeText(context, "Timeout: operazione troppo lenta. Riprova.", Toast.LENGTH_LONG).show()
                            }
                        }
                        timeoutHandler.postDelayed(timeoutRunnable, 30000)

                        viewModel.deleteAccount(
                            password = passwordForReauth,
                            onSuccess = {
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isDeleting = false
                                passwordForReauth = ""
                                Toast.makeText(context, "Account eliminato con successo. Arrivederci!", Toast.LENGTH_LONG).show()
                                navController.navigate("home") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            },
                            onFailure = { exception ->
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isDeleting = false
                                passwordForReauth = ""

                                val errorMessage = when {
                                    exception.message?.contains("password is invalid") == true ->
                                        "Password non corretta"
                                    exception.message?.contains("network") == true ->
                                        "Errore di connessione. Riprova."
                                    else -> "Errore nell'eliminazione account: ${exception.message}"
                                }

                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReauthDialog = false
                        passwordForReauth = ""
                        passwordError = ""
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("Seleziona foto profilo") },
            text = { Text("Scegli come aggiungere la tua foto profilo") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImagePicker = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            val imageFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                            tempImageUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                imageFile
                            )
                            tempImageUri?.let { cameraLauncher.launch(it) }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Text("Fotocamera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImagePicker = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Text("Galleria")
                }
            }
        )
    }
}