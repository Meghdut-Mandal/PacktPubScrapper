import com.google.gson.Gson
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
import models.BookPage
import models.Section
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

    private val objDump = mutableListOf<JsonObject>()
    private val gson = Gson()
    private val database = BooksDatabase()
    private val counter = AtomicInteger(0)


    private val BookInfo.contentType: String
        get() {
            return readUrl.split("/")[1]
        }

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

        val jsonObject:JsonObject = client.get(
            "${NetworkConfig.BOOKS_ENDPOINT}/${bookInfo.contentType}/${bookInfo.bookId}/${bookChapter.id.toInt()}/${section.id}",
            user.authHeader()
        ).body()

        println("Done loading  ${section.id}")

        counter.incrementAndGet()

        val data = jsonObject["data"].asJsonObject

        objDump.add(data)
        data["content"].asJsonObject.entrySet().forEach {
            if (isLoaded[it.key] != true) {
                isLoaded[it.key] = true
                val bookPage = BookPage(
                    it.key,
                    bookInfo.bookId,
                    it.value.asString,
                    bookChapter.id.toInt(),
                    bookChapter.title
                )
                println("${it.key} : ${section.title}")
                database.savePage(bookPage)
            }
        }

    }


    private suspend fun loadBookInfo(user: User, bookId: String): BookInfo {
        val jsonObject:JsonObject  = client.get("${NetworkConfig.API_BASE}/$bookId/summary", user.authHeader()).body()

        val data = jsonObject["data"].asJsonObject

        val chapters = data["toc"].asJsonObject["chapters"].asJsonArray.map {
            gson.fromJson(
                it,
                BookChapter::class.java
            )
        }
        val title = data["title"].asString
        val oneLiner = data["oneLiner"].asString
        val isbn10 = data["isbn10"]?.asString ?: "NA"
        val about = data["about"].asString
        val readUrl = data["readUrl"].asString
        val coverImage = data["coverImage"].asString
        val category = data["category"].asString
        val type = data["type"].asString
        val author =
            data["linkAuthors"]?.asJsonArray?.get(0)?.asJsonObject?.get("author")?.asString
                ?: ""
        val info = BookInfo(
            bookId,
            title,
            author,
            isbn10,
            oneLiner,
            about,
            coverImage,
            category,
            readUrl,
            type,
            chapters
        )
        database.saveBookInfo(info)
        return info
    }

    private val executor = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

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

            println("Requests made  ${counter.get()}")

            if(bookInfo.type!="videos"){
                println("Now converting to Epub")
                epubHandler.convertBook(bookid)
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
            delay(500)
        }
    }
}


