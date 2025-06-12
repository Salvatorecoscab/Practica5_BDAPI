package com.example.crudfirebaseapp

import android.app.Application
import com.google.firebase.FirebaseApp

class CRUDFirebaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
    }
}