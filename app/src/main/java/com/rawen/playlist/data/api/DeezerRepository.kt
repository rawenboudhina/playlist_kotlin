package com.rawen.playlist.data.api

import com.rawen.playlist.models.Song
import com.rawen.playlist.models.toSong
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DeezerRepository {

    private val api: DeezerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DeezerApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiService::class.java)
    }

    suspend fun searchTracks(query: String): List<Song> {
        return try {
            val response = api.searchTracks(query)
            response.data.map { it.toSong() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTrendingTracks(): List<Song> {
        return try {
            val response = api.getChart()
            response.tracks?.data?.map { it.toSong() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
