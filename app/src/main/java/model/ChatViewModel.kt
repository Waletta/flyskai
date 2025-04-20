package model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ChatViewModel(private val context: Context) : ViewModel() {
    private val model = PhiModel.instance()

    private val _chatMessages = mutableStateListOf<Pair<String, String>>()
    val chatMessages: List<Pair<String, String>> get() = _chatMessages

    private val _currentResponse = mutableStateOf("")
    val currentResponse: String get() = _currentResponse.value

    init {
        Log.i("ChatViewModel", "✅ init called — ViewModel is alive")

        viewModelScope.launch {
            try {
                Log.i("Maria", "🚀 Coroutine launched")

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Model loading...", Toast.LENGTH_SHORT).show()
                }

                val modelPath = File(context.filesDir, "llms_data/Phi-3-mini-4k-instruct-q4.gguf").absolutePath
                Log.i("ChatViewModel", "📦 Model path = $modelPath")

                model.load(modelPath)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "✅ Model loaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "❌ Exception in coroutine", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Load failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
