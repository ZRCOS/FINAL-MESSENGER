package com.minimal.messenger.service

import android.app.Notification
import android.app.RemoteInput
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.minimal.messenger.data.local.ConversationEntity
import com.minimal.messenger.data.local.MessageEntity
import com.minimal.messenger.data.local.MinimalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Servicio autorizado por el usuario para escuchar notificaciones del sistema.
 * Solo procesa el paquete de WhatsApp ("com.whatsapp").
 * No accede a bases de datos privadas ni intercepta conexiones de red de WhatsApp.
 */
class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { MinimalDatabase.getInstance(applicationContext) }

    companion object {
        private const val TAG = "MinimalNotifListener"
        const val WHATSAPP_PKG = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PKG = "com.whatsapp.w4b"

        // Cache en memoria de acciones de respuesta RemoteInput activas
        val activeRemoteInputs = mutableMapOf<String, RemoteInputActionHolder>()
    }

    data class RemoteInputActionHolder(
        val pendingIntent: android.app.PendingIntent,
        val remoteInput: RemoteInput,
        val notificationKey: String,
        val timestamp: Long
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg != WHATSAPP_PKG && pkg != WHATSAPP_BUSINESS_PKG) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        serviceScope.launch {
            processWhatsAppNotification(sbn, notification, extras)
        }
    }

    private suspend fun processWhatsAppNotification(
        sbn: StatusBarNotification,
        notification: Notification,
        extras: Bundle
    ) {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank() || text.isNullOrBlank()) return

        val sender = title
        val rawMessage = text
        val timestamp = sbn.postTime

        val mediaType = when {
            rawMessage.contains("📷") || rawMessage.contains("Foto") -> "image"
            rawMessage.contains("🎥") || rawMessage.contains("Vídeo") -> "video"
            rawMessage.contains("🎙️") || rawMessage.contains("Nota de voz") -> "audio"
            rawMessage.contains("📄") || rawMessage.contains("Documento") -> "document"
            else -> "none"
        }

        val conversationId = "wa_" + sender.hashCode().toString()

        extractRemoteInputAction(notification, conversationId, sbn.key)

        val conversation = ConversationEntity(
            id = conversationId,
            contactName = sender,
            lastMessage = rawMessage,
            lastTimestamp = timestamp,
            unreadCountIncrement = 1,
            hasActiveNotificationAction = true
        )
        database.minimalDao().insertOrUpdateConversation(conversation)

        val isDuplicate = database.minimalDao().isRecentDuplicate(conversationId, rawMessage, timestamp - 5000)
        if (!isDuplicate) {
            val message = MessageEntity(
                id = sbn.key + "_" + timestamp,
                conversationId = conversationId,
                sender = "contact",
                text = rawMessage,
                timestamp = timestamp,
                deliveryStatus = "captured_from_notification",
                mediaType = mediaType,
                notificationKey = sbn.key
            )
            database.minimalDao().insertMessage(message)
            Log.d(TAG, "Mensaje capturado legalmente de notificación: $sender -> $rawMessage")
        }
    }

    
private fun extractRemoteInputAction(
    notification: Notification,
    conversationId: String,
    notificationKey: String
) {
    val actions = NotificationCompat.getActionCount(notification)

    for (i in 0 until actions) {
        val action = NotificationCompat.getAction(notification, i) ?: continue
        val remoteInputs = action.remoteInputs ?: continue
        val pendingIntent = action.actionIntent ?: continue

        for (remoteInput in remoteInputs) {
            val resultKey = remoteInput.resultKey ?: continue

            val nativeRemoteInput = RemoteInput.Builder(resultKey)
                .setLabel(remoteInput.label)
                .setChoices(remoteInput.choices)
                .setAllowFreeFormInput(remoteInput.allowFreeFormInput)
                .build()

            activeRemoteInputs[conversationId] = RemoteInputActionHolder(
                pendingIntent = pendingIntent,
                remoteInput = nativeRemoteInput,
                notificationKey = notificationKey,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Acción RemoteInput registrada para: $conversationId")
            return
        }
    }

    Log.d(TAG, "No se encontró acción de respuesta para: $conversationId")
}

    }
