package com.example.fitjourney.ui.viewModel

import androidx.compose.runtime.mutableStateOf
import com.example.fitjourney.data.UserData
import com.example.fitjourney.util.viewModel.BaseViewModel
import android.util.Log
import java.io.File
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : BaseViewModel() {

    val userData = mutableStateOf(UserData())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun isLoggedIn(): Boolean = isUserLoggedIn()

    fun getCurrentUserName(): String? = getCurrentUserEmail()

    fun register(
        nome: String,
        cognome: String,
        dataNascita: String,
        email: String,
        telefono: String,
        password: String,
        sesso: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    val userDataMap = hashMapOf(
                        "nome" to nome,
                        "cognome" to cognome,
                        "dataNascita" to dataNascita,
                        "email" to email,
                        "telefono" to telefono,
                        "sesso" to sesso,
                        "photoUrl" to ""
                    )

                    firestore.collection("users").document(userId)
                        .set(userDataMap)
                        .addOnSuccessListener {
                            loadUserData()
                            _isLoading.value = false
                            onSuccess()
                        }
                        .addOnFailureListener {
                            _isLoading.value = false
                            onFailure(it)
                        }
                } else {
                    _isLoading.value = false
                    onSuccess()
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                onFailure(it)
            }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                loadUserData()
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener {
                _isLoading.value = false
                onFailure(it)
            }
    }

    fun loadUserData() {
        val userId = getCurrentUserId()
        if (userId != null) {
            Log.d("AuthViewModel", "Loading user data for userId: $userId")
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d("AuthViewModel", "Document exists: ${document.data}")
                        val newUserData = UserData(
                            nome = document.getString("nome") ?: "",
                            cognome = document.getString("cognome") ?: "",
                            dataNascita = document.getString("dataNascita") ?: "",
                            email = document.getString("email") ?: "",
                            telefono = document.getString("telefono") ?: "",
                            sesso = document.getString("sesso") ?: "",
                            photoUrl = document.getString("photoUrl") ?: ""
                        )
                        Log.d("AuthViewModel", "New user data: $newUserData")
                        userData.value = newUserData
                    } else {
                        Log.d("AuthViewModel", "Document does not exist")
                        // Se il documento non esiste per un utente Google, crealo con i dati di base
                        val user = auth.currentUser
                        if (user != null && user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                            createGoogleUserProfile(user.email ?: "", user.displayName ?: "")
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("AuthViewModel", "Error loading user data", it)
                }
        } else {
            Log.e("AuthViewModel", "User ID is null")
        }
    }

    private fun createGoogleUserProfile(email: String, displayName: String) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val nameParts = displayName.split(" ")
            val nome = nameParts.firstOrNull() ?: ""
            val cognome = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

            val userDataMap = hashMapOf(
                "nome" to nome,
                "cognome" to cognome,
                "dataNascita" to "",
                "email" to email,
                "telefono" to "",
                "sesso" to "",
                "photoUrl" to (auth.currentUser?.photoUrl?.toString() ?: "")
            )

            firestore.collection("users").document(userId)
                .set(userDataMap)
                .addOnSuccessListener {
                    val newUserData = UserData(
                        nome = nome,
                        cognome = cognome,
                        dataNascita = "",
                        email = email,
                        telefono = "",
                        sesso = "",
                        photoUrl = auth.currentUser?.photoUrl?.toString() ?: ""
                    )
                    userData.value = newUserData
                    Log.d("AuthViewModel", "Google user profile created successfully")
                }
                .addOnFailureListener {
                    Log.e("AuthViewModel", "Error creating Google user profile", it)
                }
        }
    }

    fun updateUserData(
        updatedUserData: UserData,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val userDataMap = mapOf(
                "nome" to updatedUserData.nome,
                "cognome" to updatedUserData.cognome,
                "dataNascita" to updatedUserData.dataNascita,
                "email" to updatedUserData.email,
                "telefono" to updatedUserData.telefono,
                "sesso" to updatedUserData.sesso,
                "photoUrl" to updatedUserData.photoUrl
            )

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        firestore.collection("users").document(userId)
                            .update(userDataMap)
                            .addOnSuccessListener {
                                userData.value = updatedUserData
                                onSuccess()
                            }
                            .addOnFailureListener(onFailure)
                    } else {
                        firestore.collection("users").document(userId)
                            .set(userDataMap)
                            .addOnSuccessListener {
                                userData.value = updatedUserData
                                onSuccess()
                            }
                            .addOnFailureListener(onFailure)
                    }
                }
                .addOnFailureListener(onFailure)
        } else {
            onFailure(Exception("Utente non autenticato"))
        }
    }

    fun deleteAccount(
        password: String? = null,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        onNeedReauth: () -> Unit = {}
    ) {
        val user = auth.currentUser
        val userId = getCurrentUserId()

        if (user != null && userId != null) {
            val performDeletion = {
                user.delete()
                    .addOnSuccessListener {
                        firestore.collection("users").document(userId)
                            .delete()
                            .addOnSuccessListener {
                                if (userData.value.photoUrl.startsWith("/")) {
                                    deleteProfileImage(userData.value.photoUrl)
                                }
                                userData.value = UserData()
                                onSuccess()
                            }
                            .addOnFailureListener {
                                userData.value = UserData()
                                onSuccess()
                            }
                    }
                    .addOnFailureListener { authException ->
                        if (authException is FirebaseAuthRecentLoginRequiredException) {
                            onNeedReauth()
                        } else {
                            onFailure(authException)
                        }
                    }
            }

            if (password != null) {
                val email = user.email
                if (email != null) {
                    val credential = EmailAuthProvider.getCredential(email, password)
                    user.reauthenticate(credential)
                        .addOnSuccessListener { performDeletion() }
                        .addOnFailureListener(onFailure)
                } else {
                    onFailure(Exception("Email utente non disponibile"))
                }
            } else {
                performDeletion()
            }
        } else {
            onFailure(Exception("Utente non autenticato"))
        }
    }

    private fun deleteProfileImage(imagePath: String) {
        try {
            if (imagePath.startsWith("/") && File(imagePath).exists()) {
                File(imagePath).delete()
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Errore eliminazione immagine profilo", e)
        }
    }

    fun logout() {
        auth.signOut()
        userData.value = UserData()
    }

    fun signInWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        _isLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Google sign-in successful")
                    // Non chiamare loadUserData() qui, sarà chiamato nel callback onSuccess
                    onSuccess()
                } else {
                    Log.e("AuthViewModel", "Google sign-in failed", task.exception)
                    onFailure(task.exception ?: Exception("Errore sconosciuto durante l'accesso Google"))
                }
            }
    }
}