package com.company.app.feature.example.presentation.model

// Pure navigation clicks (open detail) are not events: View calls its navigation callback directly.
sealed interface ExampleViewEvent {
    data object LoadData : ExampleViewEvent
    data object MessageShown : ExampleViewEvent
    data object OnSaveClicked : ExampleViewEvent
    data object SavedHandled : ExampleViewEvent
    data class OnCallClicked(val phone: String) : ExampleViewEvent
}
