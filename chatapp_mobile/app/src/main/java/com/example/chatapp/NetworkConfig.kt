package com.example.chatapp

// Đảm bảo đã import BuildConfig đúng package của bạn
import com.example.chatapp.BuildConfig

object NetworkConfig {
    // Giữ const vì BuildConfig.BASE_URL là hằng số từ Gradle
    const val BASE_URL = BuildConfig.BASE_URL

    // BỎ 'const' ở đây vì có hàm xử lý chuỗi .replace()
    val WS_URL = BASE_URL.replace("http", "ws") + "ws"
}