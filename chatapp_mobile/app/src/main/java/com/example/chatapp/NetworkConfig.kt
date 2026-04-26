package com.example.chatapp

object NetworkConfig {
    // ✅ Lấy URL từ BuildConfig (được truyền vào lúc build CI)
    // Nếu build local không có biến, nó sẽ lấy địa chỉ ALB mặc định của bạn
    val BASE_URL = BuildConfig.BASE_URL
    val WS_URL = BuildConfig.WS_URL
}
