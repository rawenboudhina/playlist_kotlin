package com.rawen.playlist.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.rawen.playlist.models.Playlist
import com.rawen.playlist.models.Song
import com.rawen.playlist.ui.theme.*

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Long,
    playlists: List<Playlist>,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onDownload: () -> Unit = {},
    onAddToPlaylist: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }

    // Dynamic color extraction from album art
    var dominantColor by remember { mutableStateOf(SpotifySurfaceVariant) }
    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "dominantColor"
    )

    // Load album art and extract palette
    val context = LocalContext.current
    val imageModel = if (song.isLocal && song.albumArtUri != null) {
        com.rawen.playlist.utils.AudioModel(android.net.Uri.parse(song.path))
    } else {
        song.albumArtUri
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .build()
    )

    // Extract dominant color when image loads
    LaunchedEffect(painter.state) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Success) {
            val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    val swatch = palette?.darkMutedSwatch
                        ?: palette?.mutedSwatch
                        ?: palette?.dominantSwatch
                    swatch?.rgb?.let { rgb ->
                        dominantColor = Color(rgb)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedDominantColor,
                        animatedDominantColor.copy(alpha = 0.6f),
                        SpotifyBlack,
                        SpotifyBlack
                    )
                )
            )
    ) {
        // Blurred background album art for glassmorphism effect
        if (song.albumArtUri != null) {
            AsyncImage(
                model = if (song.isLocal) {
                    com.rawen.playlist.utils.AudioModel(android.net.Uri.parse(song.path))
                } else {
                    song.albumArtUri
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .blur(80.dp),
                alpha = 0.3f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpotifyLightGrey
                    )
                    Text(
                        text = song.albumName ?: (if (song.isLocal) "Local Music" else "Deezer"),
                        style = MaterialTheme.typography.labelMedium,
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { showPlaylistPicker = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = SpotifyWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Album Art with glassmorphism card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Glassmorphism card background
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    shadowElevation = 24.dp,
                    tonalElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpotifySurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = SpotifyMediumGrey
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(32.dp))

            // Song Info + Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpotifyLightGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action buttons
                if (!song.isLocal) {
                    IconButton(onClick = onDownload) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = SpotifyLightGrey
                        )
                    }
                }

                IconButton(onClick = { showPlaylistPicker = true }) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = SpotifyLightGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Slider
            Slider(
                value = progress.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..song.duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = SpotifyWhite,
                    activeTrackColor = SpotifyWhite,
                    inactiveTrackColor = SpotifyMediumGrey
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(progress),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyLightGrey
                )
                Text(
                    text = formatTime(song.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyLightGrey
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousClick, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Play/Pause Button
                FloatingActionButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(64.dp),
                    containerColor = SpotifyWhite,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = SpotifyBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onNextClick, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Playlist picker dialog
    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Add to Playlist", color = SpotifyWhite) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Create one first!", color = SpotifyLightGrey)
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist(playlist.id)
                                    showPlaylistPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.name,
                                        color = SpotifyWhite,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text("Cancel", color = SpotifyLightGrey)
                }
            },
            containerColor = SpotifySurface
        )
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
