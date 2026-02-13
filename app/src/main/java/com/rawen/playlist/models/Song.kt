package com.rawen.playlist.models

data class Song (
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val albumArtUri: String? = null
)