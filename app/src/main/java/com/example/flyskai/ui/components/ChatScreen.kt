package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.LLMConfiguration

sealed class Sender {
    object User : Sender()
    object Bot : Sender()
}

data class Message(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val sender: Sender
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    llmConfig: LLMConfiguration? = null
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF90CAF9),       // Light blue (user bubble)
            onPrimary = Color.Black,           // Text on user bubble
            background = Color(0xFF121212),    // Background
            surface = Color(0xFF1E1E1E),        // AppBars, BottomBars
            onSurface = Color.White,           // Text on surfaces
            surfaceVariant = Color(0xFF2C2C2C), // Bot bubble
            onSurfaceVariant = Color.White     // Text on bot bubble
        )
    ) {
        ChatScreenContent(llmConfig)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    llmConfig: LLMConfiguration? = null
) {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var userInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Chat with FlySkAI", fontSize = 20.sp) }
            )
        },
        bottomBar = {
            BottomAppBar {
                ChatInput(
                    text = userInput,
                    onTextChanged = { userInput = it },
                    onSend = {
                        if (userInput.isNotBlank()) {
                            messages = messages + Message(content = userInput, sender = Sender.User)
                            val response = llmConfig?.let { config ->
                                "Response from ${config.modelName}: $userInput"
                            } ?: "Echo: $userInput"
                            messages = messages + Message(content = response, sender = Sender.Bot)
                            userInput = ""
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ChatMessageList(
                messages = messages,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ChatMessageList(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = messages.asReversed(), key = { it.id }) { message ->
            ChatBubble(
                message = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == Sender.User
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            color = if (isUser) colors.primary else colors.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) colors.onPrimary else colors.onSurfaceVariant,
                textAlign = if (isUser) TextAlign.End else TextAlign.Start
            )
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .weight(1f),
            placeholder = { Text("Talk with AI on ✈️ mode!") },
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}