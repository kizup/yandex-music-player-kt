package ru.kizapp.ymp.api.models.tracks

data class TrackShortResponse(
        val id: String,
        val albumId: String,
        val timestamp: String
)

fun TrackShortResponse.trackId(): String {
    return if (albumId.isNotBlank()) {
        String.format("%s:%s", id, albumId)
    } else {
        id
    }
}