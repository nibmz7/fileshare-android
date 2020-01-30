package com.nibmz7gmail.fileshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.model.HostEvent
import com.nibmz7gmail.fileshare.server.NsdHelper
import com.nibmz7gmail.fileshare.storage.StorageRepository
import timber.log.Timber

@MainThread
class MainActivity : AppCompatActivity() {

    val nsdHelper by lazy { NsdHelper.getInstance(this) }
    val sharedPref by lazy { this.getPreferences(Context.MODE_PRIVATE) }
    val storageRepository by lazy {
        StorageRepository(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
//        val urlPage = "http://192.168.0.139:5500/index.html"
        val urlPage = "file:///android_asset/index.html"
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Timber.d("$url loaded")
                if(url == urlPage) {
                    nsdHelper.startDiscovery()
                }
            }
        }

        NsdHelper.EventEmitter.observe(this, Observer {
            when(it) {
                is HostEvent.Added -> {
                    val hostName = it.host.name
                    val serviceName = it.host.serviceName
                    val ipAddr = it.host.address
                    val port = it.host.port
                    val data = "'$serviceName','$hostName', '$ipAddr:$port'"
                    if(it.host.isOwner) webView.loadUrl("javascript:setServerInfo($data)")
                    webView.loadUrl("javascript:addHost($data)")
                }
                is HostEvent.Removed -> {
                    webView.loadUrl("javascript:removeHost('${it.serviceName}')")
                }
            }
        })

        webView.loadUrl(urlPage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, returnIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, returnIntent)
        if (requestCode == PICK_FILES && resultCode == Activity.RESULT_OK) {

            AppExecutors.diskIO().execute {
                storageRepository.saveFiles(intent)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovery()
    }

    companion object {
        const val PICK_FILES = 2
    }

}
