package com.rawen.playlist.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import com.rawen.playlist.data.db.PlaylistDao
import com.rawen.playlist.models.DownloadedSong
import com.rawen.playlist.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

class MusicDownloadManager(private val context: Context) {

    fun downloadSong(song: Song): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val fileName = "${song.title} - ${song.artist}.mp3"
            .replace("[^a-zA-Z0-9.\\-_ ]".toRegex(), "_")

        val request = DownloadManager.Request(Uri.parse(song.path))
            .setTitle("Downloading ${song.title}")
            .setDescription("${song.artist} - ${song.albumName ?: "Unknown Album"}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Playlist/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)
    }

    /**
     * Downloads the song AND saves metadata to Room DB for tracking.
     * Returns the system DownloadManager download ID.
     */
    suspend fun downloadAndTrack(song: Song, dao: PlaylistDao): Long {
        val downloadId = downloadSong(song)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val filePath = getDownloadedFilePath(song)

        // Register a one‑shot receiver to capture this download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != downloadId) return

                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    downloadManager.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val deezerTrackId = song.deezerTrackId ?: song.id
                                    dao.insertDownloadedSong(
                                        DownloadedSong(
                                            deezerTrackId = deezerTrackId,
                                            title = song.title,
                                            artist = song.artist,
                                            albumName = song.albumName,
                                            albumArtUrl = song.albumArtUri,
                                            localFilePath = filePath,
                                            duration = song.duration
                                        )
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    // Ensure we do not leak the receiver
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return downloadId
    }

    fun getDownloadedFilePath(song: Song): String {
        val fileName = "${song.title} - ${song.artist}.mp3"
            .replace("[^a-zA-Z0-9.\\-_ ]".toRegex(), "_")
        return java.io.File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Playlist/$fileName"
        ).absolutePath
    }

    fun isDownloaded(song: Song): Boolean {
        if (song.isLocal) return true
        val fileName = "${song.title} - ${song.artist}.mp3"
            .replace("[^a-zA-Z0-9.\\-_ ]".toRegex(), "_")
        val file = java.io.File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Playlist/$fileName"
        )
        return file.exists()
    }
}
