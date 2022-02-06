import com.google.common.annotations.Beta
import com.google.common.hash.Hashing
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
        val pageId = varchar("pageId", 20)
        val id = varchar("id", 20)
        override val primaryKey = PrimaryKey(id) // name is optional here
        val bookId = varchar("book_id", 50)
        val pageContent = text("page_content")
        val chapterId = integer("chapter_id")
    }

    fun saveRow(pageId: String, content: String, bookId: String, chapterId: Int) {
        val hf = Hashing.sha256()
        val hash = hf.newHasher()
            .putInt(chapterId)
            .putString(bookId, Charsets.UTF_8)
            .putString(content, Charsets.UTF_8)
            .hash().asLong().toString(26)

        transaction {
            addLogger(StdOutSqlLogger)
            BookPages.insertIgnore {
                it[id] = hash
                it[BookPages.bookId] = bookId
                it[BookPages.chapterId] = chapterId
                it[this.pageId] = pageId
                it[pageContent] = content
            }
            isLoaded[pageId] = true
        }
    }
}