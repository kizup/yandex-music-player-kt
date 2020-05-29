package ru.kizapp.ymapi

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.Parameters
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.protocol.HttpContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import ru.kizapp.ymp.api.models.YandexMusicResponse
import ru.kizapp.ymp.api.models.tracks.*
import ru.kizapp.ymp.api.models.user.AccountResponse
import ru.kizapp.ymp.api.models.user.AccountResultResponse
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

class YMApiClient {

    companion object {
        const val USER_AGENT = "Yandex-Music-API"
        val HEADERS = mapOf(
                "X-Yandex-Music-Client" to "YandexMusicAndroid/23020055",
                "User-Agent" to USER_AGENT
        )
        val BASE_URL = "https://api.music.yandex.net"
        val OAUTH_URL = "https://oauth.yandex.ru"
    }

    object Builder {

        fun withToken(token: String): YMApiClient = YMApiClient(token)
        fun withCredentials(login: String, password: String): YMApiClient = YMApiClient(login, password)

    }

    private var token: String? = null
    private var userName: String? = null
    private var password: String? = null

    private var accountStatus: AccountResponse? = null

    lateinit var httpClient: HttpClient

    private constructor(token: String) {
        assert(token.isNotEmpty()) {
            "Token must not be empty"
        }
        this.token = token
        initClient()
    }

    private constructor(userName: String, password: String) {
        assert(userName.isNotEmpty()) {
            "Username must not be empty"
        }
        assert(password.isNotEmpty()) {
            "password must not be empty"
        }
        this.userName = userName
        this.password = password
        initClient()
    }


    private fun initClient() {
        httpClient = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
            engine {
                customizeClient {
                    addInterceptorFirst { request: HttpRequest?, context: HttpContext? ->
                        request ?: return@addInterceptorFirst
                        HEADERS.forEach { (k, v) -> request.addHeader(k, v) }
                        request.addHeader("Authorization", "OAuth $token")
                    }
                    addInterceptorLast { response: HttpResponse?, context: HttpContext? ->
                        println()
                    }
                }
            }
        }
    }

    suspend fun fetchAccountStatus(): AccountResponse {
        val url = "$BASE_URL/account/status"
        val status: AccountResponse = get<YandexMusicResponse<AccountResultResponse>>(url).result.account
        this.accountStatus = status
        return status
    }

    suspend fun userLikesTracks(): List<TrackShortResponse> {
        val response = getLikes<YandexMusicResponse<TracksResultResponse>>("track")
        return response.result.library.tracks
    }

    suspend inline fun <reified T> getLikes(objectType: String): T {
        val url = "$BASE_URL/users/${userId()}/likes/${objectType}s"
        val params = mapOf(
                "if-modified-since-revision" to "0"
        )
        return get(url, params)
    }

    suspend fun getTrackInfo(short: TrackShortResponse): List<TrackInfoResponse> {
        return getTracksInfo(listOf(short))
    }

    suspend fun getTracksInfo(tracks: List<TrackShortResponse>): List<TrackInfoResponse> {
        return getList("track", tracks.map { it.trackId() })
    }

    suspend fun getTrackDownloadInfo(track: TrackShortResponse): List<DownloadInfoResponse> {
        val url = "$BASE_URL/tracks/${track.trackId()}/download-info"
        return get<YandexMusicResponse<List<DownloadInfoResponse>>>(url).result
    }

    suspend fun getList(objectType: String, trackIds: List<String>): List<TrackInfoResponse> {
        val params = HashMap<String, String>()
        params["${objectType}-ids"] = trackIds.joinToString(separator = ",")
        params["with-positions"] = true.toString()
        val url = "$BASE_URL/${objectType}s/"
        val response: YandexMusicResponse<List<TrackInfoResponse>> = post(url, params)
        return response.result
    }

    suspend fun getDirectLink(info: DownloadInfoResponse): String {
        val data = get<String>(info.downloadInfoUrl)
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val document = docBuilder.parse(InputSource(ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8))))
        val host = getTextNodeData(document.getElementsByTagName("host")).orEmpty()
        val path = getTextNodeData(document.getElementsByTagName("path")).orEmpty()
        val ts = getTextNodeData(document.getElementsByTagName("ts")).orEmpty()
        val s = getTextNodeData(document.getElementsByTagName("s")).orEmpty()

        val digest = MessageDigest.getInstance("MD5")
        val salt = "XGRlBW9FXlekgbPrRHuSiA"
        val subPath = path.substring(1)

        val toEncode = salt + subPath + s
        digest.update(toEncode.toByteArray(StandardCharsets.UTF_8))

        val buffer = StringBuffer()
        val byteArray = digest.digest()
        byteArray.forEach { byte ->
            buffer.append(
                    ((byte.toInt() and 0xff) + 0x100).toString(16)
                            .substring(1)
            )
        }

        val sign = buffer.toString()
        return "https://${host}/get-mp3/${sign}/${ts}${path}"
    }

    private fun getTextNodeData(elements: NodeList): String? {
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element: Element = node as Element
                val children = element.childNodes
                for (k in 0 until children.length) {
                    val child = children.item(k)
                    if (child.nodeType == Node.TEXT_NODE) {
                        return child.textContent
                    }
                }
            }
        }
        return null
    }

    fun userId(): Long {
        return accountStatus?.uid ?: throw IllegalStateException("You must authorize for execute methods")
    }

    suspend inline fun <reified T> get(url: String, additionalQueries: Map<String, String>? = null): T {
        return httpClient.get(url) {
            val queries = additionalQueries ?: return@get
            queries.forEach { entry ->
                this.url.parameters.append(entry.key, entry.value)
            }
        }
    }

    suspend inline fun <reified T> post(url: String, params: Map<String, String>? = null): T {
        return httpClient.post(url) {
            params?.let {
                val parameters: Parameters = Parameters.build {
                    params.forEach { entry ->
                        append(entry.key, entry.value)
                    }
                }
                val data = FormDataContent(parameters)
                this.body = data
            }
        }
    }

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)

}