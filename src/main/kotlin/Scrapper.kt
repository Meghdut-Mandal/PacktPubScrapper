import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.gson.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import models.BookChapter
import models.BookInfo
import models.Section
import org.bson.Document
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.set


object Scrapper {
    private val isLoaded = mutableMapOf<String, Boolean>()
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            gson()
        }
    }

    private val database = BooksDatabase()
    private val counter = AtomicInteger(0)


    private fun loadContent(
        user: User,
        bookInfo: BookInfo,
        bookChapter: BookChapter,
        section: Section
    ) {
        when (section.contentType) {
            "text" -> loadBookPageContent(bookInfo, bookChapter, section, user)
            "video" -> loadVideoContent(user, bookInfo, bookChapter, section)
        }
    }

    private fun loadVideoContent(
        user: User,
        bookInfo: BookInfo,
        bookChapter: BookChapter,
        section: Section
    ) = runBlocking {
        val jsonObject:JsonObject = client.get(
            "${NetworkConfig.VIDEO_ENDPOINT}/${bookInfo.bookId}/${bookChapter.id}/${section.id}",
            user.authHeader()
        ).body()
        counter.incrementAndGet()


        val videoUrl = jsonObject["data"].asString

        // save the video to file
        val fileName =
            "store/${bookInfo.title.asFileName()}/${bookChapter.title.asFileName()}/${section.title.asFileName()}_${section.id}.mp4"
        val videoResponse = client.get(videoUrl, user.authHeader())
        // save the response to file
        val file = File(fileName)
        file.parentFile.mkdirs()

        file.writeChannel().use {
            videoResponse.bodyAsChannel().copyAndClose(this)
        }
        println("Saved video to $fileName")
        delay(500)
    }

    fun String.asFileName(): String = replace(" ", "_")

    private fun loadBookPageContent(
        bookInfo: BookInfo,
        bookChapter: BookChapter,
        section: Section,
        user: User
    ) = runBlocking {

        if (isLoaded[section.id] == true) {
            println("Cached ${section.id}")
            return@runBlocking
        }

        val jsonObject: JsonObject = client.get(
            "${NetworkConfig.BOOKS_ENDPOINT}/${bookInfo.contentType}/${bookInfo.bookId}/${bookChapter.id.toInt()}/${section.id}",
            user.authHeader()
        ).body()

        println("Done loading  ${section.id}")

        counter.incrementAndGet()

        val data = jsonObject["data"].asJsonObject

        data["content"].asJsonObject.entrySet().forEach {
            if (isLoaded[it.key] != true) {
                isLoaded[it.key] = true
                println("${it.key} : ${section.title}")
                database.savePage(bookInfo, bookChapter, it.key, it.value.asString)
            }
        }

        delay(500)
    }


    private suspend fun loadBookInfo(user: User, bookId: String): BookInfo {
        val jsonObject: Document =
            client.get("${NetworkConfig.API_BASE}/$bookId/summary", user.authHeader()).body()

        val bookInfo = jsonObject["data"] as Map<*, *>
        return database.saveBookInfo(bookInfo)
    }

    private val executor = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    @JvmStatic
    fun main(args: Array<String>) =
        runBlocking(executor) {
            val token = System.getenv("TOKEN") ?: ""
            val bookid =
                System.getenv("bookid") ?: throw Exception("No bookid found in the ENV variables!")
            val username = System.getenv("user") ?: ""
            val password = System.getenv("pass") ?: ""
            val epubHandlerUrl = System.getenv("epubhandler") ?: "epubhelper:3000"

            if (token.isEmpty() && (username.isEmpty() || password.isEmpty())) {
                throw Exception("Please provide valid credentials!")
            }

            val epubHandler = EpubHandler(client, database, epubHandlerUrl)
            val user = User(username, password)
            if (token.isNotEmpty()) {
                user.token = token
            } else {
                user.auth(client)
            }
            println(user.token) // print token
            val bookInfo = loadBookInfo(user, bookid)


            bookInfo.bookChapters.map { chapter ->
                println("Starting loading ${chapter.title} ${chapter.id}")
                launch(executor) {
                    loadChapterSequential(chapter, user, bookInfo)
                }
            }.joinAll()

            println("Requests made  ${counter.get()} with ${bookInfo}")

            if(bookInfo.contentType !="videos"){
                println("Now converting to Epub")
                epubHandler.convertBook(bookInfo.bookId)
                println("Done")
                client.close()
            }

        }

    private suspend fun loadChapterSequential(
        chapter: BookChapter,
        user: User,
        bookInfo: BookInfo
    ) {
        chapter.sections.forEach { section ->
            loadContent(user, bookInfo, chapter, section)
        }
    }
}


