package com.minimal.messenger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val phoneNumber: String? = null,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCountIncrement: Int = 0,
    val tonePreference: String = "amigable",
    val customNotes: String = "",
    val hasActiveNotificationAction: Boolean = false,
    val packageOrigin: String = "com.whatsapp"
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val deliveryStatus: String,
    val mediaType: String = "none",
    val notificationKey: String? = null
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val timestamp: Long,
    val type: String,
    val isVideo: Boolean = false,
    val durationSeconds: Int = 0
)
