object NetworkConfig {
    // this is base url where i do the requests
    const val BASE_URL = "https://services.packtpub.com/"

    //#URL to request jwt token, params by post are user and pass, return jwt token
    const val AUTH_ENDPOINT = "auth-v1/users/tokens"

    // progamming language books urls
    const val BOOKS_ENDPOINT = "https://subscription.packtpub.com/api/product/content"

    // API Url for videos
    const val VIDEO_ENDPOINT = "https://subscription.packtpub.com/api/products"

    const val API_BASE = "https://subscription.packtpub.com/api/products/"
}