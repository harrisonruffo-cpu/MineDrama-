package com.example.data.util

object VideoUrlResolver {
    fun resolve(url: String?): String {
        if (url.isNullOrBlank()) {
            return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        }
        return url
    }
}
