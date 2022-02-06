package models


import com.google.gson.annotations.SerializedName

data class BookChapter(
    @SerializedName("id")
    val id: String,
    @SerializedName("sections")
    val sections: List<Section>,
    @SerializedName("title")
    val title: String
)