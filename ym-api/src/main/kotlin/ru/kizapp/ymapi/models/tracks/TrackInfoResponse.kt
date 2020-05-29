package ru.kizapp.ymp.api.models.tracks

import com.google.gson.Gson
import ru.kizapp.ymp.api.models.albums.AlbumInfoResponse
import ru.kizapp.ymp.api.models.artists.ArtistInfoResponse

data class TrackInfoResponse(
        val id: String?,
        val realId: String?,
        val title: String?,
        val available: Boolean?,
        val durationMs: Long,
        val storageDir: String,
        val fileSize: Long,
        val coverUri: String,
        val ogImage: String,
        val lyricsAvailable: Boolean,
        val type: String,
        val artists: List<ArtistInfoResponse>,
        val albums: List<AlbumInfoResponse>
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

fun TrackInfoResponse.toShort(): TrackShortResponse {
    val albumId = if (albums.isEmpty()) {
        ""
    } else {
        albums.first().id.toString()
    }
    return TrackShortResponse(id.orEmpty(), albumId, "")
}