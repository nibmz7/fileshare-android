package com.nibmz7gmail.fileshare.storage

import android.app.Application
import android.content.Intent
import androidx.annotation.WorkerThread
import timber.log.Timber

class StorageRepository(private val context: Application) {

    @WorkerThread
    fun saveFiles(intent: Intent?) {
        Timber.e("called")
        intent?.data?.also { returnUri ->
            Timber.e("called")

            context.saveFileFromUri(returnUri)
            return
        }

        intent?.clipData?.also { clipData ->
            Timber.e("called")

            for (i in 0 until clipData.itemCount) {
                context.saveFileFromUri(clipData.getItemAt(i).uri)
            }
        }
    }
}