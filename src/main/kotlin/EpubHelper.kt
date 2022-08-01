import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import models.BookChapter
import models.BookInfo
import net.seeseekey.epubwriter.model.EpubBook
import net.seeseekey.epubwriter.model.TocLink
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset


class EpubHelper(
    private val client: HttpClient,
    private val database: BooksDatabase,
    private val cssFilePath: String = "epubhelper/src/app.css"
) {


    suspend fun convertBook(bookId: Long,epubFilePath: String) = runBlocking(Dispatchers.IO) {
        val bookPages = database.getPagesByBookId(bookId)
        val pageMap = bookPages.associate { it._id.sectionId to it.content }
        val bookInfo = database.getBookInfoById(bookId) ?: throw Exception("Book not found")

        val book =
            EpubBook("en", bookId.toString(), bookInfo.title, bookInfo.author.joinToString(","))

        val coverImageBytes = getCoverImageBytes(bookInfo)
        book.addCoverImage(coverImageBytes, "image/jpeg", "images/cover.jpg")

        // Create toc
        book.isAutoToc = false
        book.tocLinks = bookInfo.bookChapters.map { chapter ->
            val tocChapter = TocLink("chapter-${chapter.id}.xhtml", chapter.title, null)
            tocChapter.tocChildLinks = tocLinks(chapter, pageMap, book)
            return@map tocChapter
        }

        addCss(book)
//        delete the file
        File(epubFilePath).delete()
        File(epubFilePath).outputStream().use {
            book.writeToStream(it)
        }
    }

    private fun addCss(book: EpubBook) {
        val cssFile = File(cssFilePath)
        cssFile.readBytes()
        book.addContent(
            cssFile.readBytes(),
            "text/css",
            "css/style.css",
            false,
            false
        )
    }

    private suspend fun getCoverImageBytes(bookInfo: BookInfo): ByteArray? {
        val coverImage = bookInfo.coverImage!!
        val response = client.get(coverImage)
        val byteArOutPutStream = ByteArrayOutputStream()
        response.readBytes().inputStream().use {
            it.copyTo(byteArOutPutStream)
        }
        val coverImageBytes = byteArOutPutStream.toByteArray()
        return coverImageBytes
    }

    private fun tocLinks(
        chapter: BookChapter,
        pageMap: Map<String?, String>,
        book: EpubBook
    ) = chapter.sections.map { section ->
        val sectionHref = "section-${section.id}.xhtml"
        val tocSection = TocLink(sectionHref, section.title, null)
        val content = pageMap[section.id]?.replace("<?xml encoding=\"utf-8\" ?>", "") ?: ""

        return@map if (content.isNotEmpty()) {
            val htmlContent = generateContent(section.title, content)

            book.addContent(
                htmlContent.toByteArray(Charset.forName("UTF-8")),
                "application/xhtml+xml",
                sectionHref,
                true,
                true
            )
            tocSection
        } else {
            null
        }
    }.filterNotNullTo(arrayListOf())


    private fun generateContent(title: String, data: String) = """
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">
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
