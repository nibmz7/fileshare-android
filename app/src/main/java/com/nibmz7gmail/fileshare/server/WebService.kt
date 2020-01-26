package com.nibmz7gmail.fileshare.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.MainActivity
import com.nibmz7gmail.fileshare.R
import com.nibmz7gmail.fileshare.model.Event
import timber.log.Timber

class WebService : LifecycleService() {

    private val CHANNEL_ID = "WebServiceChannel"
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private var nsdHelper: NsdHelper? = null
    private val webServer = Server(this)

    override fun onCreate() {
        super.onCreate()
        ServerLiveData.observe(this, Observer {
            when (it) {
                is Event.Success -> {
                    notificationManager.notify(1,
                        createNotification(R.string.title_ready, R.string.message_ready))
                    nsdHelper = NsdHelper(this).apply {
                        initializeNsd()
                        registerService()
                        discoverServices()
                    }
                }
                is Event.Error -> {

                }
                is Event.Loading -> {

                }
                is Event.Emit -> {
                    Timber.d("closing")
                    stopServer()
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(mChannel)
        }

        startForeground(1,
            createNotification(R.string.title_loading, R.string.message_loading))

        webServer.start()

        return START_NOT_STICKY
    }

    private fun createNotification(title: Int, message: Int, code: String? = "xxxx"): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(title))
            .setContentText(getString(message, code))
            .setSmallIcon(R.drawable.server_notification)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .setColor(getColor(R.color.colorPrimary))
            .setColorized(true)
            .build()
    }

    private fun stopServer() {
        webServer.stop()
        nsdHelper?.tearDown()
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

