package com.example.crudfirebaseapp.rickandmorty.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crudfirebaseapp.rickandmorty.data.CharacterRepository
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterDetailUiModel
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel
import kotlinx.coroutines.launch

class CharacterDetailViewModel : ViewModel() {

    private val repository = CharacterRepository()

    private val _characterDetails = MutableLiveData<CharacterDetailUiModel?>()
    val characterDetails: LiveData<CharacterDetailUiModel?> = _characterDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchCharacterDetails(id: Int) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val details = repository.getCharacterDetails(id)
            _characterDetails.postValue(details)
            _isLoading.postValue(false)
        }
    }

    fun toggleFavorite() {
        val character = _characterDetails.value ?: return
        viewModelScope.launch {
            // Convertimos el DetailUiModel a un CharacterUiModel simple para el repositorio
            val simpleModel = CharacterUiModel(
                id = character.id,
                name = character.name,
                image = character.image,
                status = character.status,
                species = character.species,
                isFavorite = character.isFavorite
            )
            repository.toggleFavorite(simpleModel)

            // Actualizamos el estado local para que el cambio sea instantáneo en la UI
            val updatedCharacter = character.copy(isFavorite = !character.isFavorite)
            _characterDetails.postValue(updatedCharacter)
        }
    }
}