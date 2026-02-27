package com.rawen.playlist.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSong(
    @PrimaryKey
    val deezerTrackId: Long,
    val title: String,
    val artist: String,
    val albumName: String? = null,
    val albumArtUrl: String? = null,
    val localFilePath: String,
    val duration: Long, // in ms
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song {
        return Song(
            id = deezerTrackId + 100000, // same offset as DeezerTrack.toSong()
            title = title,
            artist = artist,
            duration = duration,
            path = localFilePath,
            albumArtUri = albumArtUrl,
            isLocal = true, // plays from local file now
            deezerTrackId = deezerTrackId,
            albumName = albumName
        )
    }
}
