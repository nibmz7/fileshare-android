package com.nibmz7gmail.fileshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import com.nibmz7gmail.fileshare.MainActivity.Companion.PICK_FILES
import com.nibmz7gmail.fileshare.model.ServerEvent
import com.nibmz7gmail.fileshare.server.Server
import com.nibmz7gmail.fileshare.server.Server.Companion.STOP_SERVER
import com.nibmz7gmail.fileshare.server.WebService

class WebAppInterface(private val activity: MainActivity) {

    @JavascriptInterface
    fun startServer(hostName: String) {
        AppExecutors.mainThread().execute {
            val webServiceIntent = Intent(activity, WebService::class.java)
            webServiceIntent.putExtra("hostname", hostName)
            ContextCompat.startForegroundService(activity, webServiceIntent)
        }
    }

    @JavascriptInterface
    fun stopServer() {
        AppExecutors.mainThread().execute {
            Server.EventEmitter.setStatus(ServerEvent.Emit(STOP_SERVER))
        }
    }

    @JavascriptInterface
        fun openFilesPicker() {
        AppExecutors.mainThread().execute {
            val requestFilePicker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            activity.startActivityForResult(requestFilePicker, PICK_FILES)
        }
    }
}