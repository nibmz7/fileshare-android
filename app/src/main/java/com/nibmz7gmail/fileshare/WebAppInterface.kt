package com.nibmz7gmail.fileshare

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.nibmz7gmail.fileshare.model.Event
import com.nibmz7gmail.fileshare.server.Server
import com.nibmz7gmail.fileshare.server.Server.Companion.STOP_SERVER
import com.nibmz7gmail.fileshare.server.ServerLiveData
import com.nibmz7gmail.fileshare.server.WebService

class WebAppInterface(private val mContext: Context) {

    @JavascriptInterface
    fun startServer() {
        mContext.startForegroundService(Intent(mContext, WebService::class.java))
    }

    @JavascriptInterface
    fun stopServer() {
        AppExecutors.mainThread().execute {
            ServerLiveData.setStatus(Event.Emit(STOP_SERVER))
        }
    }
}