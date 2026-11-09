package com.company.app.common.domain

interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}

/** Lets a parameterless use case be called as `getSomething()` instead of `getSomething(Unit)`. */
suspend operator fun <R> UseCase<Unit, R>.invoke(): R = invoke(Unit)
