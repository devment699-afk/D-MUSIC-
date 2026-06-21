package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "BOT", "GROUP", "DIRECT", "CHANNEL"
    val avatarColor: Int, // ARGB Int for custom avatar rendering
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val bio: String = ""
) : Serializable

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val senderId: String, // "me", "bot", "expert1", "expert2", etc.
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = false,
    val isRead: Boolean = true
) : Serializable
