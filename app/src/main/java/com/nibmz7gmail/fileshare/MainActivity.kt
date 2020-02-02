package com.nibmz7gmail.fileshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.MainThread
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.model.HostEvent
import com.nibmz7gmail.fileshare.server.NsdHelper
import com.nibmz7gmail.fileshare.server.NsdHelper.Companion.SERVICE_STARTED
import com.nibmz7gmail.fileshare.storage.StorageRepository
import com.nibmz7gmail.fileshare.storage.saveFileFromUri
import timber.log.Timber

@MainThread
class MainActivity : AppCompatActivity() {

    lateinit var webview: WebView
    val nsdHelper by lazy { NsdHelper.getInstance(this) }
    val sharedPref by lazy { this.getPreferences(Context.MODE_PRIVATE) }
    val storageRepository by lazy {
        StorageRepository(this.application)
    }
    var filePickerCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webview = findViewById(R.id.webview)
        webview.settings.javaScriptEnabled = true
        webview.settings.displayZoomControls = false
        webview.setLayerType(View.LAYER_TYPE_HARDWARE, null)
//        val urlPage = "http://192.168.0.139:5500/index.html"
        val urlPage = "file:///android_res/raw/index.html"
//        val urlPage = "file:///android_asset/index.html"

        webview.addJavascriptInterface(WebAppInterface(this), "Android")

        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Timber.d("$url loaded")
                if(url == urlPage) {
                    nsdHelper.startDiscovery()
                }
            }
        }

        webview.webChromeClient = object: WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                filePickerCallback = filePathCallback
                val requestFilePicker = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(requestFilePicker, PICK_FILES)
                return true
            }
        }

        NsdHelper.EventEmitter.observe(this, Observer {
            when(it) {
                is HostEvent.Added -> {
                    webview.loadUrl("javascript:onServiceFound(${it.host})")
                }
                is HostEvent.Emit -> {
                    if(it.code == SERVICE_STARTED) {
                        Timber.e("SERVICE_STARTED called")
                        webview.loadUrl("javascript:onServiceStarted(${it.host})")
                    }
                }
            }
        })

        webview.loadUrl(urlPage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, returnIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, returnIntent)
        Timber.e("SUCCESS")
        if (requestCode == PICK_FILES) {
            Timber.e("SUCCESS")

            if(resultCode == Activity.RESULT_OK) {
                returnIntent?.data?.also { returnUri ->
                    Timber.e("SUCCESS")
                    filePickerCallback?.onReceiveValue(arrayOf(returnUri))
                    Timber.e(returnUri.path)
                    return
                }

                returnIntent?.clipData?.also { clipData ->
                    Timber.e("SUCCESS")
                    val listUri = mutableListOf<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        listUri.add(clipData.getItemAt(i).uri)
                    }
                    filePickerCallback?.onReceiveValue(listUri.toTypedArray())
                }
            } else filePickerCallback?.onReceiveValue(null)
//            AppExecutors.diskIO().execute {
//                storageRepository.saveFiles(intent)
//            }
        }

    }

    override fun onResume() {
        super.onResume()
        webview.loadUrl("javascript:onResume()")
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovery()
    }

    companion object {
        const val PICK_FILES = 2
    }

}
