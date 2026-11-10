package com.company.app.feature.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KtorExample(
    @SerialName("title")
    val title: String? = null,
)
