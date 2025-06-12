package com.example.crudfirebaseapp.services // O tu paquete deseado

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.crudfirebaseapp.MainActivity // Asume que MainActivity es tu actividad principal al abrir la notificación
import com.example.crudfirebaseapp.R // Para acceder a recursos como strings e íconos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    /**
     * Se llama cuando se genera un nuevo token FCM o cuando el token existente cambia.
     * Este token es el identificador único del dispositivo para recibir notificaciones.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM generado: $token")
        sendRegistrationToServer(token)
    }

    /**
     * Guarda el token FCM en tu Firebase Realtime Database bajo el perfil del usuario actual.
     */
    private fun sendRegistrationToServer(token: String?) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && token != null) {
            // Usamos el token como clave y 'true' como valor para facilitar la adición/eliminación
            // y para poder tener múltiples tokens por usuario (si usa la app en varios dispositivos).
            val tokenMap = mapOf(token to true)
            FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("fcmTokens") // Creamos un nodo 'fcmTokens' para este usuario
                .updateChildren(tokenMap) // updateChildren para añadir/actualizar sin borrar otros tokens
                .addOnSuccessListener {
                    Log.i(TAG, "Token FCM ($token) actualizado en DB para usuario: $currentUserId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar token FCM en DB para usuario $currentUserId", e)
                }
        } else {
            Log.w(TAG, "No se pudo guardar el token: Usuario no logueado o token nulo.")
        }
    }

    /**
     * Se llama cuando se recibe un mensaje FCM mientras la app está en primer plano.
     * Si la app está en segundo plano o cerrada y el mensaje es de tipo "notificación",
     * FCM lo maneja automáticamente mostrando una notificación en la bandeja del sistema.
     * Si es un mensaje de "datos" o quieres personalizar la notificación en segundo plano,
     * esta función también se llama (con algunas diferencias según el estado de la app).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje FCM recibido de: ${remoteMessage.from}")

        // Puedes enviar datos adicionales junto con la notificación.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Payload de datos del mensaje: " + remoteMessage.data)
            // Ejemplo: val customData = remoteMessage.data["mi_clave_personalizada"]
        }

        // Si el mensaje contiene un payload de notificación, úsalo para mostrarla.
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Título de la Notificación: ${notification.title}")
            Log.d(TAG, "Cuerpo de la Notificación: ${notification.body}")
            sendVisualNotification(notification.title, notification.body)
        }
    }

    /**
     * Crea y muestra una notificación visual simple en la bandeja del sistema.
     */
    private fun sendVisualNotification(title: String?, messageBody: String?) {
        // Intent que se ejecutará cuando el usuario toque la notificación
        val intent = Intent(this, MainActivity::class.java) // Cambia MainActivity si es otra
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Para evitar múltiples instancias de la actividad

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            pendingIntentFlags
        )

        val channelId = getString(R.string.default_notification_channel_id) // Necesitarás este string
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification) // ¡¡IMPORTANTE: Crea este ícono!!
            .setContentTitle(title ?: getString(R.string.app_name))
            .setContentText(messageBody)
            .setAutoCancel(true) // La notificación se cierra al tocarla
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Para mayor visibilidad
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android 8.0 (Oreo, API 26), los canales de notificación son obligatorios.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.default_notification_channel_name) // Necesitarás este string
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que aparezca
            )
            // channel.description = "Canal para notificaciones generales de la app" // Opcional
            notificationManager.createNotificationChannel(channel)
        }

        // Muestra la notificación. Usa un ID único si vas a mostrar múltiples notificaciones a la vez.
        notificationManager.notify(System.currentTimeMillis().toInt() /* ID único de la notificación */, notificationBuilder.build())
    }
}