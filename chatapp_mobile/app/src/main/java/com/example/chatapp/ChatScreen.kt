package com.example.chatapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(navController: NavController, viewModel: ChatViewModel, otherUserId: String, otherUserEmail: String) {
    val messages by viewModel.messages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val recentContacts by viewModel.recentContacts.collectAsState()
    val partnerStatus by viewModel.partnerStatus.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    // ✅ FIX: Dùng thuộc tính của Object User
    val contact = recentContacts.find { it.id == otherUserId }
    val displayName = contact?.nickname ?: otherUserEmail
    val isBlocked = contact?.isBlocked ?: false

    val zaloBlue = Color(0xFF0068FF)
    var text by remember { mutableStateOf("") }
    var replyMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showForwardDialog by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->     
        if (uri != null) viewModel.uploadFile(uri, context) { url ->
            viewModel.sendMessage(otherUserId, otherUserEmail, "Ảnh", "image", url, replyMessage); replyMessage = null
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadFile(uri, context) { url ->
            viewModel.sendMessage(otherUserId, otherUserEmail, "File", "file", url, replyMessage); replyMessage = null
        }
    }

    LaunchedEffect(otherUserId) {
        viewModel.fetchChatMessages(otherUserId)
        viewModel.listenToPartnerStatus(otherUserId)
    }

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.clickable { navController.navigate("chat_info/$otherUserId/$otherUserEmail") }) {
                        Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(if (partnerStatus?.isOnline == true) "Vừa mới truy cập" else "Ngoại tuyến", fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { /* Call */ }) { Icon(Icons.Default.Call, null) }
                    IconButton(onClick = { /* Video */ }) { Icon(Icons.Default.VideoCall, null) }
                    IconButton(onClick = { navController.navigate("chat_info/$otherUserId/$otherUserEmail") }) { Icon(Icons.Default.Menu, null) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFE2E9F1))) {
            LazyColumn(modifier = Modifier.weight(1f), state = listState, contentPadding = PaddingValues(8.dp)) {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUser?.uid
                    MessageBubble(msg, isMe, viewModel.formatTime(msg.timestamp))
                }
            }
            if (!isBlocked) {
                Surface(tonalElevation = 2.dp, color = Color.White) {
                    Row(Modifier.padding(8.dp).imePadding(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.Image, null, tint = Color.Gray) }
                        IconButton(onClick = { filePicker.launch("*/*") }) { Icon(Icons.Default.AttachFile, null, tint = Color.Gray) }
                        TextField(
                            value = text, onValueChange = { text = it }, placeholder = { Text("Tin nhắn") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        if (text.isNotBlank()) IconButton(onClick = { viewModel.sendMessage(otherUserId, otherUserEmail, text, "text", "", replyMessage); text = ""; replyMessage = null }) { Icon(Icons.Default.Send, null, tint = zaloBlue) }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean, time: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isMe) Color(0xFFC0D9FF) else Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(8.dp)) {
                Text(msg.text, fontSize = 16.sp)
                Text(time, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
