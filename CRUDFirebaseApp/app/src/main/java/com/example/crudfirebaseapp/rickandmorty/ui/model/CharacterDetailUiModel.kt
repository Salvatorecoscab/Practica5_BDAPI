package com.example.crudfirebaseapp.rickandmorty.ui.model

// Modelo simplificado para la pantalla de detalle
data class CharacterDetailUiModel(
    val id: Int,
    val name: String,
    val image: String,
    val status: String,
    val species: String,
    val gender: String,
    val originName: String,
    val locationName: String,
    var isFavorite: Boolean
)