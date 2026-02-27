package com.rawen.playlist

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rawen.playlist.models.Playlist
import com.rawen.playlist.ui.*
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
    Home, Search, Library, Player, PlaylistDetail
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val viewModel: MusicViewModel = viewModel()
    val context = LocalContext.current

    // Initialize
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
        viewModel.initializeController(context)

        val imageLoader = coil.ImageLoader.Builder(context)
            .components {
                add(com.rawen.playlist.utils.AudioModelFetcher.Factory(context))
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)
    }

    // Permissions
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermissionState =
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
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

    // State
    val localSongs by viewModel.localSongs.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playlistSongs by viewModel.currentPlaylistSongs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedNavItem by remember { mutableStateOf(BottomNavItem.Home) }
    var currentPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var songForPlaylistPicker by remember { mutableStateOf<com.rawen.playlist.models.Song?>(null) }

    // Folder picker
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
        containerColor = com.rawen.playlist.ui.theme.SpotifyBlack,
        bottomBar = {
            if (currentScreen != Screen.Player) {
                Column {
                    // MiniPlayer
                    if (currentSong != null) {
                        MiniPlayer(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            progress = progress.toFloat() / (currentSong!!.duration.coerceAtLeast(1L)),
                            onPlayPauseClick = {
                                if (isPlaying) viewModel.pause() else viewModel.resume()
                            },
                            onClick = { currentScreen = Screen.Player },
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    // Bottom Nav
                    BottomNavBar(
                        selectedItem = selectedNavItem,
                        onItemSelected = { item ->
                            selectedNavItem = item
                            currentScreen = when (item) {
                                BottomNavItem.Home -> Screen.Home
                                BottomNavItem.Search -> Screen.Search
                                BottomNavItem.Library -> Screen.Library
                            }
                        },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.Home -> {
                HomeScreen(
                    trendingSongs = trendingSongs,
                    localSongs = localSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isLoadingTrending = isLoadingTrending,
                    downloadedTrackIds = downloadedTrackIds,
                    onSongClick = { song -> viewModel.playSong(song) },
                    onPlayPause = { song ->
                        if (currentSong?.id == song.id) {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        } else {
                            viewModel.playSong(song)
                        }
                    },
                    onDownload = { song -> viewModel.downloadSong(song) },
                    onAddToPlaylist = { song -> songForPlaylistPicker = song },
                    onSeeAllLocalSongs = {
                        selectedNavItem = BottomNavItem.Library
                        currentScreen = Screen.Library
                    },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }

            Screen.Search -> {
                SearchScreen(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    downloadedTrackIds = downloadedTrackIds,
                    onQueryChange = { viewModel.searchDeezer(it) },
                    onSongClick = { song -> viewModel.playSong(song) },
                    onDownload = { song -> viewModel.downloadSong(song) },
                    onAddToPlaylist = { song -> songForPlaylistPicker = song },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }

            Screen.Library -> {
                LibraryScreen(
                    localSongs = localSongs,
                    playlists = playlists,
                    downloadedSongs = downloadedSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = { song -> viewModel.playSong(song) },
                    onPlayPause = { song ->
                        if (currentSong?.id == song.id) {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        } else {
                            viewModel.playSong(song)
                        }
                    },
                    onDeleteSong = { song -> viewModel.deleteSongFromDevice(song) },
                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                    onDeletePlaylist = { playlist -> viewModel.deletePlaylist(playlist) },
                    onPlaylistClick = { playlist ->
                        currentPlaylist = playlist
                        viewModel.loadPlaylistSongs(playlist.id)
                        currentScreen = Screen.PlaylistDetail
                    },
                    onFolderClick = { folderLauncher.launch(null) },
                    onDeleteDownloadedSong = { deezerTrackId ->
                        viewModel.deleteDownloadedSong(deezerTrackId)
                    },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }

            Screen.Player -> {
                currentSong?.let { song ->
                    PlayerScreen(
                        song = song,
                        isPlaying = isPlaying,
                        progress = progress,
                        playlists = playlists,
                        onPlayPauseClick = {
                            if (isPlaying) viewModel.pause() else viewModel.resume()
                        },
                        onNextClick = { viewModel.skipToNext() },
                        onPreviousClick = { viewModel.skipToPrevious() },
                        onSeek = { position -> viewModel.seekTo(position) },
                        onDownload = { viewModel.downloadSong(song) },
                        onAddToPlaylist = { playlistId ->
                            viewModel.addSongToPlaylist(playlistId, song)
                        },
                        onBack = { currentScreen = Screen.Home },
                        modifier = Modifier.navigationBarsPadding()
                    )
                    BackHandler {
                        currentScreen = when (selectedNavItem) {
                            BottomNavItem.Home -> Screen.Home
                            BottomNavItem.Search -> Screen.Search
                            BottomNavItem.Library -> Screen.Library
                        }
                    }
                } ?: run {
                    currentScreen = Screen.Home
                }
            }

            Screen.PlaylistDetail -> {
                currentPlaylist?.let { playlist ->
                    PlaylistDetailScreen(
                        playlistName = playlist.name,
                        songs = playlistSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song -> viewModel.playSong(song) },
                        onRemoveSong = { songId ->
                            viewModel.removeSongFromPlaylist(playlist.id, songId)
                        },
                        onBack = {
                            currentScreen = Screen.Library
                            selectedNavItem = BottomNavItem.Library
                        },
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                    BackHandler {
                        currentScreen = Screen.Library
                        selectedNavItem = BottomNavItem.Library
                    }
                } ?: run {
                    currentScreen = Screen.Library
                }
            }
        }
    }

    // Playlist picker dialog (for adding songs from search/home)
    songForPlaylistPicker?.let { song ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { songForPlaylistPicker = null },
            title = {
                androidx.compose.material3.Text(
                    "Add to Playlist",
                    color = com.rawen.playlist.ui.theme.SpotifyWhite
                )
            },
            text = {
                if (playlists.isEmpty()) {
                    androidx.compose.material3.Text(
                        "No playlists yet. Create one in Your Library first!",
                        color = com.rawen.playlist.ui.theme.SpotifyLightGrey
                    )
                } else {
                    androidx.compose.foundation.layout.Column {
                        playlists.forEach { playlist ->
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    viewModel.addSongToPlaylist(playlist.id, song)
                                    songForPlaylistPicker = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = com.rawen.playlist.ui.theme.SpotifyGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.width(12.dp)
                                    )
                                    androidx.compose.material3.Text(
                                        text = playlist.name,
                                        color = com.rawen.playlist.ui.theme.SpotifyWhite,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { songForPlaylistPicker = null }
                ) {
                    androidx.compose.material3.Text(
                        "Cancel",
                        color = com.rawen.playlist.ui.theme.SpotifyLightGrey
                    )
                }
            },
            containerColor = com.rawen.playlist.ui.theme.SpotifySurface
        )
    }
}
