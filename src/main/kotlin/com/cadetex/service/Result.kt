package com.cadetex.service

/**
 * Tipo Result para manejar operaciones que pueden fallar de forma elegante
 * Similar a Either en FP, pero más simple y legible en Kotlin
 */
sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Error -> throw IllegalArgumentException(message)
    }
    
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Error -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(value)
        is Error -> this
    }
}

/**
 * Helper function para crear un Success
 */
fun <T> success(value: T): Result<T> = Result.Success(value)

/**
 * Helper function para crear un Error
 */
fun error(message: String): Result<Nothing> = Result.Error(message)

