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
import okhttp3.RequestBody.Companion.toRequestBody
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
    val id: String = "",
    val email: String = "",
    val isOnline: Boolean = false,
    val lastActive: Long = 0,
    val isBlocked: Boolean = false,
    val nickname: String? = null
) {
    val uid: String get() = id
}

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

    private val _recentContacts = MutableStateFlow<List<User>>(emptyList())
    val recentContacts = _recentContacts.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<User>>(emptyList())
    val searchSuggestions = _searchSuggestions.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    fun checkSavedSession(context: Context, onResult: (Boolean) -> Unit) {
        val prefs = context.getSharedPreferences("chatapp", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val uid = prefs.getString("uid", null)
        val email = prefs.getString("email", null)

        if (token != null && uid != null && email != null) {
            val user = LocalUser(uid, email, token)
            _currentUser.value = user
            RetrofitClient.setToken(token)
            connectWebSocket()
            fetchConversations()
            onResult(true)
        } else onResult(false)
    }

    private fun saveSession(context: Context, user: LocalUser) {
        context.getSharedPreferences("chatapp", Context.MODE_PRIVATE).edit().apply {
            putString("token", user.token); putString("uid", user.uid); putString("email", user.email); apply()
        }
    }

    fun connectWebSocket() {
        val user = _currentUser.value ?: return
        if (stompClient != null) { stompClient?.disconnect() }

        val headers = mutableMapOf("Authorization" to "Bearer ${user.token}")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, NetworkConfig.WS_URL, headers)
        stompClient?.lifecycle()?.subscribe({ event ->
            if (event.type == LifecycleEvent.Type.ERROR) {
                viewModelScope.launch { delay(5000); connectWebSocket() }
            }
        }, { Log.e("WS", "Error") })
        stompClient?.connect()

        stompClient?.topic("/topic/messages/${user.uid}")?.subscribe({ topicMsg ->
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val newMsg = gson.fromJson(topicMsg.payload, ChatMessage::class.java)
                    val list = _messages.value.toMutableList()
                    val tempIdx = list.indexOfFirst { it.id == null && it.text == newMsg.text && Math.abs(it.timestamp - newMsg.timestamp) < 5000 }
                    if (tempIdx != -1) list[tempIdx] = newMsg else if (list.none { it.id == newMsg.id }) list.add(newMsg)
                    _messages.value = list.sortedBy { it.timestamp }
                    fetchConversations()
                } catch (e: Exception) { Log.e("WS", "Error parsing") }
            }
        }, { Log.e("WS", "Topic error") })
    }

    fun sendMessage(receiverId: String, receiverEmail: String, content: String, type: String = "text", fileUrl: String = "", replyTo: ChatMessage? = null) {
        val user = _currentUser.value ?: return
        val msg = ChatMessage(text = content, senderId = user.uid, receiverId = receiverId, type = type, fileUrl = fileUrl, replyToId = replyTo?.id?.toString(), replyToText = replyTo?.text)
        val list = _messages.value.toMutableList()
        list.add(msg)
        _messages.value = list
        stompClient?.send("/app/chat.sendMessage", gson.toJson(msg))?.subscribe({}, { Log.e("WS", "Send error") })
    }

    fun fetchChatMessages(otherId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _messages.value = RetrofitClient.api.getChatHistory(user.uid, otherId)
            } catch (e: Exception) { Log.e("API", "History error") }
        }
    }

    fun fetchConversations() {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _recentContacts.value = RetrofitClient.api.getConversations(user.uid)
            } catch (e: Exception) { Log.e("API", "Conv error") }
        }
    }

    fun searchUser(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _searchSuggestions.value = RetrofitClient.api.searchUsers(query)
            } catch (e: Exception) { _searchSuggestions.value = emptyList() }
        }
    }

    fun login(email: String, pass: String, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.api.login(mapOf("email" to email, "password" to pass))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val user = LocalUser(body.userId, body.email, body.token)
                    _currentUser.value = user
                    RetrofitClient.setToken(user.token)
                    saveSession(context, user)
                    viewModelScope.launch(Dispatchers.Main) { connectWebSocket(); fetchConversations(); onResult(true) }
                } else viewModelScope.launch(Dispatchers.Main) { onResult(false) }
            } catch (e: Exception) { viewModelScope.launch(Dispatchers.Main) { onResult(false) } }
        }
    }

    // ✅ SỬA: Gửi JSON Map cho Register
    fun signUp(email: String, pass: String, fName: String, lName: String, context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "email" to email,
                    "password" to pass,
                    "firstName" to fName,
                    "lastName" to lName
                )
                val res = RetrofitClient.api.register(data)
                if (res.isSuccessful) login(email, pass, context, onResult)
                else viewModelScope.launch(Dispatchers.Main) { onResult(false) }
            } catch (e: Exception) { 
                Log.e("SignUp", "Error: ${e.message}")
                viewModelScope.launch(Dispatchers.Main) { onResult(false) } 
            }
        }
    }

    fun logout(context: Context, navController: androidx.navigation.NavController) {
        stompClient?.disconnect(); _currentUser.value = null; RetrofitClient.setToken(null)
        context.getSharedPreferences("chatapp", Context.MODE_PRIVATE).edit().clear().apply()
        navController.navigate("login") { popUpTo(0) }
    }

    fun uploadFile(uri: Uri, context: Context, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploading.value = true
            try {
                val body = FileUtil.prepareFilePart(context, uri) ?: return@launch
                val response = RetrofitClient.api.uploadFile(body)
                if (response.isSuccessful && response.body() != null) {
                    viewModelScope.launch(Dispatchers.Main) { onSuccess(response.body()!!.url) }
                }
            } catch (e: Exception) { Log.e("Upload", "Error") } finally { _isUploading.value = false }
        }
    }

    fun clearSuggestions() { _searchSuggestions.value = emptyList() }
    fun getMediaHistory(): List<ChatMessage> = _messages.value.filter { it.type == "file" || it.type == "image" }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun blockUser(u: String, b: Boolean) {}
    fun updateNickname(u: String, n: String) {}
    fun setOnlineStatus(o: Boolean) {}
    fun listenToPartnerStatus(p: String) { currentPartnerId = p }
    fun revokeMessage(id: Long?) {}
    fun sendReaction(id: Long?, r: String) {}
    fun forwardMessage(m: ChatMessage, id: String, e: String) {}
    fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
}
