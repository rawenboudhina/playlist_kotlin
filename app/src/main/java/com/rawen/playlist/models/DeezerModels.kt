package com.rawen.playlist.models

import com.google.gson.annotations.SerializedName

data class DeezerSearchResponse(
    val data: List<DeezerTrack>,
    val total: Int? = null,
    val next: String? = null
)

data class DeezerChartResponse(
    val tracks: DeezerTrackList? = null
)

data class DeezerTrackList(
    val data: List<DeezerTrack>
)

data class DeezerTrack(
    val id: Long,
    val title: String,
    @SerializedName("title_short")
    val titleShort: String? = null,
    val duration: Int, // in seconds
    val preview: String, // 30-second preview URL
    val artist: DeezerArtist,
    val album: DeezerAlbum
)

data class DeezerArtist(
    val id: Long,
    val name: String,
    @SerializedName("picture_medium")
    val pictureMedium: String? = null,
    @SerializedName("picture_big")
    val pictureBig: String? = null
)

data class DeezerAlbum(
    val id: Long,
    val title: String,
    @SerializedName("cover_medium")
    val coverMedium: String? = null,
    @SerializedName("cover_big")
    val coverBig: String? = null,
    @SerializedName("cover_xl")
    val coverXl: String? = null
)

// Extension to convert DeezerTrack -> Song
fun DeezerTrack.toSong(): Song {
    return Song(
        id = this.id + 100000, // offset to avoid ID clashes with local songs
        title = this.title,
        artist = this.artist.name,
        duration = this.duration.toLong() * 1000, // convert seconds to ms
        path = this.preview,
        albumArtUri = this.album.coverBig ?: this.album.coverMedium,
        isLocal = false,
        deezerTrackId = this.id,
        albumName = this.album.title
    )
}
