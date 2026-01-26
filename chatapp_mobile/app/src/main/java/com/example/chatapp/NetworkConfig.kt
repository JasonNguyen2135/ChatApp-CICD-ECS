package com.example.chatapp

object NetworkConfig {
    const val BASE_URL = BuildConfig.BASE_URL
    const val WS_URL = BASE_URL.replace("http", "ws") + "ws"
}