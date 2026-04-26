package com.example.chatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Đảm bảo có import này
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(navController: NavController, viewModel: ChatViewModel) {
    var query by remember { mutableStateOf("") }
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val recentContacts by viewModel.recentContacts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val zaloBlue = Color(0xFF0068FF)

    LaunchedEffect(query) {
        if (query.length >= 2) viewModel.searchUser(query) else viewModel.clearSuggestions()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(zaloBlue).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zalo", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    Box {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Color.White)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentUser?.email?.take(1)?.uppercase() ?: "U", color = zaloBlue, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Đăng xuất", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    viewModel.logout(context, navController)
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.Red) }
                            )
                        }
                    }
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    CustomSearchBar(query, { query = it }, "Tìm kiếm bạn bè...")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F6F7))) {
            if (query.isNotEmpty() && suggestions.isNotEmpty()) {
                Text("KẾT QUẢ TÌM KIẾM", Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyColumn {
                    items(suggestions) { user ->
                        ContactItem(user.email, "Nhấn để nhắn tin") {
                            navController.navigate("chat/${user.id}/${user.email}")
                            query = ""
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recentContacts) { contact ->
                        // ✅ FIX: Dùng contact.id và contact.email (Object User)
                        val name = contact.nickname?.ifEmpty { contact.email } ?: contact.email
                        ContactItem(name, "Nhấn để trò chuyện") {
                            navController.navigate("chat/${contact.id}/${contact.email}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSearchBar(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    BasicTextField(
        value = value, onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
        singleLine = true, cursorBrush = SolidColor(Color(0xFF0068FF)),
        decorationBox = { innerTextField ->
            Row(
                Modifier.fillMaxWidth().height(40.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = Color.Gray, fontSize = 14.sp)
                    innerTextField()
                }
            }
        }
    )
}

@Composable
fun ContactItem(name: String, sub: String, onClick: () -> Unit) {
    Column {
        // Modifier này giờ sẽ hoạt động chuẩn xác
        Row(
            Modifier.fillMaxWidth().background(Color.White).clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE1F5FE)), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0068FF))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(sub, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp, modifier = Modifier.padding(start = 84.dp))
    }
}
