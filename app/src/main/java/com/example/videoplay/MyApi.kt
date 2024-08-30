package com.example.videoplay

import com.google.android.exoplayer2.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface MyAPI {
    @Multipart
    @POST("api/v1/videos")
    fun uploadVideo(
        @Part file: MultipartBody.Part,
        @Part("title") title:RequestBody,
        @Part("description") description: RequestBody
    ): Call<UploadResponse>
    @GET("api/v1/stream/{videoId}")
    suspend fun streamVideo(@Path("videoId") videoId: String): Response<ResponseBody>

    companion object {
        operator fun invoke(): MyAPI {
            // Setup logging interceptor
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Create OkHttpClient with logging
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl("http://192.168.98.70:8080/")
                .client(okHttpClient) // Use OkHttpClient with logging
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MyAPI::class.java)
        }
    }
}
