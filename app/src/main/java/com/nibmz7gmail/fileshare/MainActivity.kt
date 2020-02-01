package com.nibmz7gmail.fileshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.model.HostEvent
import com.nibmz7gmail.fileshare.server.NsdHelper
import com.nibmz7gmail.fileshare.server.NsdHelper.Companion.SERVICE_STARTED
import com.nibmz7gmail.fileshare.storage.StorageRepository
import timber.log.Timber

@MainThread
class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView
    val nsdHelper by lazy { NsdHelper.getInstance(this) }
    val sharedPref by lazy { this.getPreferences(Context.MODE_PRIVATE) }
    val storageRepository by lazy {
        StorageRepository(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        val urlPage = "http://192.168.0.139:5500/index.html"
//        val urlPage = "file:///android_res/raw/index.html"
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
                    webView.loadUrl("javascript:onServiceFound(${it.host})")
                }
                is HostEvent.Emit -> {
                    if(it.code == SERVICE_STARTED) {
                        Timber.e("SERVICE_STARTED called")
                        webView.loadUrl("javascript:onServiceStarted(${it.host})")
                    }
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

    override fun onResume() {
        super.onResume()
        webView.loadUrl("javascript:onResume()")
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovery()
    }

    companion object {
        const val PICK_FILES = 2
    }

}
