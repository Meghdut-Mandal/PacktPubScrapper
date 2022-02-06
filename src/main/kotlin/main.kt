import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking

val client = HttpClient(OkHttp) {
    install(JsonPlugin) {
        serializer = GsonSerializer()
    }
}


fun main() = runBlocking {
    val user = User(System.getenv("user"), System.getenv("pass"))
    println(user.auth())
    client.close()
}


object Config {
    // this is base url where i do the requests
    const val BASE_URL = "https://services.packtpub.com/"

    //#URL to request jwt token, params by post are user and pass, return jwt token
    const val AUTH_ENDPOINT = "auth-v1/users/tokens"

}


class User(val username: String, val password: String) {

    lateinit var token: String

    @OptIn(InternalAPI::class)
    suspend fun auth() {
        val response = client.post("${Config.BASE_URL}${Config.AUTH_ENDPOINT}") {
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
        }

        throw Exception("Error getting token ${response.status} ${response.bodyAsText()}")
    }

}