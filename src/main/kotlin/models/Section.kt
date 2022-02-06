package models


import com.google.gson.annotations.SerializedName

data class Section(
    @SerializedName("content")
    val content: String,
    @SerializedName("contentType")
    val contentType: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("metaDescription")
    val metaDescription: String,
    @SerializedName("packtplusUrl")
    val packtplusUrl: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("url")
    val url: String
)