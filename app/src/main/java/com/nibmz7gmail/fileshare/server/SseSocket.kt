package com.nibmz7gmail.fileshare.server

import android.os.Looper
import androidx.annotation.WorkerThread
import org.nanohttpd.protocols.http.content.ContentType
import org.nanohttpd.protocols.http.response.ChunkedOutputStream
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status
import timber.log.Timber
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class SseSocket(source: Any) {

    private val support = PropertyChangeSupport(source)

    fun createSseResponse(): Response {
        val sseResponse = SseResponse()
        support.addPropertyChangeListener(sseResponse)
        return sseResponse
    }

    @WorkerThread
    fun fireEvent(message: String) {
        support.firePropertyChange("update", null, message)
    }

    private inner class SseResponse: Response(Status.OK, null, null, 0), PropertyChangeListener {

        val pauseLock = Any() as Object
        var message: String? = "Lol"

        override fun propertyChange(evt: PropertyChangeEvent) {
            synchronized(pauseLock) {
                this.message = evt.newValue.toString()
                pauseLock.notify()
            }
        }

        override fun send(out: OutputStream) {
            val gmtFrmt = SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            gmtFrmt.timeZone = TimeZone.getTimeZone("GMT")

            val pw = PrintWriter(
                BufferedWriter(
                    OutputStreamWriter(
                        out,
                        ContentType(mimeType).encoding
                    )
                ), false
            )

            pw.append("HTTP/1.1 200 OK\r\n")
            pw.append("Access-Control-Allow-Origin: *\r\n")
            pw.append("Content-Type: text/event-stream\r\n")
            pw.append("Date: ${gmtFrmt.format(Date())}\r\n")
            pw.append("Cache-Control: no-cache\r\n")
            pw.append("Connection: keep-alive\r\n")
            pw.append("Keep-Alive: timeout=60, max=1000\r\n")
            pw.append("Transfer-Encoding: chunked0\r\n\r\n")
            pw.flush()

            while (true) {
                try {
                    synchronized(pauseLock) {
                        pauseLock.wait()
                        val data = "data: $message\n\n"
                        val chunkedOutputStream = ChunkedOutputStream(out)
                        chunkedOutputStream.write(data.toByteArray())
                        chunkedOutputStream.finish()
                        Timber.i("Message sent")
                    }
                } catch (e: Exception) {
                    Timber.e("SENDING FAILED CLIENT CLOSED CONNECTION")
                    support.removePropertyChangeListener(this)
                    break
                }
            }

            pw.close()
            out.close()
            Timber.e("CLOSE THIS BITCH ASS STREAM THEN")
        }
    }
}