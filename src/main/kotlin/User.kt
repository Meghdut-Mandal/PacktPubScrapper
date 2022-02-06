import Scrapper.client
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

class User(val username: String, val password: String) {

    lateinit var token: String

    @OptIn(InternalAPI::class)
    suspend fun auth() {
        val response = client.post("${NetworkConfig.BASE_URL}${NetworkConfig.AUTH_ENDPOINT}") {
            contentType(ContentType.Application.Json)
            headers {
                append(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"
                )
                append(HttpHeaders.Authorization, "")
            }
            body = this@User
        }
        if (response.status == HttpStatusCode.OK) {
            val jsonText = response.bodyAsText()
            // parse json to get token
            val jsonObject = Gson().fromJson(jsonText, JsonObject::class.java)
            token = "Bearer " + jsonObject["data"].asJsonObject["access"].asString
        } else throw Exception("Error getting token ${response.status} ${response.bodyAsText()}")
    }

    fun authHeader(): HttpRequestBuilder.() -> Unit =
        {
            header(HttpHeaders.Authorization, token)
            header(
                "sec-ch-ua",
                "\" Not;A Brand\";v=\"99\", \"Google Chrome\";v=\"97\", \"Chromium\";v=\"97\""
            )
            header("dnt", "1")
            header("accept-language", "en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7")
            header("sec-ch-ua-mobile", "?0")
            header("accept", "application/json, text/plain, */*")
            header(
                "user-agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36"
            )
            header("sec-ch-ua-platform", "\"macOS\"")
            header("sec-fetch-site", "same-origin")
            header("sec-fetch-mode", "cors")
            header("sec-fetch-dest", "empty")
            header("referer", this.url.toString())
        }

}