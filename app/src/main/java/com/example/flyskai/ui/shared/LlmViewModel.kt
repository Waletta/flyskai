package com.example.flyskai.ui.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import model.LLMConfiguration

class LlmViewModel {
    private val _lmState = MutableStateFlow(LLMConfiguration())
    val lmState: StateFlow<LLMConfiguration> = _lmState.asStateFlow()
}