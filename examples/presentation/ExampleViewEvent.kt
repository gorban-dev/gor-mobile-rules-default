package com.company.app.feature.example.presentation.viewmodel

sealed interface ExampleViewEvent {
    data object LoadData : ExampleViewEvent
    data object MessageShown : ExampleViewEvent
    data class OnItemClicked(val itemId: String) : ExampleViewEvent
}
