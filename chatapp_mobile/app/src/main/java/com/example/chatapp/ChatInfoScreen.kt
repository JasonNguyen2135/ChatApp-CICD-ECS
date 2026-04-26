package com.example.chatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(navController: NavController, viewModel: ChatViewModel, userId: String, userEmail: String) {
    val recentContacts by viewModel.recentContacts.collectAsState()
    val mediaMessages = viewModel.getMediaHistory()
    
    // ✅ FIX: Dùng thuộc tính .id và .nickname của Object User
    val contact = recentContacts.find { it.id == userId }
    val displayName = contact?.nickname ?: userEmail
    val isBlocked = contact?.isBlocked ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tùy chọn", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F6F7)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF0068FF)), contentAlignment = Alignment.Center) {
                Text(displayName.take(1).uppercase(), fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(userEmail, color = Color.Gray, fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))

            // Ảnh/Video đã gửi
            InfoSection("Ảnh, link, file đã gửi") {
                if (mediaMessages.isEmpty()) {
                    Text("Chưa có phương tiện nào", Modifier.padding(16.dp), color = Color.Gray)
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(mediaMessages) { msg ->
                            AsyncImage(
                                model = msg.fileUrl, contentDescription = null,
                                modifier = Modifier.size(80.dp).padding(4.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cài đặt bảo mật
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                InfoItem(Icons.Default.Block, if (isBlocked) "Bỏ chặn" else "Chặn người dùng", Color.Red) {
                    viewModel.blockUser(userId, !isBlocked)
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                InfoItem(Icons.Default.Delete, "Xóa lịch sử trò chuyện", Color.Red) {
                    // Logic xóa
                }
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Text(title, Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = Color(0xFF0068FF))
        content()
    }
}

@Composable
fun InfoItem(icon: ImageVector, text: String, color: Color = Color.Black, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = color, fontSize = 16.sp)
    }
}
