import BooksDatabase.BookPagesTable.chapterId
import BooksDatabase.BookPagesTable.id
import BooksDatabase.BookPagesTable.pageId
import com.google.common.hash.Hashing
import models.BookInfo
import models.BookPage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


class BooksDatabase {

    init {
        Database.connect("jdbc:sqlite:book_pages.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(BookPagesTable, BookInfoTable)
        }
    }

    object BookPagesTable : Table("book_pages") {
        val pageId = varchar("pageId", 20)
        val id = varchar("id", 20)
        override val primaryKey = PrimaryKey(id) // name is optional here
        val bookId = varchar("book_id", 50)
        val pageContent = text("page_content")
        val chapterId = integer("chapter_id")
        val title = text("title")
    }

    object BookInfoTable : Table("book_info") {
        val bookId = varchar("id", 20)
        override val primaryKey = PrimaryKey(bookId) // name is optional here
        val title = varchar("title", 100)
        val isbn10 = varchar("isbn10", 20)
        val oneLiner = text("oneLiner")
        val about = text("about")
        val category = varchar("category", 50)
        val coverImage = text("coverImage")
        val author = varchar("author", 100)
    }

    fun savePage(bookPage: BookPage) {
        val hf = Hashing.sha256()
        val hash = hf.newHasher()
            .putInt(bookPage.chapterId)
            .putString(bookPage.bookId, Charsets.UTF_8)
            .putString(bookPage.pageContent, Charsets.UTF_8)
            .hash().asLong().toString(26)

        transaction {
            addLogger(StdOutSqlLogger)
            BookPagesTable.insertIgnore {
                it[id] = hash
                it[bookId] = bookPage.bookId
                it[chapterId] = bookPage.chapterId
                it[pageId] = bookPage.pageid
                it[pageContent] = bookPage.pageContent
                it[title] = bookPage.title
            }
        }
    }

    fun saveBookInfo(bookInfo: BookInfo) {
        transaction {
            addLogger(StdOutSqlLogger)
            BookInfoTable.insertIgnore {
                it[bookId] = bookInfo.bookId
                it[title] = bookInfo.title
                it[isbn10] = bookInfo.isbn10
                it[oneLiner] = bookInfo.oneLiner
                it[about] = bookInfo.about
                it[category] = bookInfo.category
                it[coverImage] = bookInfo.coverImage
                it[author] = bookInfo.author
            }
        }
    }

    // get book info
    fun getBookInfo(bookId: String): BookInfo? {
        var bookInfo: BookInfo? = null
        transaction {
            addLogger(StdOutSqlLogger)
            BookInfoTable.select { BookInfoTable.bookId eq bookId }.forEach {
                bookInfo = BookInfo(
                    it[BookInfoTable.bookId],
                    it[BookInfoTable.title],
                    it[BookInfoTable.isbn10],
                    it[BookInfoTable.oneLiner],
                    it[BookInfoTable.about],
                    it[BookInfoTable.category],
                    it[BookInfoTable.coverImage],
                    it[BookInfoTable.author]
                )
            }
        }
        return bookInfo
    }

    // read BookPages from database of a bookid
    fun getBookPages(bookId: String): List<BookPage> {
        val bookPages = mutableListOf<BookPage>()
        transaction {
            addLogger(StdOutSqlLogger)
            BookPagesTable.select { BookPagesTable.bookId eq bookId }.forEach {
                bookPages.add(
                    BookPage(
                        it[pageId],
                        it[BookPagesTable.bookId],
                        it[BookPagesTable.pageContent],
                        it[chapterId],
                        it[BookPagesTable.title]
                    )
                )
            }
        }
        return bookPages
    }
}