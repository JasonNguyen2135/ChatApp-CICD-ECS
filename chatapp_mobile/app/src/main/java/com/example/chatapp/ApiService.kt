package com.example.chatapp

import okhttp3.Interceptor
import okhttp3.MultipartBody
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
    @POST("api/auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body credentials: Map<String, String>): Response<String>

    @GET("api/messages/{senderId}/{receiverId}")
    suspend fun getChatHistory(
        @Path("senderId") senderId: String,
        @Path("receiverId") receiverId: String
    ): List<ChatMessage>

    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<UploadResponse>
}

object RetrofitClient {
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
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