package com.example.chatapp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class UploadResponse(val url: String)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String
)

interface ApiService {
    // ✅ SỬA: Chuyển sang @Body để gửi JSON
    @POST("api/auth/register")
    suspend fun register(@Body data: Map<String, String>): Response<Map<String, String>>

    @POST("api/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<AuthResponse>

    @GET("api/messages/{senderId}/{receiverId}")
    suspend fun getChatHistory(
        @Path("senderId") senderId: String,
        @Path("receiverId") receiverId: String
    ): List<ChatMessage>

    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(@Part file: okhttp3.MultipartBody.Part): Response<UploadResponse>

    @GET("api/auth/search")
    suspend fun searchUsers(@Query("query") query: String): List<User>

    @GET("api/messages/conversations/{userId}")
    suspend fun getConversations(@Path("userId") userId: String): List<User>
}

object RetrofitClient {
    private var token: String? = null
    fun setToken(newToken: String?) { token = newToken }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        token?.let { request.addHeader("Authorization", "Bearer $it") }
        chain.proceed(request.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
