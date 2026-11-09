package com.company.app.feature.example.data.repository

import com.company.app.core.network.ApiResult
import com.company.app.feature.example.data.datasource.ExampleRemoteDataSource
import com.company.app.feature.example.data.model.KtorExample
import com.company.app.feature.example.domain.repository.IExampleRepository

@MustUseReturnValues
class ExampleRepository(
    private val remoteDataSource: ExampleRemoteDataSource,
) : IExampleRepository {

    override suspend fun getExample(): ApiResult<KtorExample> {
        return remoteDataSource.fetchExample()
    }
}
