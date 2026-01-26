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

    val contact = recentContacts.find { it["uid"] == otherUserId }
    val displayName = contact?.get("nickname") ?: otherUserEmail
    val isBlocked = contact?.get("isBlocked")?.toBoolean() ?: false

    val zaloBlue = Color(0xFF0068FF)
    var text by remember { mutableStateOf("") }
    var replyMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showForwardDialog by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Launchers
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadFile(uri, context) { url ->
            viewModel.sendMessage(otherUserId, otherUserEmail, "Đã gửi ảnh", "image", url, replyMessage); replyMessage = null
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadFile(uri, context) { url ->
            viewModel.sendMessage(otherUserId, otherUserEmail, "Đã gửi file", "file", url, replyMessage); replyMessage = null
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Mỗi khi màn hình hiện lên (bao gồm lúc quay lại), tải lại tin nhắn
                viewModel.fetchChatMessages(otherUserId)
                viewModel.listenToPartnerStatus(otherUserId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(otherUserId) {
        viewModel.fetchChatMessages(otherUserId)
        viewModel.listenToPartnerStatus(otherUserId)
    }

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Scaffold(
        topBar = {
            Column(Modifier.background(zaloBlue).statusBarsPadding()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(displayName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val statusText = if (partnerStatus?.isOnline == true) "Đang hoạt động" else "Ngoại tuyến"
                            Text(statusText, color = Color(0xFFE0E0E0), fontSize = 11.sp)
                        }
                    },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = { navController.navigate("chat_info/$otherUserId/$otherUserEmail") }) {
                            Icon(Icons.Default.Info, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                if (isUploading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.Yellow)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Color(0xFFE2E9F1))) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUser?.uid
                    MessageBubbleExtended(
                        msg = msg, isMe = isMe, time = viewModel.formatTime(msg.timestamp),
                        onReply = { replyMessage = msg },
                        onCopy = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("msg", msg.text))
                            Toast.makeText(context, "Đã sao chép", Toast.LENGTH_SHORT).show()
                        },
                        onForward = { showForwardDialog = msg },
                        onReact = { emoji -> viewModel.sendReaction(msg.id ?: 0, emoji) },
                        onRevoke = { viewModel.revokeMessage(msg.id ?: 0) }
                    )
                }
            }
            if (!isBlocked) {
                Surface(tonalElevation = 2.dp, color = Color.White) {
                    Column {
                        if (replyMessage != null) {
                            Row(Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Reply, null, tint = zaloBlue)
                                Text(replyMessage!!.text, Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { replyMessage = null }) { Icon(Icons.Default.Close, null) }
                            }
                        }
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
            } else Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("Bạn đã chặn người dùng này") }
        }

        if (showForwardDialog != null) {
            ForwardDialog(
                recentContacts = recentContacts,
                onDismiss = { showForwardDialog = null },
                onSend = { uid, email ->
                    viewModel.forwardMessage(showForwardDialog!!, uid, email)
                    showForwardDialog = null
                    Toast.makeText(context, "Đã chuyển tiếp", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// --- UI COMPONENTS ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleExtended(
    msg: ChatMessage, isMe: Boolean, time: String,
    onReply: () -> Unit, onCopy: () -> Unit, onForward: () -> Unit, onReact: (String) -> Unit, onRevoke: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val emojis = listOf("❤️", "😂", "👍", "😮", "😢")

    Box(Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (showMenu) {
                Surface(tonalElevation = 4.dp, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    Row(Modifier.padding(4.dp)) { emojis.forEach { e -> Text(e, Modifier.clickable { onReact(e); showMenu = false }.padding(8.dp), fontSize = 20.sp) } }
                }
            }
            Surface(
                color = if (isMe) Color(0xFFD6F0FF) else Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { showMenu = true }).widthIn(max = 280.dp)
            ) {
                Column(Modifier.padding(8.dp)) {
                    if (msg.isRevoked) {
                        Text("Tin nhắn đã bị thu hồi", color = Color.Gray, fontStyle = FontStyle.Italic)
                    } else {
                        if (msg.replyToText != null) {
                            Text("Trả lời: ${msg.replyToText}", fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        }
                        if (msg.type == "image") {
                            AsyncImage(model = msg.fileUrl, contentDescription = null, modifier = Modifier.clip(RoundedCornerShape(8.dp)).fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        } else if (msg.type == "file") {
                            Row { Icon(Icons.Default.AttachFile, null); Text("File đính kèm", Modifier.padding(start = 8.dp)) }
                        } else {
                            Text(msg.text, fontSize = 15.sp)
                        }
                        if (!msg.reaction.isNullOrEmpty()) {
                            Box(Modifier.align(Alignment.End).offset(y = 8.dp).background(Color.White, CircleShape).padding(2.dp)) { Text(msg.reaction!!, fontSize = 10.sp) }
                        }
                    }
                }
            }
            Text(time, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (!msg.isRevoked) {
                    DropdownMenuItem(text = { Text("Trả lời") }, onClick = { onReply(); showMenu = false })
                    DropdownMenuItem(text = { Text("Sao chép") }, onClick = { onCopy(); showMenu = false })
                    DropdownMenuItem(text = { Text("Chuyển tiếp") }, onClick = { onForward(); showMenu = false })
                    if (isMe) DropdownMenuItem(text = { Text("Thu hồi", color = Color.Red) }, onClick = { onRevoke(); showMenu = false })
                }
            }
        }
    }
}

@Composable
fun ForwardDialog(recentContacts: List<Map<String, String>>, onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Chuyển tiếp đến:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                LazyColumn(Modifier.heightIn(max = 300.dp).padding(vertical = 12.dp)) {
                    items(recentContacts) { contact ->
                        val name = contact["nickname"] ?: contact["email"] ?: "User"
                        Row(Modifier.fillMaxWidth().clickable { onSend(contact["uid"]!!, contact["email"]!!) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0068FF)), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = Color.White) }
                            Text(name, Modifier.padding(start = 12.dp), fontSize = 16.sp)
                        }
                    }
                }
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("Hủy", color = Color.Gray) }
            }
        }
    }
}