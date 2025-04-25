package com.example.flyskai


import com.example.flyskai.JniWrapper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


class JniWrapperModel(private val jniWrapper: JniWrapper = JniWrapper().instance()): ViewModel() {
    companion object {
        @JvmStatic
        private val NanosPerSecond = 1_000_000_000.0
    }

    private val tag: String? = this::class.simpleName

    private var _messages: MutableState<List<String>> = mutableStateOf(listOf("Initializing..."))
    var messages: List<String> by _messages
        private set

    private var _message = mutableStateOf("")
    var message: String by _message
        private set

    fun updateMessage(newMessage: String) {
        _message.value = newMessage
    }

//    fun load(pathToModel: String) {
//        viewModelScope.launch {
//            try {
//                jniWrapper.load(pathToModel)
//                messages += "Loaded $pathToModel"
//            } catch (exc: IllegalStateException) {
//                Log.e(tag, "load() failed", exc)
//                messages += exc.message!!
//            }
//        }
//    }

    fun clear() {
        _messages.value = listOf()
    }

    fun log(message: String) {
        _messages.value += message
    }

}