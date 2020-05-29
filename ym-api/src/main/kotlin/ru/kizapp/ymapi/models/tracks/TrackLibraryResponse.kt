package ru.kizapp.ymp.api.models.tracks

data class TrackLibraryResponse(
        val uid: Long,
        val revision: Long,
        val tracks: List<TrackShortResponse>
)