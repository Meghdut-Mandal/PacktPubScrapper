import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentDisposition.Parameters.FileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.seeseekey.epubwriter.model.EpubBook
import net.seeseekey.epubwriter.model.TocLink
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.File
import kotlin.text.Typography.section


class EpubHandler(
    private val client: HttpClient,
    private val database: BooksDatabase,
    private val ephubHandlerUrl: String
) {


    suspend fun convertBook(bookId: Long) = runBlocking(Dispatchers.IO) {
        val bookPages = database.getPagesByBookId(bookId)
        val pageMap = bookPages.associate { it._id.sectionId to it.content }
        val bookInfo = database.getBookInfoById(bookId) ?: throw Exception("Book not found")

        val book =
            EpubBook("en", bookId.toString(), bookInfo.title, bookInfo.author.joinToString(","))

        val coverImage = bookInfo.coverImage!!
//        read image
        val response = client.get(coverImage)
        val byteArOutPutStream = ByteArrayOutputStream()
        response.readBytes().inputStream().use {
            it.copyTo(byteArOutPutStream)
        }
        book.addCoverImage(byteArOutPutStream.toByteArray(), "image/jpeg", "images/cover.jpg")

        book.isAutoToc = false
        // Create toc
        book.tocLinks = bookInfo.bookChapters.map { chapter ->
            val tocChapter = TocLink("chapter-${chapter.id}.xhtml", chapter.title, null)
            tocChapter.tocChildLinks = chapter.sections.map { section ->
                val sectionHref = "section-${section.id}.xhtml"
                val tocSection = TocLink(sectionHref, section.title, null)
                val content = pageMap[section.id]?.replace("<?xml encoding=\"utf-8\" ?>", "") ?: ""
                val htmlContent = generateContent(section.title, content)

                book.addContent(
                    htmlContent.toByteArray(),
                    "application/xhtml+xml",
                    sectionHref,
                    true,
                    true
                )
                tocSection
            }.toList()
            tocChapter
        }
        val cssFile = File("epubhelper/src/app.css")
        cssFile.readBytes()
        book.addContent(
            cssFile.readBytes(),
            "text/css",
            "css/style.css",
            false,
            false
        )
        File("test.epub").delete();
        book.writeToFile("test.epub")
    }


    private fun generateContent(title: String, data: String) = """
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head>
<meta charset="utf-8" />
<title>$title</title>
<link rel="stylesheet" href="css/style.css" type="text/css" />
</head>
<body>
$data
</body>
</html>
""".trimIndent()


}
