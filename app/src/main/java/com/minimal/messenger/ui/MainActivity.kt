
package com.minimal.messenger.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Settings
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val dao = remember(context) {
        MinimalDatabase.getInstance(context).minimalDao()
    }

    val conversations by dao.getAllConversations()
        .collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Mensajes"
                        )
                    },
                    label = { Text("Mensajes") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(Icons.Default.Call, contentDescription = "Llamadas")
                    },
                    label = { Text("Llamadas") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> MessagesScreen(conversations)
                1 -> SimpleInfoScreen(
                    title = "Llamadas",
                    description = "El registro de llamadas se conectará en una siguiente mejora."
                )
                2 -> SimpleInfoScreen(
                    title = "Ajustes",
                    description = "Acceso a notificaciones y privacidad local."
                )
            }
        }
    }
}

@Composable
private fun MessagesScreen(conversations: List<ConversationEntity>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = conversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationRow(conversation)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ConversationEntity) {
    val dateText = remember(conversation.lastTimestamp) {
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
        ).format(Date(conversation.lastTimestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
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

        Column(modifier = Modifier.weight(1f)) {
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

@Composable
private fun SimpleInfoScreen(title: String, description: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
