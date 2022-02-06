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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object BookPages : Table("book_pages") {
    val id = varchar("id", 20)
    override val primaryKey = PrimaryKey(id) // name is optional here
    val bookId = varchar("book_id", 50)
    val pageContent = text("page_content")
    val chapterId = integer("chapter_id")
}

val client = HttpClient(OkHttp) {
    install(JsonPlugin) {
        serializer = GsonSerializer()
    }
}


fun main() = runBlocking {
    Database.connect("jdbc:sqlite:book_pages.db", driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.create(BookPages)
    }
    val user = User(System.getenv("user"), System.getenv("pass"))
    user.auth()
    println(user.token)
    val data = loadBookData(user, "9781800564909", 14, "ch14lvl1sec94")
    println(data)
    client.close()
}


object Config {
    // this is base url where i do the requests
    const val BASE_URL = "https://services.packtpub.com/"

    //#URL to request jwt token, params by post are user and pass, return jwt token
    const val AUTH_ENDPOINT = "auth-v1/users/tokens"

    // progamming language books urls
    const val BOOKS_ENDPOINT = "https://subscription.packtpub.com/api/product/content/programming"

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
        } else throw Exception("Error getting token ${response.status} ${response.bodyAsText()}")
    }
}


fun loadBookData(user: User, bookId: String, chapterId: Int, pageId: String): String = runBlocking {
    val response = client.get("${Config.BOOKS_ENDPOINT}/$bookId/$chapterId/$pageId") {
        header(HttpHeaders.Authorization, user.token)
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

    val jsonText = response.bodyAsText()
    // get data from json
    val jsonObject = Gson().fromJson(jsonText, JsonObject::class.java)
    val data = jsonObject["data"].asJsonObject
    data.entrySet().forEach {
        println("${it.key} : ${it.value}")
        saveRow(it.key, it.value.asString, bookId, chapterId)
    }

    return@runBlocking response.bodyAsText()
}

fun saveRow(pageId: String, content: String, bookId: String, chapterId: Int) {
    transaction {
        addLogger(StdOutSqlLogger)
        BookPages.insert {
            it[BookPages.bookId] = bookId
            it[BookPages.chapterId] = chapterId
            it[id] = pageId
            it[pageContent] = content
        }
    }
}

