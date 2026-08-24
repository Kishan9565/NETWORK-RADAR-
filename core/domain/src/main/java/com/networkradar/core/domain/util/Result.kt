package com.networkradar.core.domain.util

sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : com.networkradar.core.domain.util.Error>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : Error, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(transform(data))
    }
}

inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Error) {
        action(error)
    }
    return this
}

fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> {
    return map { }
}

typealias EmptyResult<E> = Result<Unit, E>
