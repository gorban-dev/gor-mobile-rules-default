package com.company.app.common.presentation

import kotlin.random.Random

/**
 * A transient message (snackbar) kept in the ViewState until the UI reports it as shown.
 * [id] makes two equal texts two different messages.
 */
data class UserMessage(
    val text: UiText,
    val id: Long = Random.nextLong(),
)
