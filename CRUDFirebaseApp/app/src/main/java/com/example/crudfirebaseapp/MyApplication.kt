package com.example.crudfirebaseapp // O tu paquete raíz

import android.app.Application
import android.util.Log
// import com.google.firebase.BuildConfig; // <-- Considera eliminar este import si no lo usas para nada más
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication_AppCheck_Test" // Nuevo TAG para filtrar fácil
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Usar una variable local para asegurar que es el valor que estamos evaluando
        val isAppInDebugMode = com.example.crudfirebaseapp.BuildConfig.DEBUG
        Log.d(TAG, "VALOR DE isAppInDebugMode = $isAppInDebugMode")

        if (isAppInDebugMode) {
            Log.i(TAG, "RAMA IF (DEBUG): Instalando App Check DEBUG Provider.")
           firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
          )
        } else {
            Log.i(TAG, "RAMA ELSE (RELEASE): Instalando App Check PLAY INTEGRITY Provider.")
           firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
       )
        }
        Log.d(TAG, "Inicialización de App Check Provider completada (o intentada).")
    }
}