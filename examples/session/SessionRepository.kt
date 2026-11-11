package com.company.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped `single`. Events outside screens are state, not commands: `ApiClient` calls [expire] on
 * 401, login calls [start]; nobody here navigates. `AppNavHost` collects [state] and rebuilds the stack.
 */
class SessionRepository {

    private val _state = MutableStateFlow<SessionState>(SessionState.Active)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun start() {
        _state.value = SessionState.Active
    }

    fun expire() {
        _state.value = SessionState.Expired
    }
}
