package com.rawen.playlist.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val albumArtUri: String? = null,
    val isLocal: Boolean = true,
    val deezerTrackId: Long? = null,
    val albumName: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song {
        return Song(
            id = songId,
            title = title,
            artist = artist,
            duration = duration,
            path = path,
            albumArtUri = albumArtUri,
            isLocal = isLocal,
            deezerTrackId = deezerTrackId,
            albumName = albumName
        )
    }
}
