package com.mackson.delivery.util

/** Lightweight result wrapper so repository calls never leak raw exceptions into the UI layer. */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
}

suspend fun <T> resultOf(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    AppResult.Error(t.message ?: "Something went wrong. Please try again.", t)
}
