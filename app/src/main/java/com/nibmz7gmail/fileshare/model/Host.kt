package com.nibmz7gmail.fileshare.model

data class Host(
    val serviceName: String,
    val name: String,
    val address: String,
    val port: Int,
    val isOwner: Boolean = false
)

sealed class HostEvent{
    data class Added(val host: Host) : HostEvent()
    data class Removed(val serviceName: String) : HostEvent()
    data class Error(val code: Int, val message: String) : HostEvent()
    data class Emit(val code: Int, val host: Host? = null) : HostEvent()
}