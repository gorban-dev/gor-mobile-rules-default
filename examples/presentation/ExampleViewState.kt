package com.company.app.feature.example.presentation.model

import com.company.app.common.presentation.Content
import com.company.app.common.presentation.UserMessage
import com.company.app.feature.example.domain.model.Example

data class ExampleViewState(
    val content: Content<Example> = Content.Loading,
    val message: UserMessage? = null,
    /** Navigation after logic: Screen closes the screen and sends `SavedHandled`, which resets it. */
    val isSaved: Boolean = false,
)
