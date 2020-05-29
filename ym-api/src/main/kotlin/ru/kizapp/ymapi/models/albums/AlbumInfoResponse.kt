package ru.kizapp.ymp.api.models.albums

import ru.kizapp.ymp.api.models.artists.ArtistInfoResponse
import ru.kizapp.ymp.api.models.labels.LabelInfoResponse

data class AlbumInfoResponse(
        val id: Long,
        val title: String,
        val metaType: String,
        val contentWarning: String,
        val year: Long,
        val releaseDate: String,
        val coverUri: String,
        val ogImage: String,
        val genre: String,
        val trackCount: Long,
        val recent: Boolean,
        val veryImportant: Boolean,
        val artists: List<ArtistInfoResponse>,
        val labels: List<LabelInfoResponse>,
        val available: Boolean
)