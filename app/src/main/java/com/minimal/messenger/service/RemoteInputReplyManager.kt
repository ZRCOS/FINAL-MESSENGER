```kotlin
package com.minimal.messenger.service

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.minimal.messenger.data.local.MessageEntity
import com.minimal.messenger.data.local.MinimalDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object RemoteInputReplyManager {

    private const val TAG = "RemoteInputReply"

    sealed class ReplyResult {
        object Success : ReplyResult()
        data class Error(val reason: String) : ReplyResult()
        object NotificationExpired : ReplyResult()
    }

    suspend fun sendReply(
        context: Context,
        conversationId: String,
        replyText: String
    ): ReplyResult = withContext(Dispatchers.IO) {

        val text = replyText.trim()

        if (text.isEmpty()) {
            return@withContext ReplyResult.Error(
                "Escribe un mensaje antes de enviarlo."
            )
        }

        val actionHolder =
            WhatsAppNotificationListenerService.activeRemoteInputs[conversationId]
                ?: return@withContext ReplyResult.NotificationExpired

        try {
            val intent = Intent()
            val bundle = Bundle().apply {
                putCharSequence(actionHolder.remoteInput.resultKey, text)
            }

            RemoteInput.addResultsToIntent(
                arrayOf(actionHolder.remoteInput),
                intent,
                bundle
            )

            actionHolder.pendingIntent.send(context, 0, intent)

            val now = System.currentTimeMillis()
            val db = MinimalDatabase.getInstance(context)

            val sentMsg = MessageEntity(
                id = "sent_${UUID.randomUUID()}",
                conversationId = conversationId,
                sender = "user",
                text = text,
                timestamp = now,
                deliveryStatus = "sent_remote_input",
                mediaType = "none",
                notificationKey = actionHolder.notificationKey
            )

            db.minimalDao().insertMessage(sentMsg)
            db.minimalDao().updateConversationLastMessage(
                conversationId,
                text,
                now
            )

            Log.i(TAG, "Acción de respuesta enviada a Android.")

            ReplyResult.Success

        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "La acción de respuesta ya no está disponible.", e)
            ReplyResult.NotificationExpired

        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar la respuesta.", e)
            ReplyResult.Error(
                e.localizedMessage ?: "No se pudo enviar la respuesta."
            )
        }
    }
}