import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.BookChapter
import models.BookInfo
import models.BookPage
import models.Section


object Scrapper {
    private val isLoaded = mutableMapOf<String, Boolean>()
    private val client = HttpClient(OkHttp) {
        install(JsonPlugin) {
            serializer = GsonSerializer()
        }
    }
    private val database = BooksDatabase()

    private val epubHandler = EpubHandler(client, database)

    private suspend fun loadPages(
        user: User,
        bookInfo: BookInfo,
        bookChapter: BookChapter,
        section: Section
    ) {
        if (isLoaded[section.id] == true) {
            println("Already loaded ${section.id}")
            return
        }

        val response = client.get(
            "${NetworkConfig.BOOKS_ENDPOINT}/${bookInfo.bookId}/${bookChapter.id.toInt()}/${section.id}",
            user.authHeader()
        )

        val jsonText = response.bodyAsText()
        // get data from json
        val jsonObject = Gson().fromJson(jsonText, JsonObject::class.java)
        val data = jsonObject["data"].asJsonObject
        data.entrySet().forEach {
            println("${it.key} : ${it.value}")

            if (isLoaded[it.key] != true) {
                val bookPage = BookPage(
                    it.key,
                    bookInfo.bookId,
                    it.value.asString,
                    bookChapter.id.toInt(),
                    bookChapter.title
                )
                database.savePage(bookPage)
                isLoaded[it.key] = true
            }
        }
    }


    private suspend fun loadBookInfo(user: User, bookId: String): BookInfo {
        val response = client.get("${NetworkConfig.API_BASE}/$bookId/summary", user.authHeader())

        val jsonText = response.bodyAsText()
        // get data from json
        val jsonObject = Gson().fromJson(jsonText, JsonObject::class.java)
        val data = jsonObject["data"].asJsonObject
        val gson = Gson()
        val chapters = data["toc"].asJsonObject["chapters"].asJsonArray.map {
            gson.fromJson(
                it,
                BookChapter::class.java
            )
        }
        val title = data["title"].asString
        val oneLiner = data["oneLiner"].asString
        val isbn10 = data["isbn10"].asString
        val about = data["about"].asString
        val coverImage = data["coverImage"].asString
        val category = data["category"].asString
        val author =
            data["linkAuthors"]?.asJsonArray?.get(0)?.asJsonObject?.get("author")?.asString ?: ""
        val info = BookInfo(
            bookId,
            title,
            author,
            isbn10,
            oneLiner,
            about,
            coverImage,
            category,
            chapters
        )
        database.saveBookInfo(info)
        return info
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking(Dispatchers.IO) {
        val bookid = System.getenv("bookid")

        val user = User(System.getenv("user"), System.getenv("pass"))
        user.auth(client)
        println(user.token)
        val bookInfo = loadBookInfo(user, bookid)

        bookInfo.bookChapters.map { chapter ->
            println("Starting loading ${chapter.title} ${chapter.id}")
            chapter.sections.map { section ->
                launch {
                    loadPages(user, bookInfo, chapter, section)
                    delay(500)
                }
            }.forEach {
                it.join()
            }
        }

        println("Now converting to Epub")

        epubHandler.convertBook(bookid)
        println("Done")
        client.close()
    }
}


