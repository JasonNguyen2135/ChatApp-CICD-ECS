package com.example.chatapp

object NetworkConfig {
    // ✅ ALB trỏ về cổng 80 mặc định
    const val BASE_URL = "http://chatapp-nexus-alb-1541337314.ap-southeast-1.elb.amazonaws.com/"
    
    // ✅ WebSocket cũng đi qua cổng 80
    const val WS_URL = "ws://chatapp-nexus-alb-1541337314.ap-southeast-1.elb.amazonaws.com/ws/websocket"
}
