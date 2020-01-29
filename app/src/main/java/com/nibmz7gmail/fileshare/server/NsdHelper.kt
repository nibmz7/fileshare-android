package com.nibmz7gmail.fileshare.server

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.*
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.LiveData
import com.nibmz7gmail.fileshare.model.Host
import com.nibmz7gmail.fileshare.model.HostEvent
import timber.log.Timber
import java.lang.Exception


class NsdHelper(context: Application) {
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: DiscoveryListener? = null
    private var registrationListener: RegistrationListener? = null
    private var myServiceName: String = ""

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
        const val SERVICE_NAME = "NiMzShare"

        private val LOCK = Any()
        private var instance: NsdHelper? = null

        fun getInstance(context: Context): NsdHelper {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = NsdHelper(context.applicationContext as Application)
                }
                return instance as NsdHelper
            }
        }
    }

    object EventEmitter : LiveData<HostEvent>() {

        fun setStatus(event: HostEvent) {
            value = event
        }

        fun postStatus(event: HostEvent) {
            postValue(event)
        }
    }

    fun createResolveListener(): ResolveListener {
        return object : ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("Resolve failed code: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Timber.e("Resolve Succeeded. $serviceInfo")
                val port: Int = serviceInfo.port
                val address: String = serviceInfo.host.hostAddress
                val hostname: String =
                    serviceInfo.attributes["hostname"]?.toString(Charsets.UTF_8) ?: "Anon"
                if (serviceInfo.serviceName == myServiceName) {
                    Timber.d("Same IP.")
                }
                val host = Host(serviceInfo.serviceName, hostname, address, port)
                EventEmitter.postStatus(HostEvent.Added(host))
                Timber.d("Service Name:${serviceInfo.serviceName} port:$port address:$address name:$hostname")
            }
        }
    }

    fun startRegister(localPort: Int, hostname: String) {
        stopRegister()
        initializeRegistrationListener()
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = localPort
            setAttribute("hostname", hostname)
        }
        try {
            nsdManager.registerService(serviceInfo, PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    fun stopRegister() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) {
                Timber.e(e)
            }
            registrationListener = null
        }
    }

    fun startDiscovery() {
        stopDiscovery()
        initializeDiscoveryListener()
        nsdManager.discoverServices(SERVICE_TYPE, PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Timber.e(e)
            }
            discoveryListener = null
        }
    }

    fun initializeRegistrationListener() {
        registrationListener = object : RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                myServiceName = NsdServiceInfo.serviceName
                Timber.d("Service registered: $myServiceName")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Timber.d("Service registration failed: $arg1")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Timber.d("Service unregistered: %s", arg0.serviceName)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.d("Service unregistration failed: $errorCode")
            }
        }
    }

    fun initializeDiscoveryListener() {
        discoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Timber.d("Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != SERVICE_TYPE) return
                if (service.serviceName.contains(SERVICE_NAME)) {
                    Timber.d("File sharing service found: ${service.serviceName}")
                    nsdManager.resolveService(service, createResolveListener())
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Timber.e("service lost$service")
                if (service.serviceName.contains(SERVICE_NAME)) {
                    EventEmitter.postStatus(HostEvent.Removed(service.serviceName))
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.i("Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }
    }

}