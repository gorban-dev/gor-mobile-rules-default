package com.company.app.common.presentation

/** Screen content that is loaded from a request: the error of the load replaces the content. */
sealed interface Content<out T> {
    data object Loading : Content<Nothing>
    data class Data<out T>(val value: T) : Content<T>
    data class Error(val text: UiText) : Content<Nothing>
}
