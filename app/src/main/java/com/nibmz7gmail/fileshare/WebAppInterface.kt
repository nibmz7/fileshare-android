package com.nibmz7gmail.fileshare

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import com.nibmz7gmail.fileshare.model.ServerEvent
import com.nibmz7gmail.fileshare.server.Server
import com.nibmz7gmail.fileshare.server.Server.Companion.START_SERVER
import com.nibmz7gmail.fileshare.server.Server.Companion.STOP_SERVER
import com.nibmz7gmail.fileshare.server.WebService

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun startServer(hostName: String) {
        AppExecutors.mainThread().execute {
            val webServiceIntent = Intent(context, WebService::class.java)
            webServiceIntent.putExtra("hostname", hostName)
            ContextCompat.startForegroundService(context, webServiceIntent)
        }
    }

    @JavascriptInterface
    fun stopServer() {
        AppExecutors.mainThread().execute {
            Server.EventEmitter.setStatus(ServerEvent.Emit(STOP_SERVER))
        }
    }
}