package com.example.crudfirebaseapp.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crudfirebaseapp.rickandmorty.data.CharacterRepository
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {

    private val repository = CharacterRepository()

    private val _favorites = MutableLiveData<List<CharacterUiModel>>()
    val favorites: LiveData<List<CharacterUiModel>> = _favorites

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Carga la lista inicial de favoritos
    fun fetchFavoriteCharacters() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _favorites.postValue(repository.getFavoriteCharacters())
            _isLoading.postValue(false)
        }
    }

    // Se llama cuando el usuario quita un favorito desde esta pantalla
    fun unfavoriteCharacter(character: CharacterUiModel) {
        viewModelScope.launch {
            // Primero, actualiza el estado en Firebase
            repository.toggleFavorite(character)
            // Después, vuelve a cargar la lista para que el personaje desaparecido se refleje en la UI
            fetchFavoriteCharacters()
        }
    }
}