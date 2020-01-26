package com.nibmz7gmail.fileshare

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import timber.log.Timber
import timber.log.Timber.DebugTree


class FileShareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }

    /** A tree which logs important information for crash reporting.  */
    private class CrashReportingTree : Timber.Tree() {
        @SuppressLint("LogNotTimber")
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            t: Throwable?
        ) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }

            if (t != null) {
                Log.e(tag, message)
            }
        }
    }
}