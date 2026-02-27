package com.rawen.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rawen.playlist.models.DownloadedSong
import com.rawen.playlist.models.Playlist
import com.rawen.playlist.models.Song
import com.rawen.playlist.ui.theme.*

@Composable
fun LibraryScreen(
    localSongs: List<Song>,
    playlists: List<Playlist>,
    downloadedSongs: List<DownloadedSong>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPause: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onFolderClick: () -> Unit,
    onDeleteDownloadedSong: (Long) -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = SpotifyWhite
                    )
                }
            }
        }

        // Tabs — now with 3 tabs: Playlists, Local, Downloaded
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = SpotifyGreen,
            edgePadding = 16.dp,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Playlists",
                        color = if (selectedTab == 0) SpotifyGreen else SpotifyLightGrey
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Local Songs",
                        color = if (selectedTab == 1) SpotifyGreen else SpotifyLightGrey
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Downloaded",
                            color = if (selectedTab == 2) SpotifyGreen else SpotifyLightGrey
                        )
                        if (downloadedSongs.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SpotifyGreen
                            ) {
                                Text(
                                    text = "${downloadedSongs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpotifyBlack,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        when (selectedTab) {
            0 -> PlaylistsTab(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                onDeletePlaylist = onDeletePlaylist,
                onCreatePlaylist = { showCreateDialog = true }
            )
            1 -> LocalSongsTab(
                songs = localSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onSongClick = onSongClick,
                onPlayPause = onPlayPause,
                onDeleteSong = onDeleteSong
            )
            2 -> DownloadedSongsTab(
                downloadedSongs = downloadedSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onSongClick = onSongClick,
                onDeleteDownload = onDeleteDownloadedSong
            )
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist", color = SpotifyWhite) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotifyGreen,
                        cursorColor = SpotifyGreen,
                        focusedLabelColor = SpotifyGreen,
                        focusedTextColor = SpotifyWhite,
                        unfocusedTextColor = SpotifyWhite
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = SpotifyGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = SpotifyLightGrey)
                }
            },
            containerColor = SpotifySurface
        )
    }
}

// ============================================================
// NEW: Downloaded Songs Tab
// ============================================================
@Composable
fun DownloadedSongsTab(
    downloadedSongs: List<DownloadedSong>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onDeleteDownload: (Long) -> Unit
) {
    if (downloadedSongs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = SpotifyMediumGrey,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No downloaded songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = SpotifyWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Download songs from Home or Search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotifyLightGrey
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            item {
                Text(
                    text = "${downloadedSongs.size} downloaded tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyLightGrey,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(downloadedSongs) { downloaded ->
                val song = downloaded.toSong()
                DownloadedSongItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = isPlaying,
                    onClick = { onSongClick(song) },
                    onDelete = { onDeleteDownload(downloaded.deezerTrackId) }
                )
            }
        }
    }
}

@Composable
fun DownloadedSongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpotifySurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SpotifyLightGrey,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentSong) SpotifyGreen else SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyLightGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isCurrentSong && isPlaying) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing",
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Remove download",
                tint = SpotifyLightGrey
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Download?", color = SpotifyWhite) },
            text = {
                Text(
                    "Remove \"${song.title}\" from downloads?",
                    color = SpotifyLightGrey
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = SpotifyError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = SpotifyLightGrey)
                }
            },
            containerColor = SpotifySurface
        )
    }
}

// ============================================================
// EXISTING: Playlists Tab (unchanged)
// ============================================================
@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = SpotifyMediumGrey,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No playlists yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = SpotifyWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create a playlist to organize your music",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotifyLightGrey
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreatePlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Create Playlist", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onDelete = { onDeletePlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpotifySurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyLightGrey
            )
        }

        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = SpotifyLightGrey
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Playlist?", color = SpotifyWhite) },
            text = {
                Text(
                    "Are you sure you want to delete \"${playlist.name}\"?",
                    color = SpotifyLightGrey
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = SpotifyError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = SpotifyLightGrey)
                }
            },
            containerColor = SpotifySurface
        )
    }
}

// ============================================================
// EXISTING: Local Songs Tab (unchanged)
// ============================================================
@Composable
fun LocalSongsTab(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPause: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicOff,
                    contentDescription = null,
                    tint = SpotifyMediumGrey,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No local songs found",
                    style = MaterialTheme.typography.titleMedium,
                    color = SpotifyWhite
                )
                Text(
                    text = "Try selecting a different folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpotifyLightGrey
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            item {
                Text(
                    text = "${songs.size} songs on device",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpotifyLightGrey,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(songs) { song ->
                LocalSongItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = isPlaying,
                    onClick = { onSongClick(song) },
                    onPlayPause = { onPlayPause(song) },
                    onDelete = { onDeleteSong(song) }
                )
            }
        }
    }
}

@Composable
fun LocalSongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpotifySurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = com.rawen.playlist.utils.AudioModel(android.net.Uri.parse(song.path)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SpotifyLightGrey,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentSong) SpotifyGreen else SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyLightGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrentSong && isPlaying) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing",
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isCurrentSong) SpotifyGreen else SpotifyLightGrey
            )
        }

        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = SpotifyLightGrey
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song?", color = SpotifyWhite) },
            text = {
                Text(
                    "Delete \"${song.title}\" from your device?",
                    color = SpotifyLightGrey
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = SpotifyError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = SpotifyLightGrey)
                }
            },
            containerColor = SpotifySurface
        )
    }
}
