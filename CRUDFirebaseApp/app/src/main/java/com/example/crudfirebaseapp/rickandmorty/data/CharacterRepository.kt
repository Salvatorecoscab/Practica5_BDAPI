package com.example.crudfirebaseapp.rickandmorty.data

import android.util.Log
import com.example.crudfirebaseapp.rickandmorty.network.ApiClient
import com.example.crudfirebaseapp.rickandmorty.network.dto.CharacterApiResponse
import com.example.crudfirebaseapp.rickandmorty.network.dto.CharacterDetailDto
import com.example.crudfirebaseapp.rickandmorty.network.dto.CharacterDto
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterDetailUiModel
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CharacterRepository {

    private val api = ApiClient.instance
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Obtiene la lista general de personajes
    suspend fun getCharacters(page: Int): Pair<List<CharacterUiModel>, Boolean> {
        return fetchAndProcess(page) {
            api.getCharacters(it)
        }
    }

    // NUEVO: Obtiene la lista de personajes filtrada por búsqueda
    suspend fun searchCharacters(query: String, page: Int): Pair<List<CharacterUiModel>, Boolean> {
        return fetchAndProcess(page) {
            api.searchCharacters(query, it)
        }
    }

    // Función interna para evitar repetir código
    private suspend fun fetchAndProcess(
        page: Int,
        apiCall: suspend (Int) -> CharacterApiResponse
    ): Pair<List<CharacterUiModel>, Boolean> = withContext(Dispatchers.IO) {
        try {
            val (charactersFromApi, hasNextPage, favoriteIds) = coroutineScope {
                val apiResult = async { apiCall(page) }
                val favoriteIdsResult = async { getFavoriteIdsFromFirebase() }
                val response = apiResult.await()
                Triple(response.results, response.info.next != null, favoriteIdsResult.await())
            }

            val characterUiModels = charactersFromApi.map { dto ->
                dto.toUiModel(isFavorite = favoriteIds.contains(dto.id))
            }
            return@withContext Pair(characterUiModels, hasNextPage)
        } catch (e: Exception) {
            // Un error 404 es común si la búsqueda no encuentra resultados, lo tratamos como una lista vacía.
            Log.w("CharacterRepository", "API call failed (might be 404 Not Found): ${e.message}")
            return@withContext Pair(emptyList(), false)
        }
    }

    private suspend fun getFavoriteIdsFromFirebase(): Set<Int> {
        // ... (sin cambios en esta función)
        val userId = auth.currentUser?.uid ?: return emptySet()
        val favoritesRef = database.getReference("users/$userId/favoriteCharacters")

        return try {
            val snapshot = favoritesRef.get().await()
            if (snapshot.exists()) {
                snapshot.children.mapNotNull { it.key?.toInt() }.toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            Log.e("CharacterRepository", "Failed to fetch favorite IDs", e)
            emptySet()
        }
    }

    suspend fun toggleFavorite(character: CharacterUiModel) {
        // ... (sin cambios en esta función)
        val userId = auth.currentUser?.uid ?: return
        val favoriteRef = database.getReference("users/$userId/favoriteCharacters/${character.id}")
        val newStatus = !character.isFavorite

        try {
            if (newStatus) {
                favoriteRef.setValue(true).await()
            } else {
                favoriteRef.removeValue().await()
            }
        } catch (e: Exception) {
            Log.e("CharacterRepository", "Failed to toggle favorite status in Firebase", e)
        }
    }
    // NUEVO: Obtiene la lista de personajes favoritos completos
    suspend fun getFavoriteCharacters(): List<CharacterUiModel> {
        // 1. Obtener los IDs de Firebase
        val favoriteIds = getFavoriteIdsFromFirebase()
        if (favoriteIds.isEmpty()) {
            return emptyList()
        }

        // 2. Convertir los IDs a un string separado por comas
        val idsAsString = favoriteIds.joinToString(",")

        return try {
            // 3. Llamar a la API para obtener los detalles de esos personajes
            // Asegúrate de que tu RickAndMortyApi tiene el método getMultipleCharacters
            val charactersFromApi = api.getMultipleCharacters(idsAsString)

            // 4. Mapear a nuestro modelo de UI, marcando todos como favoritos
            charactersFromApi.map { dto ->
                dto.toUiModel(isFavorite = true)
            }
        } catch (e: Exception) {
            Log.e("CharacterRepository", "Failed to fetch favorite characters by ID", e)
            emptyList()
        }
    }
    suspend fun getCharacterDetails(id: Int): CharacterDetailUiModel? {
        return try {
            val characterDto = api.getCharacterById(id)
            val favoriteIds = getFavoriteIdsFromFirebase()
            characterDto.toDetailUiModel(isFavorite = favoriteIds.contains(id))
        } catch (e: Exception) {
            Log.e("CharacterRepository", "Failed to get details for id $id", e)
            null
        }
    }

    private fun CharacterDto.toUiModel(isFavorite: Boolean): CharacterUiModel {
        // ... (sin cambios en esta función)
        return CharacterUiModel(
            id = this.id,
            name = this.name,
            status = this.status,
            species = this.species,
            image = this.image,
            isFavorite = isFavorite
        )
    }
    private fun CharacterDetailDto.toDetailUiModel(isFavorite: Boolean): CharacterDetailUiModel {
        return CharacterDetailUiModel(
            id = this.id,
            name = this.name,
            image = this.image,
            status = this.status,
            species = this.species,
            gender = this.gender,
            originName = this.origin.name,
            locationName = this.location.name,
            isFavorite = isFavorite
        )
    }
}