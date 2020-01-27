package com.nibmz7gmail.fileshare.model

sealed class ServerEvent {
    data class Success(val code: Int, val message: String = "") : ServerEvent()
    data class Error(val code: Int, val message: String) : ServerEvent()
    data class Loading(val message: String) : ServerEvent()
    data class Emit(val code: Int, val message: String = "") : ServerEvent()
}