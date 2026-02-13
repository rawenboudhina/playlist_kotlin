package com.rawen.playlist.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import android.graphics.drawable.BitmapDrawable

class AudioThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(
                    uri,
                    Size(500, 500),
                    null
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        } ?: loadLegacyThumbnail(context, uri)

        return bitmap?.let {
            DrawableResult(
                drawable = BitmapDrawable(context.resources, it),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }
    }

    private fun loadLegacyThumbnail(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val embedPic = retriever.embeddedPicture
            if (embedPic != null) {
                BitmapFactory.decodeByteArray(embedPic, 0, embedPic.size)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle content URIs that are likely audio (or just try all content URIs if strict checks are hard without MIME type)
            // We can check scheme.
            if (data.scheme == ContentResolver.SCHEME_CONTENT) {
                // potentially check mime type if available, but for now specific usage in app is safe
                // or we rely on the caller passing only audio URIs.
                // To be safe, we could check if it is a media URI.
                // For this app, we will assume we pass audio URIs to AsyncImage when we want this header.
                // BUT: Coil's default fetcher also handles content URIs (for images).
                // We shouldn't override it for everything. 
                // Strategy: We will strip the fetcher here and just simple load function?
                // No, a specific Model type is better.
                return null 
            }
            return null
        }
    }
}

// Better approach to avoid conflict with default ContentUriFetcher:
// Use a wrapper class for the model.

data class AudioModel(val uri: Uri)

class AudioModelFetcher(
    private val context: Context,
    private val data: AudioModel
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        // Reuse the logic
        val fetcher = AudioThumbnailFetcher(context, data.uri)
        return fetcher.fetch()
    }

    class Factory(private val context: Context) : Fetcher.Factory<AudioModel> {
        override fun create(data: AudioModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return AudioModelFetcher(context, data)
        }
    }
}
