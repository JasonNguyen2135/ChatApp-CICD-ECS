package com.example.chatapp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

    // QUAN TRỌNG: Dùng String? để khớp với Backend
    @SerializedName("replyToId") val replyToId: String? = null,
    @SerializedName("replyToText") val replyToText: String? = null,
    @SerializedName("reaction") val reaction: String? = null
)

class ChatViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val gson = Gson()
    private var stompClient: StompClient? = null

    // Biến để lọc tin nhắn chuyển tiếp
    private var currentPartnerId: String? = null

    // State quản lý tin nhắn và tìm kiếm
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _searchSuggestions = MutableStateFlow<List<User>>(emptyList())
    val searchSuggestions = _searchSuggestions.asStateFlow()

    val messages = combine(_messages, _searchQuery) { msgs, query ->
        if (query.isBlank()) msgs else msgs.filter { it.text.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _partnerStatus = MutableStateFlow<User?>(null)
    val partnerStatus = _partnerStatus.asStateFlow()

    private val _recentContacts = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val recentContacts = _recentContacts.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    init {
        if (auth.currentUser != null) {
            connectWebSocket()
            fetchRecentContacts()
        }
    }

    // --- KẾT NỐI WEBSOCKET ---
    private fun connectWebSocket() {
        if (stompClient != null) { stompClient?.disconnect(); stompClient = null }
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, NetworkConfig.WS_URL)

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

        val myId = auth.currentUser?.uid ?: return
        stompClient?.topic("/topic/messages/$myId")?.subscribe { topicMessage ->
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val payload = topicMessage.payload
                    if (payload.isNullOrBlank() || !payload.startsWith("{")) return@launch

                    val newMsg = gson.fromJson(payload, ChatMessage::class.java)

                    // --- BỘ LỌC CHUYỂN TIẾP ---
                    val isFromPartner = newMsg.senderId == currentPartnerId
                    val isToPartner = newMsg.receiverId == currentPartnerId
                    val isMe = newMsg.senderId == auth.currentUser?.uid

                    // Chỉ hiển thị tin nhắn nếu nó thuộc cuộc trò chuyện này
                    if (isFromPartner || (isMe && isToPartner)) {
                        val currentList = _messages.value.toMutableList()
                        val index = currentList.indexOfFirst { it.id == newMsg.id }

                        if (index != -1) {
                            currentList[index] = newMsg // Cập nhật (Reaction/Revoke)
                        } else {
                            currentList.add(newMsg) // Thêm mới
                        }
                        _messages.value = currentList
                    }

                    // Lưu contact nếu là người lạ
                    if (newMsg.senderId != myId) saveContact(newMsg.senderId, "Người lạ")

                } catch (e: Exception) {
                    Log.e("Socket", "Lỗi Parse JSON: ${e.message}")
                }
            }
        }
    }

    // --- CÁC TÍNH NĂNG CHAT ---
    fun sendMessage(receiverId: String, receiverEmail: String, content: String, type: String = "text", fileUrl: String = "", replyTo: ChatMessage? = null) {
        val myId = auth.currentUser?.uid ?: return
        val msg = ChatMessage(
            id = null,
            text = content,
            senderId = myId,
            receiverId = receiverId,
            type = type,
            fileUrl = fileUrl,
            replyToId = replyTo?.id?.toString(), // Chuyển sang String
            replyToText = replyTo?.text
        )
        stompClient?.send("/app/chat.sendMessage", gson.toJson(msg))?.subscribe()
        saveContact(receiverId, receiverEmail)
    }

    fun forwardMessage(msg: ChatMessage, uid: String, email: String) {
        sendMessage(uid, email, msg.text, msg.type, msg.fileUrl)
    }

    fun sendReaction(messageId: Long, reactionIcon: String) {
        val payload = mapOf("id" to messageId, "reaction" to reactionIcon)
        stompClient?.send("/app/chat.sendReaction", gson.toJson(payload))?.subscribe()
    }

    // --- THU HỒI TIN NHẮN ---
    fun revokeMessage(messageId: Long) {
        val payload = mapOf("id" to messageId)
        stompClient?.send("/app/chat.revokeMessage", gson.toJson(payload))?.subscribe()
    }

    // --- DATA & HELPER ---
    fun fetchChatMessages(otherId: String) {
        currentPartnerId = otherId // Cập nhật ID người đang chat
        val myId = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = RetrofitClient.api.getChatHistory(myId, otherId)
                _messages.value = history
                if (history.isNotEmpty()) {
                    val otherEmail = if (history[0].senderId == myId) history[0].receiverId else history[0].senderId
                    saveContact(otherId, otherEmail)
                }
            } catch (e: Exception) { Log.e("API", "Lỗi lấy lịch sử: ${e.message}") }
        }
    }

    fun getMediaHistory(): List<ChatMessage> {
        return _messages.value.filter { (it.type == "image" || it.fileUrl.isNotEmpty()) && !it.isRevoked }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // --- QUẢN LÝ CONTACT & USER ---
    private fun saveContact(targetUid: String, defaultName: String) {
        val myId = auth.currentUser?.uid ?: return
        val contactRef = db.collection("users").document(myId).collection("contacts").document(targetUid)
        contactRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = mapOf("uid" to targetUid, "email" to defaultName, "nickname" to defaultName, "isBlocked" to false)
                contactRef.set(data)
            }
        }
    }

    fun fetchRecentContacts() {
        val myId = auth.currentUser?.uid ?: return
        db.collection("users").document(myId).collection("contacts").addSnapshotListener { s, _ ->
            _recentContacts.value = s?.documents?.map {
                mapOf(
                    "uid" to it.id,
                    "email" to (it.getString("email")?:""),
                    "nickname" to (it.getString("nickname") ?: it.getString("email") ?: "Unknown"),
                    "isBlocked" to (it.getBoolean("isBlocked") ?: false).toString()
                )
            } ?: emptyList()
        }
    }

    fun blockUser(targetUid: String, isBlock: Boolean) {
        val myId = auth.currentUser?.uid ?: return
        db.collection("users").document(myId).collection("contacts").document(targetUid).update("isBlocked", isBlock)
    }

    fun updateNickname(targetUid: String, newName: String) {
        val myId = auth.currentUser?.uid ?: return
        db.collection("users").document(myId).collection("contacts").document(targetUid).update("nickname", newName)
    }

    fun setOnlineStatus(isOnline: Boolean) {
        val myId = auth.currentUser?.uid ?: return
        db.collection("users").document(myId).update(mapOf("isOnline" to isOnline, "lastActive" to System.currentTimeMillis()))
    }

    fun listenToPartnerStatus(uid: String) {
        db.collection("users").document(uid).addSnapshotListener { s, _ ->
            if (s != null) _partnerStatus.value = User(uid, isOnline = s.getBoolean("isOnline")?:false, lastActive = s.getLong("lastActive")?:0)
        }
    }

    fun uploadFile(uri: Uri, context: Context, onSuccess: (String) -> Unit) {
        _isUploading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val body = FileUtil.prepareFilePart(context, uri)
                if (body != null) {
                    val res = RetrofitClient.api.uploadFile(body)
                    if (res.isSuccessful && res.body() != null) {
                        viewModelScope.launch(Dispatchers.Main) { onSuccess(res.body()!!.url) }
                    }
                }
            } catch (e: Exception) { Log.e("Upload", "${e.message}") }
            finally { _isUploading.value = false }
        }
    }
    // --- AUTHENTICATION (ĐĂNG NHẬP / ĐĂNG KÝ) ---
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _currentUser.value = auth.currentUser
                connectWebSocket()
                fetchRecentContacts()
            }
            onResult(task.isSuccessful)
        }
    }

    fun signUp(email: String, pass: String, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.uid!!
                // Lưu user mới vào Firestore
                val user = User(uid = uid, email = email, isOnline = true, lastActive = System.currentTimeMillis())
                db.collection("users").document(uid).set(user)

                _currentUser.value = auth.currentUser
                connectWebSocket()
            }
            onResult(task.isSuccessful)
        }
    }

    fun logout() {
        setOnlineStatus(false) // Báo offline trước khi thoát
        stompClient?.disconnect()
        stompClient = null
        auth.signOut()
        _currentUser.value = null
        _messages.value = emptyList()
        _recentContacts.value = emptyList()
        _partnerStatus.value = null
    }

    // --- TÌM KIẾM NGƯỜI DÙNG (Để chat mới) ---
    fun searchUser(query: String) {
        if (query.isBlank()) {
            _searchSuggestions.value = emptyList()
            return
        }
        // Tìm theo email (bắt đầu bằng query)
        db.collection("users")
            .whereGreaterThanOrEqualTo("email", query)
            .whereLessThanOrEqualTo("email", query + "\uf8ff")
            .limit(5)
            .get()
            .addOnSuccessListener { s ->
                _searchSuggestions.value = s.documents.mapNotNull { doc ->
                    val email = doc.getString("email") ?: ""
                    if (email.isNotEmpty()) User(uid = doc.id, email = email) else null
                }
            }
    }

    fun clearSuggestions() {
        _searchSuggestions.value = emptyList()
    }

    fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    fun formatDate(ts: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts))
}