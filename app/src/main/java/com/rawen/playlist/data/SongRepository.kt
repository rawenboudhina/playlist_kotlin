package com.rawen.playlist.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.rawen.playlist.models.Song

object SongRepository {

    fun getSongs(context: Context, folderUri: String? = null): List<Song> {
        val folderName = if (folderUri != null) {
            // Try to extract a folder name from the URI (e.g., from OpenDocumentTree)
            val uri = Uri.parse(folderUri)
            val path = uri.path ?: ""
            if (path.contains(":")) {
                path.substringAfterLast(":")
            } else {
                uri.lastPathSegment ?: "Download"
            }
        } else {
            "Download"
        }
        return getSongsFromMediaStore(context, folderName)
    }

    private fun getSongsFromMediaStore(context: Context, folderName: String?): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Filter for music only
        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = mutableListOf<String>()

        // Add filter for folder if provided
        if (!folderName.isNullOrEmpty()) {
            // DATA is deprecated but still works for filtering in many cases. 
            // For better compatibility on Android 10+, RELATIVE_PATH could be used.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                selection += " AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                selectionArgs.add("%$folderName%")
            } else {
                selection += " AND ${MediaStore.Audio.Media.DATA} LIKE ?"
                selectionArgs.add("%/$folderName/%")
            }
        }
        
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            val sArtworkUri = Uri.parse("content://media/external/audio/albumart")

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()
                val contentUri = ContentUris.withAppendedId(collection, id).toString()

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        path = contentUri, 
                        albumArtUri = albumArtUri
                    )
                )
            }
        }
        return songs
    }

    fun getRemoteSongs(): List<Song> {
        return listOf(
            Song(
                id = 1001,
                title = "Al-Fatiha",
                artist = "Mishary Alafasy",
                duration = 50000,
                path = "https://server8.mp3quran.net/afs/001.mp3",
                albumArtUri = "https://i1.sndcdn.com/artworks-000236666873-u32v67-t500x500.jpg"
            ),
            Song(
                id = 1002,
                title = "SoundHelix Song 1",
                artist = "SoundHelix",
                duration = 372000,
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                albumArtUri = null
            ),
            Song(
                id = 1003,
                title = "SoundHelix Song 2",
                artist = "SoundHelix",
                duration = 430000,
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                albumArtUri = null
            )
        )
    }
}
