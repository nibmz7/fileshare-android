package com.nibmz7gmail.fileshare.server

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.lifecycle.LiveData
import com.nibmz7gmail.fileshare.model.ServerEvent
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newChunkedResponse
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.util.IHandler
import timber.log.Timber
import java.io.InputStream

class Server(private val context: Application) : NanoHTTPD(45635)  {

    private var myHostName: String = "Anon"

    companion object {
        const val START_SERVER: Int = 1
        const val STOP_SERVER: Int = 2

        private val LOCK = Any()
        private var instance: Server? = null

        fun getInstance(context: Context): Server {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = Server(context.applicationContext as Application)
                }
                return instance as Server
            }
        }
    }

    init {
        addHTTPInterceptor(SSEInterceptor)
    }

    object SSEInterceptor : IHandler<IHTTPSession, Response> {

        override fun handle(input: IHTTPSession): Response? {
            val uri = input.uri.removePrefix("/").ifEmpty { "index.html" }
            if(uri == "events") {
                val sseSocket = SseSocket(input)
                return sseSocket.handshakeResponse
            } else return null

        }
    }


    object EventEmitter : LiveData<ServerEvent>() {

        fun setStatus(event: ServerEvent) {
            value = event
        }
    }

    fun start(hostname: String) {
        myHostName = hostname
        try {
            if(isAlive) {
                Timber.e("Server is already listening on $listeningPort")
                EventEmitter.setStatus(ServerEvent.Success(START_SERVER, myHostName))
                return
            }
            start(SOCKET_READ_TIMEOUT, false)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun start(timeout: Int, daemon: Boolean) {
        super.start(timeout, daemon)
        if(wasStarted()) {
            Timber.i("Listening on port $listeningPort")
            EventEmitter.setStatus(ServerEvent.Success(START_SERVER, myHostName))
        }
    }

    override fun stop() {
        super.stop()
        if(!isAlive) Timber.e("Server has been stopped")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.removePrefix("/").ifEmpty { "index.html" }

        if (uri == "upload") {
            val fileUpload = ServletFileUpload()
            val iter: FileItemIterator = fileUpload.getItemIterator(NanoHttpdContext(session))
            while (iter.hasNext()) {
                val item: FileItemStream = iter.next()
                val name = item.fieldName
                val inputStream: InputStream = item.openStream()

                if (item.isFormField) {
                    println("Form field $name with value ${Streams.asString(inputStream)} detected.")
                } else {
                    println("File field $name with file name ${item.name} detected.")
                    inputStream.use {input ->
                        val outputStream = context.openFileOutput(item.name, Context.MODE_PRIVATE)
                        outputStream.use {output ->
                            val buffer = ByteArray(4 * 1024) // buffer size
                            while (true) {
                                val byteCount = input.read(buffer)
                                if (byteCount < 0) break
                                output.write(buffer, 0, byteCount)
                            }
                            output.flush()
                        }
                    }
                }
            }
            return newFixedLengthResponse("success")
        }

        return try {
            val mime = when (uri.substringAfterLast(".")) {
                "ico" -> "image/x-icon"
                "html" -> "text/html"
                "js" -> "application/javascript"
                else -> "text"
            }
            newChunkedResponse(Status.OK, mime, context.assets.open(uri))
        } catch (e: Exception) {
            val message = "Failed to load asset $uri because $e"
            newFixedLengthResponse(message)
        }
    }

    private class NanoHttpdContext(val session: IHTTPSession) : UploadContext {

        override fun getCharacterEncoding(): String {
            return "UTF-8"
        }

        override fun contentLength(): Long {
            val size: Long
            size = try {
                val cl1: String = session.headers["content-length"]!!
                cl1.toLong()
            } catch (var4: NumberFormatException) {
                -1L
            }

            return size
        }

        override fun getContentLength(): Int {
            return contentLength().toInt()
        }

        override fun getContentType(): String {
            return session.headers["content-type"] ?: ""
        }

        override fun getInputStream(): InputStream {
            return session.inputStream
        }

    }

}