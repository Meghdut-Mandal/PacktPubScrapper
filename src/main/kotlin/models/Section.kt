package models


class Section(rawData: Map<*, *>) : BaseJsonObject(rawData) {

    val content: String?
        get() = g("content")

    val contentType: String?
        get() = g("contentType")

    val id: String?
        get() = g("id")

    val metaDescription: String?
        get() = g("metaDescription")

    val packtplusUrl: String?
        get() = g("packtplusUrl")

    val title: String
        get() = g("title") ?: ""

    val url: String?
        get() = g("url")
}