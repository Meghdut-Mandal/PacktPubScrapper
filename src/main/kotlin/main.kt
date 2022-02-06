import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*


val client = HttpClient(OkHttp) {
    install(JsonPlugin) {
        serializer = GsonSerializer()
    }
}
val database = BooksDatabase()


fun main() = runBlocking {
    val user = User(System.getenv("user"), System.getenv("pass"))
    user.auth()
    println(user.token)

    val bookid = "9781800564909"

    loadBookSummary(user, bookid)
    client.close()
}


object Config {
    // this is base url where i do the requests
    const val BASE_URL = "https://services.packtpub.com/"

    //#URL to request jwt token, params by post are user and pass, return jwt token
    const val AUTH_ENDPOINT = "auth-v1/users/tokens"

    // progamming language books urls
    const val BOOKS_ENDPOINT = "https://subscription.packtpub.com/api/product/content/programming"

    const val API_BASE = "https://subscription.packtpub.com/api/products/"
}


val isLoaded = mutableMapOf<String, Boolean>()


suspend fun loadBookData(user: User, bookId: String, chapterId: Int, pageId: String): String  {
    val response = client.get(
        "${Config.BOOKS_ENDPOINT}/$bookId/$chapterId/$pageId",
        user.authHeader()
    )

    val jsonText = response.bodyAsText()
    // get data from json
    val jsonObject = Gson().fromJson(jsonText, JsonObject::class.java)
    val data = jsonObject["data"].asJsonObject
    data.entrySet().forEach {
        println("${it.key} : ${it.value}")

        if (isLoaded[it.key] != true)
            database.saveRow(it.key, it.value.asString, bookId, chapterId) // save data to db
    }

    return response.bodyAsText()
}


suspend fun loadBookSummary(user: User, bookId: String) {
    val response = client.get("${Config.API_BASE}/$bookId/summary", user.authHeader())

    val jsonText = response.bodyAsText()
    // get data from json
    val jsonObject = Gson().fromJson(jsonText, JsonObject::class.java)
    val data = jsonObject["data"].asJsonObject
    data.entrySet().forEach {
        println("${it.key} : ${it.value}")
    }
}

