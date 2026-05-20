package com.ilya.safarshots.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : AppResult<Nothing>
}

inline fun <T> runCatchingResult(
    fallbackMessage: String,
    block: () -> T,
): AppResult<T> = try {
    AppResult.Success(block())
} catch (throwable: Throwable) {
    AppResult.Error(throwable.message ?: fallbackMessage, throwable)
}
