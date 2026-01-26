package com.example.chatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(navController: NavController, viewModel: ChatViewModel, partnerId: String, partnerEmail: String) {
    val mediaHistory = viewModel.getMediaHistory()
    val recentContacts by viewModel.recentContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val contact = recentContacts.find { it["uid"] == partnerId }
    val displayName = contact?.get("nickname") ?: partnerEmail
    val isBlocked = contact?.get("isBlocked")?.toBoolean() ?: false

    var showRenameDialog by remember { mutableStateOf(false) }
    var newNickname by remember { mutableStateOf(displayName) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tùy chọn", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // 1. Profile giản lược
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF0068FF)), contentAlignment = Alignment.Center) {
                        Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Đổi tên gợi nhớ")
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }

            // 2. Tìm kiếm tin nhắn
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tìm kiếm trong cuộc trò chuyện") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(24.dp))
            }

            // 3. Kho Media (Ảnh đã gửi)
            item {
                Text("Hình ảnh đã gửi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
            }

            item {

                if (mediaHistory.isEmpty()) {
                    Text("Chưa có hình ảnh nào", color = Color.Gray, fontSize = 13.sp)
                } else {
                    // Hiển thị ảnh dạng Grid nhỏ
                    Box(Modifier.heightIn(max = 400.dp)) {
                        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(250.dp)) {
                            items(mediaHistory) { msg ->
                                AsyncImage(
                                    model = msg.fileUrl, contentDescription = null,
                                    modifier = Modifier.padding(2.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // 4. Các nút chức năng Chặn
            item {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.blockUser(partnerId, !isBlocked) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isBlocked) Color.Gray else Color(0xFFFFEBEE)),
                    elevation = null
                ) {
                    Icon(if (isBlocked) Icons.Default.Check else Icons.Default.Block, null, tint = if (isBlocked) Color.White else Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isBlocked) "Bỏ chặn người dùng" else "Chặn người dùng", color = if (isBlocked) Color.White else Color.Red)
                }
            }
        }

        // Dialog đổi tên
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Đổi tên") },
                text = { OutlinedTextField(value = newNickname, onValueChange = { newNickname = it }) },
                confirmButton = { TextButton(onClick = { viewModel.updateNickname(partnerId, newNickname); showRenameDialog = false }) { Text("Lưu") } }
            )
        }
    }
}