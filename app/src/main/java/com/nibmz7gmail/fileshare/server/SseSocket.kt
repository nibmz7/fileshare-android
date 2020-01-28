package com.nibmz7gmail.fileshare.server

import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.content.ContentType
import org.nanohttpd.protocols.http.response.ChunkedOutputStream
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status
import timber.log.Timber
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class SseSocket(handshakeRequest: IHTTPSession) {

    val handshakeResponse: Response =
        object : Response(Status.OK, null, null, 0) {
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
                        val data = "retry: 500\n" +
                                "event: update\n" +
                                "data: Hello\n\n"
                        val chunkedOutputStream = ChunkedOutputStream(out)
                        chunkedOutputStream.write(data.toByteArray())
                        chunkedOutputStream.finish()
                        Timber.e("Sent")
                        Thread.sleep(3000)
                    } catch (e: Exception) {
                        Timber.e(e)
                        pw.close()
                        out.close()
                        break
                    }
                }
            }

        }

}