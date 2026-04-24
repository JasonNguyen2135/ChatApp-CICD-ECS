package com.example.chatapp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// --- MODEL DỮ LIỆU ---
data class User(
    val uid: String = "",
    val email: String = "",
    val isOnline: Boolean = false,
    val lastActive: Long = 0,
    val isBlocked: Boolean = false
)

data class ChatMessage(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("text") var text: String = "",
    @SerializedName("senderId") val senderId: String = "",
    @SerializedName("receiverId") val receiverId: String = "",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("type") val type: String = "text",
    @SerializedName("fileUrl") val fileUrl: String = "",
    @SerializedName("isRevoked") val isRevoked: Boolean = false,
    @SerializedName("isPinned") val isPinned: Boolean = false,
    @SerializedName("replyToId") val replyToId: String? = null,
    @SerializedName("replyToText") val replyToText: String? = null,
    @SerializedName("reaction") val reaction: String? = null
)

data class LocalUser(val uid: String, val email: String, val token: String)

class ChatViewModel : ViewModel() {
    private val gson = Gson()
    private var stompClient: StompClient? = null

    private var currentPartnerId: String? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    
    val messages = combine(_messages, _searchQuery) { msgs, query ->
        if (query.isBlank()) msgs else msgs.filter { it.text.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUser = MutableStateFlow<LocalUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _partnerStatus = MutableStateFlow<User?>(null)
    val partnerStatus = _partnerStatus.asStateFlow()

    private val _recentContacts = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val recentContacts = _recentContacts.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    // --- KẾT NỐI WEBSOCKET ---
    private fun connectWebSocket() {
        val user = _currentUser.value ?: return
        if (stompClient != null) { stompClient?.disconnect(); stompClient = null }
        
        // Thêm Token vào Header của WebSocket nếu cần (tùy cấu hình backend)
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = "Bearer ${user.token}"
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, NetworkConfig.WS_URL, headers)

        stompClient?.lifecycle()?.subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> Log.d("STOMP", "Kết nối WebSocket THÀNH CÔNG")
                LifecycleEvent.Type.ERROR -> {
                    Log.e("STOMP", "Lỗi kết nối: ${lifecycleEvent.exception}")
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(5000)
                        connectWebSocket()
                    }
                }
                else -> {}
            }
        }

        stompClient?.connect()

        stompClient?.topic("/topic/messages/${user.uid}")?.subscribe { topicMessage ->
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val payload = topicMessage.payload
                    if (payload.isNullOrBlank() || !payload.startsWith("{")) return@launch

                    val newMsg = gson.fromJson(payload, ChatMessage::class.java)

                    val isFromPartner = newMsg.senderId == currentPartnerId
                    val isToPartner = newMsg.receiverId == currentPartnerId
                    val isMe = newMsg.senderId == user.uid

                    if (isFromPartner || (isMe && isToPartner)) {
                        val currentList = _messages.value.toMutableList()
                        val index = currentList.indexOfFirst { it.id == newMsg.id }

                        if (index != -1) {
                            currentList[index] = newMsg
                        } else {
                            currentList.add(newMsg)
                        }
                        _messages.value = currentList
                    }
                } catch (e: Exception) {
                    Log.e("Socket", "Lỗi Parse JSON: ${e.message}")
                }
            }
        }
    }

    // --- CÁC TÍNH NĂNG CHAT ---
    fun sendMessage(receiverId: String, receiverEmail: String, content: String, type: String = "text", fileUrl: String = "", replyTo: ChatMessage? = null) {
        val user = _currentUser.value ?: return
        val msg = ChatMessage(
            id = null,
            text = content,
            senderId = user.uid,
            receiverId = receiverId,
            type = type,
            fileUrl = fileUrl,
            replyToId = replyTo?.id?.toString(),
            replyToText = replyTo?.text
        )
        stompClient?.send("/app/chat.sendMessage", gson.toJson(msg))?.subscribe()
    }

    fun fetchChatMessages(otherId: String) {
        currentPartnerId = otherId
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = RetrofitClient.api.getChatHistory(user.uid, otherId)
                _messages.value = history
            } catch (e: Exception) { Log.e("API", "Lỗi lấy lịch sử: ${e.message}") }
        }
    }

    // --- AUTHENTICATION (JWT) ---
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.login(mapOf("email" to email, "password" to pass))
                if (response.isSuccessful && response.body() != null) {
                    val authBody = response.body()!!
                    val user = LocalUser(authBody.userId, authBody.email, authBody.token)
                    _currentUser.value = user
                    RetrofitClient.setToken(user.token)
                    
                    viewModelScope.launch(Dispatchers.Main) {
                        connectWebSocket()
                        onResult(true)
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun signUp(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.register(mapOf("email" to email, "password" to pass))
                if (response.isSuccessful) {
                    // Sau khi đăng ký thành công thì tự động đăng nhập
                    login(email, pass, onResult)
                } else {
                    viewModelScope.launch(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun logout() {
        stompClient?.disconnect()
        stompClient = null
        _currentUser.value = null
        RetrofitClient.setToken(null)
        _messages.value = emptyList()
    }

    fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
}
