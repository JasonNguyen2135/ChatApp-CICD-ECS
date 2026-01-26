package com.example.chatapp

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// --- MỚI: Thêm lớp này để hứng dữ liệu JSON từ Server ---
data class UploadResponse(val url: String)

interface ApiService {
    @GET("api/messages/{senderId}/{receiverId}")
    suspend fun getChatHistory(
        @Path("senderId") senderId: String,
        @Path("receiverId") receiverId: String
    ): List<ChatMessage>

    @Multipart
    @POST("api/files/upload")
    // SỬA Ở ĐÂY: Nhận về UploadResponse thay vì String
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResponse>
}

object RetrofitClient {
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            // Chỉ cần dùng GsonConverterFactory cho tất cả JSON
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}