package com.example.crudfirebaseapp


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// FcmApi.kt
interface FcmApi {
    @POST("sendHttpPushNotification") // Asegúrate que este path coincida con el nombre de tu función desplegada
    suspend fun sendMessageToDevice(
        @Header("Authorization") authToken: String, // Nueva cabecera
        @Body body: SendMessageDto
    ): Response<Unit>
}
