package com.rawen.playlist.data.db

import androidx.room.*
import com.rawen.playlist.models.DownloadedSong
import com.rawen.playlist.models.Playlist
import com.rawen.playlist.models.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // ---- Playlists ----

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    @Query("SELECT playlistId FROM playlist_songs WHERE songId = :songId")
    suspend fun getPlaylistIdsForSong(songId: Long): List<Long>

    // ---- Downloaded Songs ----

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloadedSongs(): Flow<List<DownloadedSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedSong(song: DownloadedSong)

    @Query("DELETE FROM downloaded_songs WHERE deezerTrackId = :deezerTrackId")
    suspend fun deleteDownloadedSong(deezerTrackId: Long)

    @Query("SELECT COUNT(*) FROM downloaded_songs WHERE deezerTrackId = :deezerTrackId")
    suspend fun isTrackDownloaded(deezerTrackId: Long): Int

    @Query("SELECT deezerTrackId FROM downloaded_songs")
    fun getAllDownloadedTrackIds(): Flow<List<Long>>

    // ---- Helpers ----
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun hasSong(playlistId: Long, songId: Long): Int
}
