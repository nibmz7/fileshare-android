package com.nibmz7gmail.fileshare

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import com.nibmz7gmail.fileshare.MainActivity.Companion.PICK_FILES
import com.nibmz7gmail.fileshare.model.ServerEvent
import com.nibmz7gmail.fileshare.server.Server
import com.nibmz7gmail.fileshare.server.Server.Companion.STOP_SERVER
import com.nibmz7gmail.fileshare.server.WebService
import timber.log.Timber
import java.io.BufferedInputStream
import java.net.URL
import java.net.URLConnection

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

    @JavascriptInterface
    fun downloadFile(filename: String) {
        Timber.e("DOWNLOADING")
        AppExecutors.networkIO().execute {
            val url = URL("http://192.168.0.15:45635/download/cover.png")
            val connection: URLConnection = url.openConnection()
            connection.connect()
            val input = BufferedInputStream(url.openStream(), 1024)
            input.use { inputStream ->
                activity.openFileOutput("cover.png", Context.MODE_PRIVATE)?.use {
                        outputStream ->
                    val data = ByteArray(1024)
                    var total: Long = 0
                    var count: Int
                    while (true) {
                        count = inputStream.read(data)
                        if (count == -1) break
                        total += count
                        outputStream.write(data, 0, count)
                    }
                    outputStream.flush()
                }
                Timber.e("completedddd")
            }

        }
    }
}