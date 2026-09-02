package com.example.data.model

data class Friend(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val status: String,
    val isOnline: Boolean = true,
    val lastSeen: String = "Online",
    val currentWatching: String? = null
)

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val senderId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = true,
    val isRead: Boolean = true,
    val sharedDramaId: String? = null,
    val sharedDramaTitle: String? = null,
    val sharedDramaCover: String? = null
)

data class Conversation(
    val friend: Friend,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)
