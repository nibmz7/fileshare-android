package com.nibmz7gmail.fileshare

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.model.HostEvent
import com.nibmz7gmail.fileshare.server.NsdHelper
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    lateinit var nsdHelper: NsdHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nsdHelper = NsdHelper.getInstance(this)
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        val urlPage = "http://192.168.0.139:5500/index.html"
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
                    webView.loadUrl("javascript:addHost('$serviceName','$hostName')")
                }
                is HostEvent.Removed -> {
                    webView.loadUrl("javascript:removeHost('${it.serviceName}')")
                }
            }
        })

        webView.loadUrl(urlPage)

//        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovery()
    }

}
