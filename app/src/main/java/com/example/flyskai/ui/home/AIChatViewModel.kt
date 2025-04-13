package com.example.flyskai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import model.PhiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AIChatViewModel(
    private val phiModel: PhiModel
) : ViewModel() {
    private val _conversation = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversation = _conversation.asStateFlow()

    fun sendMessage(message: String) {
        viewModelScope.launch {
            _conversation.value += ChatMessage(message, true)
            val response = phiModel.generateText(
                prompt = buildPrompt(_conversation.value),
                maxLength = 200
            )
            _conversation.value += ChatMessage(response, false)
        }
    }

    private fun buildPrompt(history: List<ChatMessage>): String {
        return history.joinToString("\n") { msg ->
            if (msg.isUser) "User: ${msg.text}" else "AI: ${msg.text}"
        } + "\nAI:"
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)