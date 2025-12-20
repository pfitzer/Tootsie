package com.pfitzer.tootsie.util

/**
 * A generic wrapper class for handling success and error states in the domain layer.
 * Provides type-safe error handling without exceptions.
 */
sealed class Result<out T> {
    /**
     * Represents a successful result with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents an error result with optional message and exception.
     */
    data class Error(
        val message: String? = null,
        val exception: Throwable? = null
    ) : Result<Nothing>()

    /**
     * Represents a loading state.
     */
    data object Loading : Result<Nothing>()

    /**
     * Returns true if this result is a success.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this result is an error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this result is loading.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if this is a success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if this is a success, or throws the exception if error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: Exception(message ?: "Unknown error")
        is Loading -> throw IllegalStateException("Result is still loading")
    }
}

/**
 * Extension function to map successful results to a different type.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

/**
 * Extension function to perform an action on success.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Extension function to perform an action on error.
 */
inline fun <T> Result<T>.onError(action: (String?, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(message, exception)
    }
    return this
}
