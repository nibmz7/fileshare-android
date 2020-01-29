package com.nibmz7gmail.fileshare.server

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
        Timber.e(support.propertyChangeListeners.toString())
        return sseResponse
    }

    fun fireEvent(message: String) {
        Timber.e("FIRING NEW PROPERTY")
        Timber.e(support.propertyChangeListeners.toString())
        support.firePropertyChange("update", "old$message", message)
    }

    private inner class SseResponse: Response(Status.OK, null, null, 0), PropertyChangeListener {

        val pauseLock = Any() as Object
        var message: String? = "Lol"

        override fun propertyChange(evt: PropertyChangeEvent) {
            Timber.e("PROPERTY CHANGED")
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
            Timber.e("HEADER SENT")

            while (true) {
                try {
                    Timber.e("ENTERING")
                    synchronized(pauseLock) {
                        Timber.e("STARTING")
                        pauseLock.wait()
                        Timber.e("ENDED")
                        val data = "retry: 500\n" +
                                "event: update\n" +
                                "data: Hello\n\n"
                        val chunkedOutputStream = ChunkedOutputStream(out)
                        chunkedOutputStream.write(data.toByteArray())
                        chunkedOutputStream.finish()
                        Timber.e("Sent")
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