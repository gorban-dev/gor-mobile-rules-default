package com.company.app.feature.example.presentation.viewmodel

sealed interface ExampleViewAction {
    data class NavigateToDetail(val itemId: String) : ExampleViewAction
}
