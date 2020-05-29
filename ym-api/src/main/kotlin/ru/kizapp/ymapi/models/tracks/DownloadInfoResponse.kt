package ru.kizapp.ymp.api.models.tracks

data class DownloadInfoResponse(
        val codec: String,
        val bitrateInKbps: Int,
        val gain: Boolean,
        val preview: Boolean,
        val downloadInfoUrl: String,
        val direct: Boolean
)

fun DownloadInfoResponse.isMp3(): Boolean {
    return codec == "mp3"
}