package com.rawen.playlist.ui

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.rawen.playlist.data.SongRepository
import com.rawen.playlist.models.Song
import com.rawen.playlist.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _customFolderUri = MutableStateFlow<String?>(null)
    val customFolderUri: StateFlow<String?> = _customFolderUri.asStateFlow()

    private var appContext: Context? = null

    fun setContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        loadPreferences()
        loadSongs()
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

    fun loadSongs() {
        val context = appContext ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val loadedSongs = SongRepository.getSongs(context, _customFolderUri.value)
            _songs.value = loadedSongs
        }
    }

    fun loadRemoteSongs() {
        viewModelScope.launch {
            val remoteSongs = SongRepository.getRemoteSongs()
            _songs.value = remoteSongs
        }
    }
    
    init {
        // Songs will be loaded when context is set or permission granted
    }

    @OptIn(UnstableApi::class)
    fun initializeController(context: Context) {
        if (mediaController != null) return

        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
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

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            // Can update duration or other states here
                        }
                    }
                })
                // Restore state if already playing
                updateCurrentSong(controller?.currentMediaItem)
                _isPlaying.value = controller?.isPlaying == true
                
                // Start progress update loop
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
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId
        if (mediaId != null) {
            val song = _songs.value.find { it.id.toString() == mediaId }
            _currentSong.value = song
        }
    }

    fun playSong(song: Song) {
        val controller = mediaController ?: return
        
        // Check if it's already the current song
        if (controller.currentMediaItem?.mediaId == song.id.toString()) {
            if (!controller.isPlaying) controller.play()
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.path)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(if (song.albumArtUri != null) android.net.Uri.parse(song.albumArtUri) else null)
                    .build()
            )
            .build()
        
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun stop() {
        mediaController?.stop()
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
