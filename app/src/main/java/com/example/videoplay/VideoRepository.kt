package com.example.videoplay

import okhttp3.ResponseBody
import retrofit2.Response
import java.io.InputStream

class VideoRepository(private val apiService: MyAPI) {

    suspend fun fetchVideo(videoId: String): Result<InputStream> {
        return try {
            val response: Response<ResponseBody> = apiService.streamVideo(videoId)
            if (response.isSuccessful) {
                val inputStream = response.body()?.byteStream()
                if (inputStream != null) {
                    Result.success(inputStream)
                } else {
                    Result.failure(NullPointerException("Input stream is null"))
                }
            } else {
                Result.failure(Exception("Error fetching video: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
