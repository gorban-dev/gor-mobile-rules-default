package com.company.app.feature.example.data.mapper

import com.company.app.feature.example.data.model.KtorExample
import com.company.app.feature.example.domain.model.Example

fun KtorExample.toDomain(): Example = Example(
    title = title.orEmpty(),
)
