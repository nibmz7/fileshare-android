package com.nibmz7gmail.fileshare.model

import org.json.JSONObject

data class Host(
    val serviceName: String,
    val address: String,
    val port: Int
) {
    companion object {
        fun create(jsonString: String): Host {
            val obj = JSONObject(jsonString)
            return Host(
                obj.optString("serviceName", ""),
                obj.optString("address", ""),
                obj.optInt("port", 0)
            )
        }
    }
    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("serviceName", serviceName)
        obj.put("address", address)
        obj.put("port", port)
        return  obj.toString()
    }

     override fun toString(): String {
        return "'$serviceName', '$address:$port'"
    }
}

sealed class HostEvent{
    data class Added(val host: Host) : HostEvent()
    data class Removed(val serviceName: String) : HostEvent()
    data class Error(val code: Int, val message: String) : HostEvent()
    data class Emit(val code: Int, val host: Host? = null) : HostEvent()
}