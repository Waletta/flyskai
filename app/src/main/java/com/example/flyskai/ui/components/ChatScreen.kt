package com.example.flyskai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.LLMConfiguration

@Composable
fun ChatScreen(
    llmConfig: LLMConfiguration? = null, // Optional LLM configuration for integration
    modifier: Modifier = Modifier
) {
    // State for messages (user input and responses)
    var messages by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var userInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable chat history
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Take available space
                .fillMaxWidth(),
            reverseLayout = true // New messages at bottom
        ) {
            items(messages.reversed()) { (userMessage, response) ->
                // User message
                Text(
                    text = "You: $userMessage",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                // Bot response
                Text(
                    text = "Bot: $response",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field for user input
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Type your message...") },
                singleLine = true
            )

            // Send button
            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        // Placeholder response (replace with LLM call)
                        val response = if (llmConfig != null) {
                            // Simulate LLM processing using llmConfig
                            "Response from ${llmConfig.modelName}: $userInput"
                        } else {
                            "Echo: $userInput"
                        }
                        messages = messages + (userInput to response)
                        userInput = "" // Clear input
                    }
                },
                modifier = Modifier.height(56.dp) // Match TextField height
            ) {
                Text("Send")
            }
        }
    }
}