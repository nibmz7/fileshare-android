package com.nibmz7gmail.fileshare.model

sealed class Event<T> {
    data class Success<T>(val data: T) : Event<T>()
    data class Error<T>(val message: String, val data: T? = null) : Event<T>()
    data class Loading<T>(val message: String) : Event<T>()
    data class Emit<T>(val data: T) : Event<T>()
}