package models


class BookInfo(rawData: Map<*, *>) : BaseJsonObject(rawData) {

    val contentType: String
        get() = readUrl.split("/")[1]

    val readUrl: String
        get() = g("readUrl") ?: ""


    val bookId: Long
        get() = (g<Double>("productId")!!).toLong()

    val title: String
        get() = g("title") ?: ""


    val author: List<String>
        get() = rawData.l("linkAuthors")
                    .map { it["author"] as String }


    val coverImage: String?
        get() = g("coverImage")


    val bookChapters: List<BookChapter>
        get() {
            val tableOfContents = m("toc")!!
            val chapters = tableOfContents.l("chapters").map { BookChapter(it) }
            val preface = tableOfContents.l("prefaces").map { BookChapter(it) }
            val appendices = tableOfContents.l("appendices").map { BookChapter(it) }
            return preface + chapters + appendices
        }
}
