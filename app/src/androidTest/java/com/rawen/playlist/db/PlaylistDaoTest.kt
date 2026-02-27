package com.rawen.playlist.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.rawen.playlist.data.db.AppDatabase
import com.rawen.playlist.data.db.PlaylistDao
import com.rawen.playlist.models.Playlist
import com.rawen.playlist.models.PlaylistSong
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addDetectAndRemoveSongFromPlaylist() = runBlocking {
        val playlistId = dao.insertPlaylist(Playlist(name = "Test"))

        val songId = 42L
        val ps = PlaylistSong(
            playlistId = playlistId,
            songId = songId,
            title = "Song",
            artist = "Artist",
            duration = 1000L,
            path = "content://media/external/audio/media/1",
            albumArtUri = null,
            isLocal = true,
            deezerTrackId = null,
            albumName = null
        )

        // Initially absent
        assertEquals(0, dao.hasSong(playlistId, songId))

        // Add once
        dao.addSongToPlaylist(ps)
        assertEquals(1, dao.hasSong(playlistId, songId))
        assertTrue(dao.getPlaylistIdsForSong(songId).contains(playlistId))

        // Add again (REPLACE on conflict), still one entry
        dao.addSongToPlaylist(ps.copy(title = "Song Updated"))
        assertEquals(1, dao.hasSong(playlistId, songId))

        // Remove
        dao.removeSongFromPlaylist(playlistId, songId)
        assertEquals(0, dao.hasSong(playlistId, songId))
    }
}
