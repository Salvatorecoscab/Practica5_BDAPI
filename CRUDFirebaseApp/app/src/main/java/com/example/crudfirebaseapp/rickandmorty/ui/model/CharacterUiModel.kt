package com.example.crudfirebaseapp.rickandmorty.ui.model

// Este modelo representa un personaje en la interfaz de usuario.
data class CharacterUiModel(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val image: String,
    var isFavorite: Boolean = false
)