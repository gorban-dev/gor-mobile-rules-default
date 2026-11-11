package com.company.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * A screen key in the back stack. Every screen is its own `@Serializable` subtype in this package:
 * a sealed interface accepts subtypes only from its own package and module.
 */
@Serializable
sealed interface AppNavKey : NavKey
