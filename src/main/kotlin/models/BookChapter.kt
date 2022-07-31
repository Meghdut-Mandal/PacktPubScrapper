package models


class BookChapter(rawData: Map<*, *>) : BaseJsonObject(rawData) {
    val id:String
        get() = g("id")?: ""

    val title:String
        get() = g("title") ?: ""

    val sections:List<Section>
        get() = l("sections").map { Section(it) }
}