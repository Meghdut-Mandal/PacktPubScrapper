import com.mongodb.client.model.Filters.eq
import models.*
import org.litote.kmongo.*

class BooksDatabase {

    // connect to mongoDb database
    private val mongoURl = System.getenv("MONGO_URL") ?: "mongodb://localhost:27017"
    private val mongoClient = KMongo.createClient(mongoURl)
    private val database = mongoClient.getDatabase("books")

    // create a pages collection
    private val pagesCollection = database.getCollection<SectionContent>("pages")

    //        create a bookInfo collection
    private val bookInfoCollection = database.getCollection<Map<*, *>>("bookInfo")


    //    save book info
    fun saveBookInfo(bookInfo: Map<*, *>): BookInfo {
        val book = BookInfo(bookInfo)
        bookInfo.toMutableMap().apply {
            this["_id"] = book.bookId
            bookInfoCollection.save(this)
        }
        return book
    }

    //     get book info by id
    fun getBookInfoById(id: Long): BookInfo? {
        val data = bookInfoCollection.findOne(eq("_id", id)) ?: return null
        return BookInfo(data)
    }

    fun getPagesByBookId(bookId: Long): List<SectionContent> {
        return pagesCollection.find(eq("bookId", bookId)).toList()
    }


    fun savePage(
        bookInfo: BookInfo,
        bookChapter: BookChapter,
        sectionId: String,
        content: String
    ) {
        val sectionContentKey = SectionContentKey(bookInfo.bookId, bookChapter.id, sectionId)
        val sectionContent = SectionContent(sectionContentKey,bookInfo.bookId, content)
        pagesCollection.save(sectionContent)
    }

}