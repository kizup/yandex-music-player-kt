package ru.kizapp.ymp.api.models.artists

import ru.kizapp.ymp.api.models.covers.CoverInfoResponse

class ArtistInfoResponse(
        val id: Long,
        val name: String,
        val cover: CoverInfoResponse
)