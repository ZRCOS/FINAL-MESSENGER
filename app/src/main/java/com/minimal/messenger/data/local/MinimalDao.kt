package com.minimal.messenger.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MinimalDao {

    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMsg, lastTimestamp = :time WHERE id = :convId")
    suspend fun updateConversationLastMessage(convId: String, lastMsg: String, time: Long)

    @Query("UPDATE conversations SET unreadCountIncrement = 0 WHERE id = :convId")
    suspend fun markConversationAsRead(convId: String)

    @Query("SELECT COUNT(*) > 0 FROM messages WHERE conversationId = :convId AND text = :text AND timestamp > :sinceTime")
    suspend fun isRecentDuplicate(convId: String, text: String, sinceTime: Long): Boolean

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("DELETE FROM calls")
    suspend fun clearAllCalls()
}
