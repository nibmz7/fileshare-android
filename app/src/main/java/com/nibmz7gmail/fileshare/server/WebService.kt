package com.nibmz7gmail.fileshare.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.nibmz7gmail.fileshare.MainActivity
import com.nibmz7gmail.fileshare.R
import com.nibmz7gmail.fileshare.model.ServerEvent
import com.nibmz7gmail.fileshare.server.Server.Companion.START_SERVER
import com.nibmz7gmail.fileshare.server.Server.Companion.STOP_SERVER
import timber.log.Timber

class WebService : LifecycleService() {

    private val CHANNEL_ID = "WebServiceChannel"
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    private val nsdHelper by lazy { NsdHelper.getInstance(this) }
    private val webServer by lazy { Server.getInstance(this) }
    private val notificationBuilder by lazy {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.server_notification)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .setColor(getColor(R.color.colorPrimary))
            .setColorized(true)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.e("WEB SERVICE CREATED")

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

        notificationBuilder.apply {
            setContentTitle(getText(R.string.title_loading))
            setContentText(getText(R.string.message_loading))
        }
        startForeground(
            1,
            notificationBuilder.build()
        )

        Server.EventEmitter.observe(this, Observer {
            when (it) {
                is ServerEvent.Success -> {
                    if (it.code == START_SERVER) {
                        notificationBuilder.apply {
                            setContentTitle(getText(R.string.title_ready))
                            setContentText(getString(R.string.message_ready, "xxxx"))
                        }
                        notificationManager.notify(
                            1,
                            notificationBuilder.build()
                        )
                        nsdHelper.startRegister(webServer.listeningPort, it.message)
                    }

                }
                is ServerEvent.Error -> {

                }
                is ServerEvent.Loading -> {

                }
                is ServerEvent.Emit -> {
                    Timber.d("$it")
                    if (it.code == STOP_SERVER) {
                        stopForeground(true)
                        stopSelf()
                    }
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val hostname = intent?.getStringExtra("hostname") ?: "Anon"
        webServer.start(hostname)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.e("Web service is stopping")
        webServer.stop()
        nsdHelper.stopRegister()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

