package com.company.app.feature.example.domain.usecase

import com.company.app.common.domain.UseCase
import com.company.app.core.network.ApiResult
import com.company.app.core.network.map
import com.company.app.feature.example.data.mapper.toDomain
import com.company.app.feature.example.domain.model.Example
import com.company.app.feature.example.domain.repository.IExampleRepository

@MustUseReturnValues
class GetExampleDataUseCase(
    private val repository: IExampleRepository,
) : UseCase<Unit, ApiResult<Example>> {

    override suspend fun invoke(params: Unit): ApiResult<Example> {
        return repository.getExample().map { it.toDomain() }
    }
}
