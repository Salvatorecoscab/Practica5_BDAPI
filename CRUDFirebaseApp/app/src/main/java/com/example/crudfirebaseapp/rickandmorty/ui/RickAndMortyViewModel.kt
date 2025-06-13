package com.example.crudfirebaseapp.rickandmorty.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crudfirebaseapp.rickandmorty.data.CharacterRepository
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RickAndMortyViewModel : ViewModel() {

    private val repository = CharacterRepository()

    // --- ESTADO GENERAL ---
    private var currentPage = 1
    private var isLastPage = false
    private var isLoading = false
    private var searchQuery = ""
    private var searchJob: Job? = null

    // --- NUEVO ESTADO PARA EL FILTRO DE FAVORITOS ---
    private val _isFavoritesFilterActive = MutableLiveData(false)
    val isFavoritesFilterActive: LiveData<Boolean> = _isFavoritesFilterActive

    // LiveData para la UI
    private val _characters = MutableLiveData<List<CharacterUiModel>?>(emptyList())
    val characters: MutableLiveData<List<CharacterUiModel>?> = _characters

    private val _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean> = _showLoading

    init {
        fetchCharacters(isNewList = true)
    }

    private fun fetchCharacters(isNewList: Boolean = false) {
        if (isLoading) return

        if (isNewList) {
            currentPage = 1
            _characters.value = emptyList() // Limpia la lista para una nueva carga
        }

        viewModelScope.launch {
            isLoading = true
            if (isNewList) _showLoading.postValue(true)

            val (result, hasNext) = when {
                // Si el filtro de favoritos está activo, lo priorizamos
                _isFavoritesFilterActive.value == true -> {
                    Pair(repository.getFavoriteCharacters(), false) // Favoritos no se paginan
                }
                // Si hay una búsqueda, la realizamos
                searchQuery.isNotBlank() -> {
                    repository.searchCharacters(searchQuery, currentPage)
                }
                // Si no, obtenemos la lista general
                else -> {
                    repository.getCharacters(currentPage)
                }
            }

            isLastPage = !hasNext

            if (isNewList) {
                _characters.postValue(result)
            } else {
                val currentList = _characters.value ?: emptyList()
                _characters.postValue(currentList + result)
            }

            isLoading = false
            if (isNewList) _showLoading.postValue(false)
        }
    }

    fun loadMoreCharacters() {
        // La paginación se deshabilita si el filtro de favoritos está activo
        if (!isLastPage && _isFavoritesFilterActive.value == false) {
            currentPage++
            fetchCharacters()
        }
    }

    fun onRefresh() {
        // Al refrescar, reiniciamos todos los filtros
        _isFavoritesFilterActive.value = false
        searchQuery = ""
        fetchCharacters(isNewList = true)
    }

    fun setSearchQuery(query: String) {
        searchJob?.cancel()
        // Si el usuario busca, desactivamos el filtro de favoritos
        if (_isFavoritesFilterActive.value == true) {
            _isFavoritesFilterActive.value = false
        }
        searchQuery = query
        searchJob = viewModelScope.launch {
            delay(500)
            fetchCharacters(isNewList = true)
        }
    }

    // NUEVA FUNCIÓN PARA EL BOTÓN DE FILTRO
    fun onFavoritesFilterToggled() {
        val newFilterState = !(_isFavoritesFilterActive.value ?: false)
        _isFavoritesFilterActive.value = newFilterState
        // Si se activa el filtro, limpiamos la búsqueda
        if (newFilterState) {
            searchQuery = ""
        }
        fetchCharacters(isNewList = true)
    }

    fun toggleFavorite(character: CharacterUiModel) {
        viewModelScope.launch {
            repository.toggleFavorite(character)
            val updatedList = _characters.value?.mapNotNull {
                if (it.id == character.id) {
                    // Si el filtro de favoritos está activo y quitamos uno, debe desaparecer
                    if (_isFavoritesFilterActive.value == true && it.isFavorite) {
                        null
                    } else {
                        it.copy(isFavorite = !it.isFavorite)
                    }
                } else {
                    it
                }
            }
            _characters.postValue(updatedList)
        }
    }
}