package com.nibmz7gmail.fileshare.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import timber.log.Timber
import java.io.FileInputStream

fun Context.saveFileFromUri(uri: Uri) {
    contentResolver
        .query(uri, null, null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            val size = cursor.getLong(sizeIndex).toString()

            val inputPFD = try {
                contentResolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                Timber.e(e)
                return
            }
            val fd = inputPFD!!.fileDescriptor
            val source = FileInputStream(fd).channel
            val destination = openFileOutput(name, Context.MODE_PRIVATE).channel
            destination.transferFrom(source, 0, source.size())
            source.close()
            destination.close()
            Timber.e("$name saved")

        }
}

fun Context.getDir(dirName: String) {
    val filesDir = getDir(dirName, Context.MODE_PRIVATE)
}

