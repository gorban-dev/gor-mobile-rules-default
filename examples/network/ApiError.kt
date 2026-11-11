package com.company.app.core.network

import io.ktor.http.HttpStatusCode

/** Why a request failed. A value carried by [ApiResult.Failure], not an exception. */
sealed interface ApiError {

    /** 401 after the Ktor Auth refresh failed. [ApiClient] expires the session; screens only reset their loading state. */
    data object Unauthorized : ApiError

    /** Project-specific status for a blocked account (apteka.ru uses 432). Delete it if the backend has none. */
    data object Banned : ApiError

    /** Any other non-2xx response. [serverMessage] is the user-facing text from the backend, if any. */
    data class Http(
        val status: HttpStatusCode,
        val serverMessage: String?,
        val requestId: String?,
        val body: String?,
    ) : ApiError

    /** Transport failure, including every Ktor timeout. */
    data class Network(val cause: Throwable) : ApiError

    /** 204 for a request that expects a typed body. A normal server answer, not a bug. */
    data object NoContent : ApiError

    /** The response body could not be deserialized. */
    data class Parse(val cause: Throwable) : ApiError

    /** Anything else thrown while the request ran (e.g. inside an interceptor). */
    data class Unknown(val cause: Throwable) : ApiError
}
