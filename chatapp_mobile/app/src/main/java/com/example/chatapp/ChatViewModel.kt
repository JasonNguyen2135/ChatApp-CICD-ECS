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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// --- MODELS ---
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

    private val _searchSuggestions = MutableStateFlow<List<User>>(emptyList())
    val searchSuggestions = _searchSuggestions.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    // --- WEBSOCKET ---
    private fun connectWebSocket() {
        val user = _currentUser.value ?: return
        if (stompClient != null) { stompClient?.disconnect(); stompClient = null }

        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = "Bearer ${user.token}"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, NetworkConfig.WS_URL, headers)
        stompClient?.lifecycle()?.subscribe { event ->
            if (event.type == LifecycleEvent.Type.ERROR) {
                viewModelScope.launch { delay(5000); connectWebSocket() }
            }
        }
        stompClient?.connect()

        stompClient?.topic("/topic/messages/${user.uid}")?.subscribe { topicMsg ->
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val newMsg = gson.fromJson(topicMsg.payload, ChatMessage::class.java)
                    val list = _messages.value.toMutableList()
                    val idx = list.indexOfFirst { it.id == newMsg.id }
                    if (idx != -1) list[idx] = newMsg else list.add(newMsg)
                    _messages.value = list
                } catch (e: Exception) { Log.e("WS", "Parse error") }
            }
        }
    }

    // --- FIX LỖI: CÁC PHƯƠNG THỨC THIẾU ---

    fun uploadFile(context: Context, uri: Uri, receiverId: String, receiverEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.value = true
            try {
                val file = File(FileUtil.getPath(context, uri) ?: return@launch)
                val requestFile = file.asRequestBody(context.contentResolver.getType(uri)?.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                
                val response = RetrofitClient.api.uploadFile(body)
                if (response.isSuccessful && response.body() != null) {
                    sendMessage(receiverId, receiverEmail, "[File] ${file.name}", type = "file", fileUrl = response.body()!!.url)
                }
            } catch (e: Exception) { Log.e("Upload", "${e.message}") } finally { _isUploading.value = false }
        }
    }

    fun sendMessage(receiverId: String, receiverEmail: String, content: String, type: String = "text", fileUrl: String = "", replyTo: ChatMessage? = null) {
        val user = _currentUser.value ?: return
        val msg = ChatMessage(text = content, senderId = user.uid, receiverId = receiverId, type = type, fileUrl = fileUrl, replyToId = replyTo?.id?.toString(), replyToText = replyTo?.text)
        stompClient?.send("/app/chat.sendMessage", gson.toJson(msg))?.subscribe()
    }

    fun revokeMessage(messageId: Long) {
        stompClient?.send("/app/chat.revokeMessage", messageId.toString())?.subscribe()
    }

    fun sendReaction(messageId: Long, reaction: String) {
        val payload = mapOf("messageId" to messageId, "reaction" to reaction)
        stompClient?.send("/app/chat.react", gson.toJson(payload))?.subscribe()
    }

    fun forwardMessage(message: ChatMessage, targetUserId: String) {
        sendMessage(targetUserId, "", "[Forwarded] ${message.text}", type = message.type, fileUrl = message.fileUrl)
    }

    fun fetchChatHistory(otherId: String) {
        currentPartnerId = otherId
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _messages.value = RetrofitClient.api.getChatHistory(user.uid, otherId)
            } catch (e: Exception) { Log.e("API", "${e.message}") }
        }
    }

    fun getMediaHistory(): List<ChatMessage> = _messages.value.filter { it.type == "file" || it.type == "image" }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun searchUser(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Mock search - Trong thực tế gọi API
            _searchSuggestions.value = listOf(User(uid = "user1", email = "test@gmail.com"))
        }
    }

    fun clearSuggestions() { _searchSuggestions.value = emptyList() }

    fun blockUser(userId: String) { Log.d("Dev", "Blocked $userId") }

    fun updateNickname(userId: String, name: String) { Log.d("Dev", "Nickname updated") }

    fun setOnlineStatus(isOnline: Boolean) {
        val user = _currentUser.value ?: return
        val status = mapOf("userId" to user.uid, "online" to isOnline)
        stompClient?.send("/app/chat.setOnline", gson.toJson(status))?.subscribe()
    }

    fun listenToPartnerStatus(partnerId: String) {
        currentPartnerId = partnerId
        // Logic listen WebSocket for status
    }

    // --- AUTH ---
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.api.login(mapOf("email" to email, "password" to pass))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val user = LocalUser(body.userId, body.email, body.token)
                    _currentUser.value = user
                    RetrofitClient.setToken(user.token)
                    viewModelScope.launch(Dispatchers.Main) { connectWebSocket(); onResult(true) }
                } else viewModelScope.launch(Dispatchers.Main) { onResult(false) }
            } catch (e: Exception) { viewModelScope.launch(Dispatchers.Main) { onResult(false) } }
        }
    }

    fun signUp(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (RetrofitClient.api.register(mapOf("email" to email, "password" to pass)).isSuccessful) login(email, pass, onResult)
                else viewModelScope.launch(Dispatchers.Main) { onResult(false) }
            } catch (e: Exception) { viewModelScope.launch(Dispatchers.Main) { onResult(false) } }
        }
    }

    fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
}
