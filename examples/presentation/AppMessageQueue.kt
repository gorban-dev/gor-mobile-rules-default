package com.company.app.common.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-scoped fallback for messages that must outlive the screen (e.g. a background save failed after navigation).
 * Registered as a DI single and rendered once at the app root. Screen errors stay in the screen's ViewState.
 */
class AppMessageQueue {

    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()

    fun show(text: UiText) {
        _messages.update { it + UserMessage(text) }
    }

    fun messageShown(id: Long) {
        _messages.update { messages -> messages.filterNot { it.id == id } }
    }
}
