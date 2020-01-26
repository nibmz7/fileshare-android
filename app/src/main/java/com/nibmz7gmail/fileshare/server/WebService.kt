package com.nibmz7gmail.fileshare.server

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.MainActivity
import com.nibmz7gmail.fileshare.R
import com.nibmz7gmail.fileshare.model.Event
import timber.log.Timber


class WebService : LifecycleService() {

    private val CHANNEL_ID = "WebServiceChannel"
    private val webServer = Server(this)

    override fun onCreate() {
        super.onCreate()
        ServerLiveData.observe(this, Observer {
            when(it) {
                is Event.Success -> {

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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.server_notification)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .build()

        startForeground(1, notification)

        webServer.start()

        return START_NOT_STICKY
    }

    private fun stopServer() {
        webServer.stop()
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

}