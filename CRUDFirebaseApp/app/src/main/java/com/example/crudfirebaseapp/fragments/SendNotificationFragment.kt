package com.example.crudfirebaseapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.crudfirebaseapp.FcmApi
import com.example.crudfirebaseapp.NotificationBody
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.SendMessageDto
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
// Importaciones para Firebase Realtime Database
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class SendNotificationFragment : Fragment() {

    private lateinit var titleEditText: TextInputEditText
    private lateinit var messageEditText: TextInputEditText
    private lateinit var sendButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var headerTextView: TextView

    private val api: FcmApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        // Configurar el interceptor de logging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Loguea cabeceras y cuerpo de la petición/respuesta
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Añadir el interceptor
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            // Asegúrate que esta URL base sea la correcta para tu Cloud Function
            // Si tu FcmApi tiene @POST("sendHttpPushNotification"), la baseUrl debe terminar en "/"
            // Ejemplo: "https://sendHttpPushNotification-abcdef123-uc.a.run.app/" si ese es el endpoint completo
            // O si el endpoint es "https://<region>-<project-id>.cloudfunctions.net/sendHttpPushNotification"
            // la baseUrl sería "https://<region>-<project-id>.cloudfunctions.net/"
            .baseUrl("https://sendotification-857833539524.us-central1.run.app") // Reemplaza con tu URL base correcta
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FcmApi::class.java)
    }

    private var targetUserId: String? = null
    private var targetUserName: String? = null

    companion object {
        private const val TAG = "SendNotificationFrag"
        const val ARG_TARGET_USER_ID = "TARGET_USER_ID"
        const val ARG_TARGET_USER_NAME = "TARGET_USER_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetUserId = it.getString(ARG_TARGET_USER_ID)
            targetUserName = it.getString(ARG_TARGET_USER_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_send_notification, container, false)

        titleEditText = view.findViewById(R.id.editTextNotificationTitle)
        messageEditText = view.findViewById(R.id.editTextNotificationMessage)
        sendButton = view.findViewById(R.id.buttonSendNotification)
        progressBar = view.findViewById(R.id.progressBarSendNotification)
        headerTextView = view.findViewById(R.id.textViewSendNotificationTitleHeader)

        if (targetUserName != null) {
            headerTextView.text = "Enviar a: $targetUserName"
            sendButton.text = "Enviar a $targetUserName"
        } else if (targetUserId != null) {
            headerTextView.text = "Enviar a Usuario Específico"
            sendButton.text = "Enviar Notificación"
        } else {
            headerTextView.text = "Enviar Notificación (Error: Sin Destinatario)"
            sendButton.isEnabled = false
        }

        sendButton.setOnClickListener {
            // El usuario actual (admin) debe estar logueado para obtener su token de ID
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "ERROR CRÍTICO: Usuario admin no logueado.")
                Toast.makeText(requireContext(), "Error de autenticación del administrador.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            proceedWithSendingNotification()
        }
        return view
    }

    private fun proceedWithSendingNotification() {
        if (targetUserId == null) {
            Toast.makeText(requireContext(), "Error: No se ha especificado un destinatario.", Toast.LENGTH_LONG).show()
            return
        }

        val title = titleEditText.text.toString().trim()
        val message = messageEditText.text.toString().trim()

        if (title.isEmpty()) {
            titleEditText.error = "El título no puede estar vacío"
            titleEditText.requestFocus()
            return
        }
        if (message.isEmpty()) {
            messageEditText.error = "El mensaje no puede estar vacío"
            messageEditText.requestFocus()
            return
        }
        titleEditText.error = null
        messageEditText.error = null

        fetchFcmTokenAndSendNotification(targetUserId!!, title, message)
    }


    private fun fetchFcmTokenAndSendNotification(userId: String, title: String, message: String) {
        progressBar.visibility = View.VISIBLE
        sendButton.isEnabled = false

        val userTokensRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("fcmTokens")
        Log.d(TAG, "Buscando tokens para el usuario: $userId en Realtime Database")

        userTokensRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    val firstToken = snapshot.children.firstOrNull()?.key

                    if (firstToken != null && firstToken.isNotEmpty()) {
                        Log.d(TAG, "Token FCM encontrado para $userId: $firstToken")
                        // Ahora se llamará a sendNotificationViaBackend con el token del destinatario
                        sendNotificationViaBackend(firstToken, title, message)
                    } else {
                        Log.w(TAG, "No se encontraron tokens FCM válidos para el usuario: $userId")
                        Toast.makeText(requireContext(), "Token FCM no disponible para este usuario.", Toast.LENGTH_LONG).show()
                        resetUiState()
                    }
                } else {
                    Log.w(TAG, "Nodo 'fcmTokens' no encontrado o vacío para el usuario: $userId")
                    Toast.makeText(requireContext(), "Token FCM no disponible (sin tokens registrados).", Toast.LENGTH_LONG).show()
                    resetUiState()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener token FCM para $userId desde Realtime Database", error.toException())
                Toast.makeText(requireContext(), "Error al obtener token: ${error.message}", Toast.LENGTH_LONG).show()
                resetUiState()
            }
        })
    }

    // Modificado para incluir la obtención del token de ID de Firebase del admin
    private fun sendNotificationViaBackend(
        destinationFcmToken: String, // Token del usuario destinatario
        title: String,
        message: String
    ) {
        val notificationDto = SendMessageDto(
            to = destinationFcmToken, // Este es el token del usuario al que se envía la notificación
            notification = NotificationBody(
                title = title,
                body = message
            )
        )

        Log.d(TAG, "Preparando notificación para DTO: $notificationDto")

        // Obtener el token de ID del usuario admin actual para la cabecera Authorization
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "Admin no logueado al intentar enviar notificación.")
            Toast.makeText(requireContext(), "Error de autenticación del admin.", Toast.LENGTH_LONG).show()
            resetUiState() // Asegúrate de que la UI se restablezca
            return
        }

        currentUser.getIdToken(true) // true para forzar la actualización del token
            .addOnSuccessListener { getIdTokenResult ->
                val idToken = getIdTokenResult.token
                if (idToken == null) {
                    Log.e(TAG, "El token de ID de Firebase del admin es null.")
                    Toast.makeText(requireContext(), "No se pudo obtener el token de autenticación del admin.", Toast.LENGTH_LONG).show()
                    resetUiState()
                    return@addOnSuccessListener
                }

                val authToken = "Bearer $idToken"
                Log.d(TAG, "Token de autorización para la Cloud Function preparado.")

                // Lanzar la coroutina para la llamada de red DESPUÉS de obtener el token de ID
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        Log.d(TAG, "Enviando a API con AuthToken y DTO...")
                        // Llamada correcta a la API con ambos parámetros
                        val response = api.sendMessageToDevice(authToken, notificationDto)

                        if (response.isSuccessful) {
                            Log.d(TAG, "Notificación enviada exitosamente a través de la Cloud Function.")
                            Toast.makeText(requireContext(), "Notificación enviada.", Toast.LENGTH_LONG).show()
                            titleEditText.text?.clear()
                            messageEditText.text?.clear()
                            if (isAdded) {
                                parentFragmentManager.popBackStack()
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Error de la Cloud Function: ${response.code()} - $errorBody")
                            Toast.makeText(requireContext(), "Error del backend: ${response.code()} ${errorBody ?: ""}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: HttpException) {
                        Log.e(TAG, "HttpException al llamar a la Cloud Function", e)
                        Toast.makeText(requireContext(), "Error de red (HTTP): ${e.message()}", Toast.LENGTH_LONG).show()
                    } catch (e: IOException) {
                        Log.e(TAG, "IOException al llamar a la Cloud Function", e)
                        Toast.makeText(requireContext(), "Error de red (IO): ${e.message}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Excepción general al llamar a la Cloud Function", e)
                        Toast.makeText(requireContext(), "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        // El resetUiState ya se maneja aquí, pero asegúrate de que
                        // no se llame antes si la obtención del token falla.
                        // Se movió resetUiState a los bloques de error de getIdToken también.
                        if (isActive) { // Verificar si la coroutina sigue activa
                            resetUiState()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener el token de ID de Firebase del admin", exception)
                Toast.makeText(requireContext(), "Error al obtener autenticación del admin: ${exception.message}", Toast.LENGTH_LONG).show()
                resetUiState()
            }
    }


    private fun resetUiState() {
        if (isAdded) {
            progressBar.visibility = View.GONE
            sendButton.isEnabled = true
        }
    }
}
