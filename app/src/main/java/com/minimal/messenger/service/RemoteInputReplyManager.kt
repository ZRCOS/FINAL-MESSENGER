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
        val actionHolder = WhatsAppNotificationListenerService.activeRemoteInputs[conversationId]
            ?: return@withContext ReplyResult.NotificationExpired

        try {
            val intent = Intent()
            val bundle = Bundle()

            bundle.putCharSequence(actionHolder.remoteInput.resultKey, replyText)
            RemoteInput.addResultsToIntent(
                arrayOf(actionHolder.remoteInput),
                intent,
                bundle
            )

            actionHolder.pendingIntent.send(context, 0, intent)

            val db = MinimalDatabase.getInstance(context)
            val sentMsg = MessageEntity(
                id = "sent_" + System.currentTimeMillis(),
                conversationId = conversationId,
                sender = "user",
                text = replyText,
                timestamp = System.currentTimeMillis(),
                deliveryStatus = "sent_remote_input",
                mediaType = "none",
                notificationKey = actionHolder.notificationKey
            )
            db.minimalDao().insertMessage(sentMsg)
            db.minimalDao().updateConversationLastMessage(
                conversationId,
                replyText,
                System.currentTimeMillis()
            )

            Log.i(TAG, "Respuesta despachada con éxito vía RemoteInput: $replyText")
            ReplyResult.Success
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "El PendingIntent de la notificación ya expiró", e)
            ReplyResult.NotificationExpired
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al enviar respuesta RemoteInput", e)
            ReplyResult.Error(e.localizedMessage ?: "Error desconocido")
        }
    }
}
