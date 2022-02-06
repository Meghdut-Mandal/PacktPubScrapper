package models

data class BookPage(
    val pageid: String,
    val bookId: String,
    val pageContent: String,
    val chapterId : Int,
    val title: String)