package com.example.fitjourney.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fitjourney.R
import com.example.fitjourney.ui.viewModel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterForm(
    navController: NavController,
    viewModel: AuthViewModel,
    googleSignInClient: GoogleSignInClient
) {
    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }
    var dataNascita by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var sesso by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALY) }

    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                dataNascita = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.signInWithGoogle(
                        token,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Registrazione con Google completata",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate("profile")
                        },
                        onFailure = { exception ->
                            Toast.makeText(
                                context,
                                "Errore registrazione Google: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            } catch (e: ApiException) {
                Toast.makeText(
                    context,
                    "Errore registrazione Google: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Validazione password
    val passwordsMatch = password == confirmPassword
    val passwordError = when {
        password.isNotEmpty() && password.length < 6 -> "La password deve essere di almeno 6 caratteri"
        confirmPassword.isNotEmpty() && !passwordsMatch -> "Le password non coincidono"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Registrazione", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF283593)),
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Torna indietro",
                        tint = Color.White
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nome.isBlank(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = cognome,
                        onValueChange = { cognome = it },
                        label = { Text("Cognome *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = cognome.isBlank(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dataNascita,
                        onValueChange = { },
                        label = { Text("Data di nascita *") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { showDatePicker() },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Seleziona data",
                                modifier = Modifier.clickable(enabled = !isLoading) { showDatePicker() },
                                tint = Color(0xFF283593)
                            )
                        },
                        isError = dataNascita.isBlank(),
                        enabled = !isLoading
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sesso *",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sesso.isBlank()) Color(0xFFE53935) else Color(0xFF757575),
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
                                    selected = sesso == "Maschio",
                                    onClick = { if (!isLoading) sesso = "Maschio" },
                                    enabled = !isLoading,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF283593)
                                    )
                                )
                                Text(
                                    text = "Maschio",
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable(enabled = !isLoading) { sesso = "Maschio" }
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sesso == "Femmina",
                                    onClick = { if (!isLoading) sesso = "Femmina" },
                                    enabled = !isLoading,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF283593)
                                    )
                                )
                                Text(
                                    text = "Femmina",
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable(enabled = !isLoading) { sesso = "Femmina" }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = email.isBlank(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Telefono") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password *") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = password.isNotEmpty() && password.length < 6,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Nascondi password" else "Mostra password",
                                    tint = Color(0xFF283593)
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Conferma Password *") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Nascondi password" else "Mostra password",
                                    tint = Color(0xFF283593)
                                )
                            }
                        }
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError,
                            color = Color(0xFFE53935),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Text(
                        text = "* Campi obbligatori",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val isValid = nome.isNotBlank() &&
                                    cognome.isNotBlank() &&
                                    dataNascita.isNotBlank() &&
                                    sesso.isNotBlank() &&
                                    email.isNotBlank() &&
                                    password.length >= 6 &&
                                    passwordsMatch

                            if (isValid) {
                                viewModel.register(
                                    nome,
                                    cognome,
                                    dataNascita,
                                    email,
                                    telefono,
                                    password,
                                    sesso,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Registrazione completata",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.navigate("profile")
                                    },
                                    onFailure = {
                                        Toast.makeText(
                                            context,
                                            "Errore: ${it.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            } else {
                                val errorMessage = when {
                                    nome.isBlank() || cognome.isBlank() || dataNascita.isBlank() ||
                                            sesso.isBlank() || email.isBlank() -> "Completa tutti i campi obbligatori!"

                                    password.length < 6 -> "La password deve essere di almeno 6 caratteri"
                                    !passwordsMatch -> "Le password non coincidono"
                                    else -> "Controlla i dati inseriti"
                                }
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF283593)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrazione...", color = Color.White)
                        } else {
                            Text("Crea account", color = Color.White)
                        }
                    }

                    // Divider con "oppure"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFBDBDBD)
                        )
                        Text(
                            text = "oppure",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF757575)
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = Color(0xFFBDBDBD)
                        )
                    }

                    // Pulsante Google
                    OutlinedButton(
                        onClick = {
                            googleAuthLauncher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrati con Google")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Hai già un account? ",
                            color = Color(0xFF757575)
                        )
                        Text(
                            "Accedi",
                            modifier = Modifier.clickable {
                                if (!isLoading) {
                                    navController.navigate("login")
                                }
                            },
                            color = Color(0xFF283593),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
