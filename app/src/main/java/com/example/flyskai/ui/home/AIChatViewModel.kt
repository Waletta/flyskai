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

    private val defSysPrompt =
        "You are FlyskAI, a helpful and knowledgeable assistant for pilots and travelers. " +
                "You provide aviation-related advice, preflight tips, travel suggestions, and destination insights clearly and safely."

    private val _conversation = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversation = _conversation.asStateFlow()



    fun sendMessage(message: String) {
        viewModelScope.launch {
            _conversation.value += ChatMessage(message, true)
            val response = phiModel.chat(
                prompt = buildPrompt(_conversation.value)
            )
            _conversation.value += ChatMessage(response, false)
        }
    }

    private fun buildPrompt(history: List<ChatMessage>): String {
        val systemPrompt = "<|system|>\n$defSysPrompt<|end|>\n"

        val conversation = history.joinToString("") { msg ->
            val role = if (msg.isUser) "<|user|>" else "<|assistant|>"
            "$role\n${msg.text}<|end|>\n"
        }

        return "$systemPrompt$conversation<|assistant|>\n"
    }

    override fun onCleared() {
        super.onCleared()
        phiModel.unload()
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)