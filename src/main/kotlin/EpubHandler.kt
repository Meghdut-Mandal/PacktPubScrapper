import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.MongoOperator

class EpubHandler(private val client: HttpClient, private val database: BooksDatabase, private val ephubHandlerUrl: String) {


    fun convertBook(bookId: Long) = runBlocking(Dispatchers.IO) {

        val bookPages = database.getPagesByBookId(bookId)
        val pageMap = bookPages.associate { it._id.sectionId to it.content }
        val bookInfo = database.getBookInfoById(bookId)?: throw Exception("Book not found")

        bookInfo.bookChapters.sortedBy { it.id }.forEach {chapter->
            chapter.sections.sortedBy { it.id }.forEach { section ->
                val page = pageMap[section.id] ?: throw Exception("Page Content not found")
                sendChapter(section.title, page)
            }
        }



        val jsonBody = JsonObject()
        jsonBody.addProperty("title", bookInfo.title)
        jsonBody.addProperty ("author", bookInfo.author)
        jsonBody.addProperty("cover", bookInfo.coverImage)
        val res = client.post("http://$ephubHandlerUrl/make") {
            // json header
            contentType(ContentType.Application.Json)
            // add body
            setBody(jsonBody)
        }
        client.close()

    }

    private suspend fun sendChapter(title: String, data: String) {
        val jsonBody = JsonObject()
        jsonBody.addProperty("title", title)
        jsonBody.addProperty("data", data.replace("<?xml encoding=\"utf-8\" ?>", ""))
        val res = client.post("http://$ephubHandlerUrl/add") {
            // json header
            contentType(ContentType.Application.Json)
            // add body
            setBody(jsonBody)
        }.bodyAsText()
        println("sent chapter $title with status ${res}")
    }

}


