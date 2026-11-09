package com.company.app.ui.text

import androidx.compose.runtime.Composable
import com.company.app.common.presentation.UiText
import org.jetbrains.compose.resources.stringResource

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(res)
    is UiText.Raw -> value
}
