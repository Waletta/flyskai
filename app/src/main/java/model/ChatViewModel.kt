package model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(private val context: Context) : ViewModel() {
    private val model = PhiModel.instance()

    private val _chatMessages = mutableStateListOf<Pair<String, String>>()
    val chatMessages: List<Pair<String, String>> get() = _chatMessages

    private val _currentResponse = mutableStateOf("")
    val currentResponse: String get() = _currentResponse.value

    init {
        viewModelScope.launch {
            try {
                val modelPath = File(context.filesDir, "llms_data/Phi-3-mini-4k-instruct-q4.gguf").absolutePath
                model.load(modelPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(prompt: String) {
        _chatMessages.add(prompt to "")
        _currentResponse.value = ""

        viewModelScope.launch {
            model.send(prompt, formatChat = false).collect { token ->
                _currentResponse.value += token
                _chatMessages[_chatMessages.lastIndex] = prompt to _currentResponse.value
            }
        }
    }
}
