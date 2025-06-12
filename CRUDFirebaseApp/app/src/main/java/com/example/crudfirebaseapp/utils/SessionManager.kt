package com.example.crudfirebaseapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
class SessionManager(private val context: Context) {
    private var pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = pref.edit()

    companion object {
        private const val PREF_NAME = "LoginPref"
        private const val IS_LOGIN = "IsLoggedIn"
        private const val KEY_UID = "uid"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_ADMIN = "isAdmin"
        private const val KEY_NAME = "name"

        // Método mejorado para verificar el rol del usuario
        fun checkUserRole(userId: String, callback: (Boolean) -> Unit) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(userId)

            // Usar keepSynced para mantener datos disponibles offline
            userRef.keepSynced(true)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                        callback(isAdmin)
                    } else {
                        // Usuario no encontrado, asumir no admin
                        callback(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SessionManager", "Error verificando rol: ${error.message}")
                    // En caso de error, asumir que no es admin
                    callback(false)
                }
            })
        }
    }

    // Resto de métodos...

    fun createLoginSession(uid: String, email: String, isAdmin: Boolean, name: String) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_UID, uid)
        editor.putString(KEY_EMAIL, email)
        editor.putBoolean(KEY_IS_ADMIN, isAdmin)
        editor.putString(KEY_NAME, name)
        editor.apply()

        Log.d("SessionManager", "Sesión creada: $name, isAdmin: $isAdmin")
    }

    fun isLoggedIn(): Boolean {
        return pref.getBoolean(IS_LOGIN, false)
    }

    fun isAdmin(): Boolean {
        return pref.getBoolean(KEY_IS_ADMIN, false)
    }

    fun getUserDetails(): HashMap<String, String> {
        val user = HashMap<String, String>()
        user[KEY_UID] = pref.getString(KEY_UID, "") ?: ""
        user[KEY_EMAIL] = pref.getString(KEY_EMAIL, "") ?: ""
        user[KEY_NAME] = pref.getString(KEY_NAME, "") ?: ""
        return user
    }

    fun getUserId(): String {
        return pref.getString(KEY_UID, "") ?: ""
    }

    fun getUserEmail(): String {
        return pref.getString(KEY_EMAIL, "") ?: ""
    }

    fun getUserName(): String {
        return pref.getString(KEY_NAME, "") ?: ""
    }

    fun logoutUser() {
        editor.clear()
        editor.apply()
    }
}