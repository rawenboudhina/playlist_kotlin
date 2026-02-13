
package com.rawen.playlist

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rawen.playlist.ui.HomeScreen
import com.rawen.playlist.ui.MusicViewModel
import com.rawen.playlist.ui.PlayerScreen
import com.rawen.playlist.ui.theme.PlaylistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaylistTheme {
                MainScreen()
            }
        }
    }
}

enum class Screen {
    Home, Player
}

@OptIn(ExperimentalPermissionsApi::class)

@Composable
fun MainScreen() {
    val viewModel: MusicViewModel = viewModel()
    val context = LocalContext.current

    // Initialize context for ViewModel to load prefs
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
        viewModel.initializeController(context)
        
        // Config Coil
        val imageLoader = coil.ImageLoader.Builder(context)
            .components {
                add(com.rawen.playlist.utils.AudioModelFetcher.Factory(context))
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)
    }

    // Permission Handling
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    // Notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notifPermissionState.status.isGranted) {
                notifPermissionState.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.loadSongs()
        }
    }

    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Home) }

    // Folder Picker
    val folderLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            viewModel.setCustomFolder(it.toString())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen == Screen.Home && currentSong != null) {
                com.rawen.playlist.ui.MiniPlayer(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    progress = progress.toFloat() / (currentSong!!.duration.coerceAtLeast(1L)),
                    onPlayPauseClick = {
                        if (isPlaying) viewModel.pause() else viewModel.resume()
                    },
                    onClick = {
                        currentScreen = Screen.Player
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.Home -> {
                HomeScreen(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(innerPadding),
                    onSongClick = { song ->
                        viewModel.playSong(song)
                    },
                    onPlayPause = { song ->
                        if (currentSong?.id == song.id) {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        } else {
                            viewModel.playSong(song)
                        }
                    },
                    onFolderClick = {
                        folderLauncher.launch(null)
                    },
                    onLoadRemote = viewModel::loadRemoteSongs
                )
            }
            Screen.Player -> {
                currentSong?.let { song ->
                    PlayerScreen(
                        modifier = Modifier.padding(innerPadding),
                        song = song,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPauseClick = {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        },
                        onNextClick = {
                            viewModel.skipToNext()
                        },
                        onPreviousClick = {
                            viewModel.skipToPrevious()
                        },
                        onSeek = { position ->
                            viewModel.seekTo(position)
                        }
                    )
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                } ?: run {
                    currentScreen = Screen.Home
                }
            }
        }
    }
}
