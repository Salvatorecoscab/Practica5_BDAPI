package com.example.crudfirebaseapp.models

data class User(
    val uid: String = "",
    val name: String = "",  // Este debe ser el nombre real
    val email: String = "",  // Este debe ser el email real
    val isAdmin: Boolean = false,
    val profileImageUrl: String = ""
)