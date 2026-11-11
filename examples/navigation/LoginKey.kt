package com.company.app.navigation

import kotlinx.serialization.Serializable

/** The host opens it with `replaceAll` when the session expires. */
@Serializable
data object LoginKey : AppNavKey
