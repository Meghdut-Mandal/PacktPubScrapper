import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class BooksDatabase {

    init {
        Database.connect("jdbc:sqlite:book_pages.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(BookPages)
        }
    }

    object BookPages : Table("book_pages") {
        val id = varchar("id", 20)
        override val primaryKey = PrimaryKey(id) // name is optional here
        val bookId = varchar("book_id", 50)
        val pageContent = text("page_content")
        val chapterId = integer("chapter_id")
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
            isLoaded[pageId] = true
        }
    }
}