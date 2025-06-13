package com.example.crudfirebaseapp.rickandmorty.network

import com.example.crudfirebaseapp.rickandmorty.network.dto.CharacterApiResponse
import com.example.crudfirebaseapp.rickandmorty.network.dto.CharacterDetailDto
import com.example.crudfirebaseapp.rickandmorty.network.dto.CharacterDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RickAndMortyApi {
    // Para obtener la lista general paginada
    @GET("api/character")
    suspend fun getCharacters(@Query("page") page: Int): CharacterApiResponse

    // NUEVO: Para buscar personajes por nombre, también paginado
    @GET("api/character")
    suspend fun searchCharacters(
        @Query("name") name: String,
        @Query("page") page: Int
    ): CharacterApiResponse
    // NUEVO: Para obtener múltiples personajes por sus IDs
    @GET("api/character/{ids}")
    suspend fun getMultipleCharacters(
        @Path("ids") ids: String
    ): List<CharacterDto>
    @GET("api/character/{id}")
    suspend fun getCharacterById(@Path("id") id: Int): CharacterDetailDto

}