package com.company.app.feature.example.presentation.model

// One-off system intents only. Navigation goes through Navigator, errors and messages — through the state.
sealed interface ExampleViewAction {
    data class OpenDialer(val phone: String) : ExampleViewAction
}
