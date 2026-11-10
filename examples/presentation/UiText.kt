package com.company.app.common.presentation

import org.jetbrains.compose.resources.StringResource

/**
 * Text that the ViewModel hands to the UI without resolving it: a resource id or a raw backend string.
 * The UI resolves it with `asString()`. Android-only projects may use `@StringRes val res: Int` instead.
 */
sealed interface UiText {
    data class Res(val res: StringResource) : UiText
    data class Raw(val value: String) : UiText
}
