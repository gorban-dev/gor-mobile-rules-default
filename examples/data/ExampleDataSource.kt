package com.company.app.feature.example.data.datasource

import com.company.app.core.network.ApiClient
import com.company.app.core.network.ApiResult
import com.company.app.feature.example.data.model.KtorExample
import io.ktor.http.path

@MustUseReturnValues
class ExampleRemoteDataSource(
    private val api: ApiClient,
) {

    suspend fun fetchExample(): ApiResult<KtorExample> {
        return api.get {
            url {
                path("api/example")
            }
        }
    }

    suspend fun saveExample(): ApiResult<Unit> {
        return api.post {
            url {
                path("api/example/save")
            }
        }
    }
}
