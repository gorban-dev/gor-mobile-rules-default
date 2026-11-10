package com.company.app.feature.example.domain.repository

import com.company.app.core.network.ApiResult
import com.company.app.feature.example.data.model.KtorExample

@MustUseReturnValues
interface IExampleRepository {
    suspend fun getExample(): ApiResult<KtorExample>
}
