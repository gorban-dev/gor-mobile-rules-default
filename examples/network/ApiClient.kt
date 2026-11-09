package com.company.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

/**
 * The only way datasources talk to the network: every call returns [ApiResult] and never throws a request failure.
 * The [HttpClient] must be built with `expectSuccess = true`, so non-2xx responses reach [toApiError].
 */
@MustUseReturnValues
class ApiClient(@PublishedApi internal val httpClient: HttpClient) {

    suspend inline fun <reified T> get(noinline block: HttpRequestBuilder.() -> Unit): ApiResult<T> =
        apiCall { httpClient.get(block).typedBody<T>() }

    suspend inline fun <reified T> post(noinline block: HttpRequestBuilder.() -> Unit): ApiResult<T> =
        apiCall { httpClient.post(block).typedBody<T>() }

    suspend inline fun <reified T> put(noinline block: HttpRequestBuilder.() -> Unit): ApiResult<T> =
        apiCall { httpClient.put(block).typedBody<T>() }

    suspend inline fun <reified T> delete(noinline block: HttpRequestBuilder.() -> Unit): ApiResult<T> =
        apiCall { httpClient.delete(block).typedBody<T>() }
}

/** The single place where a thrown request failure becomes an [ApiError]. Cancellation is never swallowed. */
@PublishedApi
internal suspend inline fun <T> apiCall(block: () -> ApiResult<T>): ApiResult<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    ApiResult.Failure(e.toApiError())
}

/** A 204 carries no body: a typed request gets [ApiError.NoContent] instead of a deserialization failure. */
@PublishedApi
internal suspend inline fun <reified T> HttpResponse.typedBody(): ApiResult<T> =
    if (status == HttpStatusCode.NoContent && T::class != Unit::class) {
        ApiResult.Failure(ApiError.NoContent)
    } else {
        ApiResult.Success(body<T>())
    }

// Log every failure here; send Parse and Unknown to the crash reporter as non-fatals.
@PublishedApi
internal suspend fun Throwable.toApiError(): ApiError = when (this) {
    is ResponseException -> response.toHttpError()
    is IOException -> ApiError.Network(this)
    is SerializationException, is JsonConvertException, is NoTransformationFoundException -> ApiError.Parse(this)
    else -> ApiError.Unknown(this)
}

private suspend fun HttpResponse.toHttpError(): ApiError = when (status.value) {
    401 -> ApiError.Unauthorized
    432 -> ApiError.Banned
    else -> {
        val body = readBodyOrNull()
        ApiError.Http(
            status = status,
            serverMessage = body?.let(::parseServerMessage),
            requestId = headers["X-Request-Id"],
            body = body,
        )
    }
}

private suspend fun HttpResponse.readBodyOrNull(): String? = try {
    bodyAsText()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Adjust to the backend's error format; here it is `{"message": "..."}`. */
private fun parseServerMessage(body: String): String? = try {
    Json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}
