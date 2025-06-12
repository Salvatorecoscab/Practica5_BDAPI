package com.example.crudfirebaseapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseUtils {
    fun updateFCMTokenForCurrentUser() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (token != null) { // Añadir verificación de nulidad para el token
                        val userTokensRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(user.uid)
                            .child("fcmTokens") // NODO PLURAL "fcmTokens"

                        // Guardar el token como una clave en el mapa con valor true
                        // Esto permite múltiples tokens por usuario y es fácil de manejar
                        val tokenEntry = mapOf(token to true)

                        userTokensRef.updateChildren(tokenEntry) // Usar updateChildren para añadir/actualizar
                            .addOnSuccessListener {
                                Log.d("FirebaseUtils", "FCM token ($token) guardado/actualizado en Realtime DB bajo fcmTokens")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseUtils", "Error al guardar FCM token en fcmTokens", e)
                            }
                    } else {
                        Log.e("FirebaseUtils", "El token FCM obtenido es nulo.")
                    }
                } else {
                    Log.e("FirebaseUtils", "Error al obtener FCM token", task.exception)
                }
            }
    }
}
