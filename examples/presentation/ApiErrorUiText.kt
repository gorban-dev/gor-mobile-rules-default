package com.company.app.common.presentation

import com.company.app.core.network.ApiError
import com.company.app.generated.resources.Res
import com.company.app.generated.resources.error_connection
import com.company.app.generated.resources.error_generic
import com.company.app.generated.resources.error_server

/** The only place where an [ApiError] becomes user-facing text. */
fun ApiError.toUiText(): UiText = when (this) {
    is ApiError.Http -> serverMessage?.let(UiText::Raw) ?: UiText.Res(Res.string.error_server)
    is ApiError.Network -> UiText.Res(Res.string.error_connection)
    // Unauthorized / Banned: the session layer reacts (refresh or logout); the screen gets a neutral text.
    ApiError.Unauthorized,
    ApiError.Banned,
    ApiError.NoContent,
    is ApiError.Parse,
    is ApiError.Unknown -> UiText.Res(Res.string.error_generic)
}
