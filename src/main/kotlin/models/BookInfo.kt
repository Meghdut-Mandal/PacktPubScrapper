package models

data class BookInfo(
    val bookId: String,
    val title: String,
    val author: String,
    val isbn10: String,
    val oneLiner: String,
    val about: String,
    val coverImage: String,
    val category: String,
    val readUrl: String,
    val type: String,
    val bookChapters: List<BookChapter> = listOf()
)