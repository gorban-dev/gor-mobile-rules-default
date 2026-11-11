package com.company.app.core.session

/** Whether the user is signed in. The host collects it and moves to login on [Expired]. */
sealed interface SessionState {
    data object Active : SessionState
    data object Expired : SessionState
}
