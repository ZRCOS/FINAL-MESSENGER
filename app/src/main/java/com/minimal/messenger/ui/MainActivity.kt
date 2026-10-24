
package com.minimal.messenger.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minimal.messenger.data.local.ConversationEntity
import com.minimal.messenger.data.local.MinimalDatabase
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDark = isSystemInDarkTheme()

            MaterialTheme(
                colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinimalMainScreen()
                }
            }
        }
    }
}

@Composable
fun MinimalMainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val dao = remember(context) {
        MinimalDatabase.getInstance(context).minimalDao()
    }

    val conversations by dao.getAllConversations()
        .collectAsState(initial = emptyList())

    var selectedConversationId by remember {
        mutableStateOf<String?>(null)
    }

    val selectedConversation = conversations.find {
        it.id == selectedConversationId
    }

    if (selectedConversation != null) {
        ChatScreen(
            conversation = selectedConversation,
            onBack = { selectedConversationId = null }
        )
    } else {
        Scaffold { padding ->
            MessagesScreen(
                conversations = conversations,
                onConversationClick = { conversation ->
                    selectedConversationId = conversation.id
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun MessagesScreen(
    conversations: List<ConversationEntity>,
    onConversationClick: (ConversationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mensajes",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Conversaciones capturadas de WhatsApp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Todavía no hay conversaciones guardadas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = conversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = {
                            onConversationClick(conversation)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val dateText = remember(conversation.lastTimestamp) {
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
        ).format(Date(conversation.lastTimestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.contactName
                        .firstOrNull()
                        ?.uppercaseChar()
                        ?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = conversation.contactName,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
