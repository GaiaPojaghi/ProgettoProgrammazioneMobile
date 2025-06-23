package com.example.fitjourney.util.viewModel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

open class BaseViewModel : ViewModel() {

    protected val auth: FirebaseAuth = FirebaseAuth.getInstance()
    protected val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    protected fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    protected fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    protected fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    protected fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    protected fun getCurrentTimestamp(): String {
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return timestampFormat.format(Date())
    }
}
