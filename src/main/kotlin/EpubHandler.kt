import Scrapper.client
import Scrapper.database
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
//
//@OptIn(InternalAPI::class)
//fun mains() = runBlocking(Dispatchers.IO) {
//    val bookid = "9781838821470"
//    val user = User(System.getenv("user"), System.getenv("pass"))
////    user.auth()
//    user.token="Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJmOGIzMGY5Yi1mMzhlLTQxMjItYTI1NC0xZmQ3YzJkOGVmZmYiLCJ1c2VybmFtZSI6Im1lZ2hkdXQud2luZG93c0BnbWFpbC5jb20iLCJwZXJtaXNzaW9ucyI6W10sInN1YnNjcmlwdGlvbiI6WyJhbGwiXSwicGVybXMiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQT09Iiwib3JnYW5pc2F0aW9uSWRzIjpbXSwiaWF0IjoxNjQ0MTczNjY4LCJleHAiOjE2NDQyNjAwNjh9.Ojz1tZJdelXpL-4Lv3eDC6UC1vJwDrP8UBWksdZVW0vAg86PnCNS-FVQWJgQlnazpB2r-bIC9_PD5VZfeGnAhtkFr05VnEG-rAq70pPooGlqR75wxkW3GiwmPUX5mYxrpiY8LmDQgOhRN-_gMum7-dC3kFV0_wZw4NEgFT3abgp-wbgnbk6l-YSQTNg1ebPIy_0DOfCBohYvjfUdUQn9l8n1yM1XKiakfSKvZmn7MkzOBWz0FS0yA2eER95RfHbT8jXy4V0Utm0Pf_j4lxtt1wynty2WNylvMVNB8pVObyFDwLngt_2tAl_2onWGvj1KoqQyHHwW-kvwy0NXZ1_FPA"
//    println(user.token)
//    val chapters = loadBookSummary(user, bookid)
//
//    val bookPages = database.readBookPages(bookid)
//    chapters.flatMap { it.sections }.forEach {
//        // add a new chapter to the book
//        sendChapter(it.title, bookPages[it.id] ?: "")
//    }
//
//    val jsonBody = JsonObject()
//    jsonBody.addProperty("title", "GraalVM Book")
//    jsonBody.addProperty("author", "GraalVM")
//    val res = client.post("http://localhost:3000/make") {
//        // json header
//        contentType(ContentType.Application.Json)
//        // add body
//        body = jsonBody
//    }
//    println("  " + res.content)
//    client.close()
//
//}

// send chapter to server
@OptIn(InternalAPI::class)
suspend fun sendChapter(title: String, data: String) {
    val jsonBody = JsonObject()
    jsonBody.addProperty("title", title)
    jsonBody.addProperty("data", data.replace("<?xml encoding=\"utf-8\" ?>", ""))
    val res = client.post("http://localhost:3000/add") {
        // json header
        contentType(ContentType.Application.Json)
        // add body
        body = jsonBody
    }
    println("sent chapter $title with status ${res.status}")
}


