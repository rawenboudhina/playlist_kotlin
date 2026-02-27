package com.rawen.playlist.data.api

import com.rawen.playlist.models.DeezerChartResponse
import com.rawen.playlist.models.DeezerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApiService {

    @GET("search")
    suspend fun searchTracks(@Query("q") query: String): DeezerSearchResponse

    @GET("chart/0")
    suspend fun getChart(): DeezerChartResponse

    companion object {
        const val BASE_URL = "https://api.deezer.com/"
    }
}
