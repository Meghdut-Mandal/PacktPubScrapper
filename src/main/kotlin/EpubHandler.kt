import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class EpubHandler(private val client: HttpClient, private val database: BooksDatabase) {


    @OptIn(InternalAPI::class)
    fun convertBook(bookId: String) = runBlocking(Dispatchers.IO) {
        val bookid = "9781838821470"
        val bookPages = database.getBookPages(bookid)
        bookPages.forEach {
            sendChapter(it.title, it.pageContent)
        }

        val jsonBody = JsonObject()
        jsonBody.addProperty("title", "GraalVM Book")
        jsonBody.addProperty ("author", "GraalVM")
        val res = client.post("http://localhost:3000/make") {
            // json header
            contentType(ContentType.Application.Json)
            // add body
            body = jsonBody
        }
        println("  " + res.content)
        client.close()

    }

    @OptIn(InternalAPI::class)
    suspend fun sendChapter(title: String, data: String) {
        val jsonBody = JsonObject()
        jsonBody.addProperty("title", title)
        jsonBody.addProperty("data", data.replace("<?xml encoding=\"utf-8\" ?>", ""))
        val res = client.post("http://localhost:3000/add") {
            // json header
            contentType(ContentType.Application.Json)
            // add body
            body = jsonBody
        }
        println("sent chapter $title with status ${res.status}")
    }

}


