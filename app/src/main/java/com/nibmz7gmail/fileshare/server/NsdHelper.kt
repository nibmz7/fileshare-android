package com.nibmz7gmail.fileshare.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.*
import android.net.nsd.NsdServiceInfo
import timber.log.Timber
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket


class NsdHelper(context: Context) {
    var mContext: Context
    var mNsdManager: NsdManager
    var mResolveListener: ResolveListener? = null
    var mDiscoveryListener: DiscoveryListener? = null
    var mRegistrationListener: RegistrationListener? = null
    var mServiceName = "NsdChat"
    var chosenServiceInfo: NsdServiceInfo? = null
    var mSocket: ServerSocket? = null

    fun initializeNsd() {
        initializeResolveListener()
    }

    fun initializeDiscoveryListener() {
        mDiscoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Timber.d("Service discovery success$service")
                when {
                    service.serviceType != SERVICE_TYPE -> {
                        Timber.d(
                            "Unknown Service Type: %s", service.serviceType
                        )
                    }
                    service.serviceName == mServiceName -> {
                        Timber.d("Same machine: $mServiceName")
                        mNsdManager.resolveService(service, initializeResolveListener())

                    }
                    service.serviceName.contains(mServiceName) -> {
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.e("service lost$service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.i("Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code:$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code:$errorCode")
            }
        }
    }

    fun initializeResolveListener(): ResolveListener {
        return object : ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("Resolve failed code: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Timber.e("Resolve Succeeded. $serviceInfo")
                if (serviceInfo.serviceName == mServiceName) {
                    Timber.d("Same IP.")
                }
                val port: Int = serviceInfo.port
                val address: String = serviceInfo.host.hostAddress
                Timber.i("NameL${serviceInfo.serviceName} port:$port address:$address")
            }
        }
    }

    fun initializeRegistrationListener() {
        mRegistrationListener = object : RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                mServiceName = NsdServiceInfo.serviceName
                Timber.d("Service registered: $mServiceName")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Timber.d("Service registration failed: $arg1")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Timber.d("Service unregistered: %s", arg0.serviceName)
            }

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int
            ) {
                Timber.d("Service unregistration failed: $errorCode")
            }
        }
    }

    fun registerService() {
        initializeRegistrationListener()
        mSocket = ServerSocket(0).also { socket ->
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = mServiceName
                serviceType = SERVICE_TYPE
                port = socket.localPort
            }
            mNsdManager.registerService(
                serviceInfo, PROTOCOL_DNS_SD, mRegistrationListener
            )
        }
    }

    fun discoverServices() {
        initializeDiscoveryListener()
        mNsdManager.discoverServices(
            SERVICE_TYPE, PROTOCOL_DNS_SD, mDiscoveryListener
        )
    }

    fun tearDown() {
        mNsdManager.apply {
            unregisterService(mRegistrationListener)
            stopServiceDiscovery(mDiscoveryListener)
            mSocket?.close()
        }
    }

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
    }

    init {
        mContext = context
        mNsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
}