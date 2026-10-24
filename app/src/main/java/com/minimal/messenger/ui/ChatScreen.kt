
package com.minimal.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minimal.messenger.data.local.ConversationEntity
import com.minimal.messenger.data.local.MessageEntity
import com.minimal.messenger.data.local.MinimalDatabase
import com.minimal.messenger.service.RemoteInputReplyManager
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversation: ConversationEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(context) {
        MinimalDatabase.getInstance(context).minimalDao()
    }

    val messages by dao.getMessagesForConversation(conversation.id)
        .collectAsState(initial = emptyList())

    var messageText by remember(conversation.id) { mutableStateOf("") }
    var statusText by remember(conversation.id) { mutableStateOf<String?>(null) }
    var sending by remember(conversation.id) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversation.contactName,
                            maxLines = 1
                        )
                        Text(
                            text = "Historial local",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Column {
                    statusText?.let { status ->
                        Text(
                            text = status,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 6.dp
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe un mensaje…") },
                            enabled = !sending,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (messageText.isNotBlank() && !sending) {
                                        sendChatReply(
                                            text = messageText,
                                            conversation = conversation,
                                            context = context,
                                            onSending = { sending = it },
                                            onStatus = { statusText = it },
                                            onClearText = { messageText = "" },
                                            scope = scope
                                        )
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            enabled = messageText.isNotBlank() && !sending,
                            onClick = {
                                sendChatReply(
                                    text = messageText,
                                    conversation = conversation,
                                    context = context,
                                    onSending = { sending = it },
                                    onStatus = { statusText = it },
                                    onClearText = { messageText = "" },
                                    scope = scope
                                )
                            }
                        ) {
                            if (sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar"
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todavía no hay mensajes guardados para esta conversación.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }
        }
    }
}

private fun sendChatReply(
    text: String,
    conversation: ConversationEntity,
    context: android.content.Context,
    onSending: (Boolean) -> Unit,
    onStatus: (String?) -> Unit,
    onClearText: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val cleanText = text.trim()
    if (cleanText.isBlank()) return

    scope.launch {
        onSending(true)
        onStatus(null)

        try {
            when (
                val result = RemoteInputReplyManager.sendReply(
                    context = context,
                    conversationId = conversation.id,
                    replyText = cleanText
                )
            ) {
                is RemoteInputReplyManager.ReplyResult.Success -> {
                    onClearText()
                    onStatus(
                        "Respuesta enviada a WhatsApp. La entrega no está confirmada."
                    )
                }

                is RemoteInputReplyManager.ReplyResult.NotificationExpired -> {
                    onStatus(
                        "La acción de respuesta ya no está disponible. Espera una nueva notificación de WhatsApp."
                    )
                }

                is RemoteInputReplyManager.ReplyResult.Error -> {
                    onStatus("No se pudo enviar: ${result.reason}")
                }
            }
        } catch (e: Exception) {
            onStatus("Error al enviar el mensaje.")
        } finally {
            onSending(false)
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    // El RemoteInputReplyManager guarda los mensajes propios con sender = "user".
    val isMine = message.sender == "user" || message.sender == "me"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = DateFormat.getTimeInstance(DateFormat.SHORT)
                        .format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (
                    isMine &&
                    (message.deliveryStatus == "sent_remote_input" ||
                     message.deliveryStatus == "reply_action_sent")
                ) {
                    Text(
                        text = "Envío solicitado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
