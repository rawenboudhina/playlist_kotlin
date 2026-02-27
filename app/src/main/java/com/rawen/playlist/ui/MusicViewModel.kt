package com.rawen.playlist.ui

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.rawen.playlist.data.MusicDownloadManager
import com.rawen.playlist.data.SongRepository
import com.rawen.playlist.data.api.DeezerRepository
import com.rawen.playlist.data.db.AppDatabase
import com.rawen.playlist.models.DownloadedSong
import com.rawen.playlist.models.Playlist
import com.rawen.playlist.models.PlaylistSong
import com.rawen.playlist.models.Song
import com.rawen.playlist.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    // --- Local Songs ---
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    // --- API Songs (Trending) ---
    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    // --- Search ---
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // --- Loading ---
    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending: StateFlow<Boolean> = _isLoadingTrending.asStateFlow()

    // --- Player State ---
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    // --- Playlists ---
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _currentPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylistSongs: StateFlow<List<Song>> = _currentPlaylistSongs.asStateFlow()

    // --- All songs currently loaded (for player queue) ---
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    // --- Downloaded Songs ---
    private val _downloadedSongs = MutableStateFlow<List<DownloadedSong>>(emptyList())
    val downloadedSongs: StateFlow<List<DownloadedSong>> = _downloadedSongs.asStateFlow()

    private val _downloadedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadedTrackIds: StateFlow<Set<Long>> = _downloadedTrackIds.asStateFlow()

    // --- Folder ---
    private val _customFolderUri = MutableStateFlow<String?>(null)

    // --- Repos ---
    private val deezerRepository = DeezerRepository()
    private var downloadManager: MusicDownloadManager? = null
    private var database: AppDatabase? = null
    private var appContext: Context? = null

    fun setContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        downloadManager = MusicDownloadManager(context.applicationContext)
        database = AppDatabase.getDatabase(context.applicationContext)
        loadPreferences()
        loadSongs()
        loadTrendingTracks()
        observePlaylists()
        observeDownloadedSongs()
    }

    private fun loadPreferences() {
        val prefs = appContext?.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        _customFolderUri.value = prefs?.getString("custom_folder_uri", null)
    }

    fun setCustomFolder(uri: String?) {
        _customFolderUri.value = uri
        val prefs = appContext?.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        prefs?.edit()?.putString("custom_folder_uri", uri)?.apply()
        loadSongs()
    }

    // ============================================================
    // LOAD LOCAL SONGS
    // ============================================================
    fun loadSongs() {
        val context = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val loadedSongs = SongRepository.getSongs(context, _customFolderUri.value)
            _localSongs.value = loadedSongs
            _songs.value = loadedSongs
        }
    }

    // ============================================================
    // DEEZER API
    // ============================================================
    fun loadTrendingTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingTrending.value = true
            val tracks = deezerRepository.getTrendingTracks()
            _trendingSongs.value = tracks
            _isLoadingTrending.value = false
        }
    }

    fun searchDeezer(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            val results = deezerRepository.searchTracks(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    // ============================================================
    // DOWNLOAD (with Room DB tracking)
    // ============================================================
    fun downloadSong(song: Song) {
        val dm = downloadManager ?: return
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dm.downloadAndTrack(song, dao)
            viewModelScope.launch(Dispatchers.Main) {
                appContext?.let {
                    Toast.makeText(it, "Downloading ${song.title}...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ============================================================
    // DELETE SONG FROM DEVICE
    // ============================================================
    fun deleteSongFromDevice(song: Song) {
        val context = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(song.path)
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    _localSongs.value = _localSongs.value.filter { it.id != song.id }
                    _songs.value = _songs.value.filter { it.id != song.id }
                    if (_currentSong.value?.id == song.id) {
                        mediaController?.stop()
                        _currentSong.value = null
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Deleted ${song.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Cannot delete this song", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ============================================================
    // PLAYLISTS
    // ============================================================
    private fun observePlaylists() {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch {
            dao.getAllPlaylists().collect { playlists ->
                _playlists.value = playlists
            }
        }
    }

    fun createPlaylist(name: String) {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylist(Playlist(name = name))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(playlist)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.addSongToPlaylist(
                PlaylistSong(
                    playlistId = playlistId,
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    duration = song.duration,
                    path = song.path,
                    albumArtUri = song.albumArtUri,
                    isLocal = song.isLocal,
                    deezerTrackId = song.deezerTrackId,
                    albumName = song.albumName
                )
            )
            appContext?.let {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(it, "Added to playlist", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    suspend fun getPlaylistIdsForSong(songId: Long): List<Long> {
        val dao = database?.playlistDao() ?: return emptyList()
        return dao.getPlaylistIdsForSong(songId)
    }

    // ============================================================
    // DOWNLOADED SONGS
    // ============================================================
    private fun observeDownloadedSongs() {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch {
            dao.getAllDownloadedSongs().collect { songs ->
                _downloadedSongs.value = songs
            }
        }
        viewModelScope.launch {
            dao.getAllDownloadedTrackIds().collect { ids ->
                _downloadedTrackIds.value = ids.toSet()
            }
        }
    }

    fun deleteDownloadedSong(deezerTrackId: Long) {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDownloadedSong(deezerTrackId)
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        val dao = database?.playlistDao() ?: return
        viewModelScope.launch {
            dao.getSongsForPlaylist(playlistId).collect { playlistSongs ->
                _currentPlaylistSongs.value = playlistSongs.map { it.toSong() }
            }
        }
    }

    // ============================================================
    // MEDIA PLAYER
    // ============================================================
    @OptIn(UnstableApi::class)
    fun initializeController(context: Context) {
        if (mediaController != null) return

        val sessionToken = SessionToken(
            context,
            android.content.ComponentName(context, MusicService::class.java)
        )
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            try {
                val controller = mediaControllerFuture?.get()
                mediaController = controller
                controller?.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentSong(mediaItem)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                })
                updateCurrentSong(controller?.currentMediaItem)
                _isPlaying.value = controller?.isPlaying == true
                startProgressUpdate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    _progress.value = it.currentPosition
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId
        if (mediaId != null) {
            // Search in all song lists
            val song = _songs.value.find { it.id.toString() == mediaId }
                ?: _trendingSongs.value.find { it.id.toString() == mediaId }
                ?: _searchResults.value.find { it.id.toString() == mediaId }
                ?: _currentPlaylistSongs.value.find { it.id.toString() == mediaId }
            _currentSong.value = song
        }
    }

    fun playSong(song: Song) {
        val controller = mediaController ?: return

        // Update the active song list
        _songs.value = when {
            _trendingSongs.value.any { it.id == song.id } -> _trendingSongs.value
            _searchResults.value.any { it.id == song.id } -> _searchResults.value
            _currentPlaylistSongs.value.any { it.id == song.id } -> _currentPlaylistSongs.value
            else -> _localSongs.value
        }

        if (controller.currentMediaItem?.mediaId == song.id.toString()) {
            if (!controller.isPlaying) controller.play()
            return
        }

        // Build playlist of all songs in current list
        val mediaItems = _songs.value.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(s.path)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setArtworkUri(
                            if (s.albumArtUri != null) Uri.parse(s.albumArtUri) else null
                        )
                        .build()
                )
                .build()
        }

        val startIndex = _songs.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
