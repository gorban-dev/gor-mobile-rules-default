package com.company.app.navigation

import kotlinx.serialization.Serializable

/** Screen arguments are key fields: an id, not the model — the screen's ViewModel loads the data. */
@Serializable
data class ExampleDetailKey(val itemId: String) : AppNavKey
