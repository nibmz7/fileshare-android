package com.nibmz7gmail.fileshare.server

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import com.nibmz7gmail.fileshare.AppExecutors
import com.nibmz7gmail.fileshare.R
import com.nibmz7gmail.fileshare.model.ServerEvent
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.content.ContentType
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newChunkedResponse
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*


class Server(private val context: Application) : NanoHTTPD(45635) {

    private var myHostName: String = "Anon"
    private val eventsLock = Any()
    private val eventsSocket = SseSocket(this)

    companion object {
        const val STOP_SERVER = 2
        const val SERVER_STARTED = 3

        private val LOCK = Any()
        private var instance: Server? = null

        fun getInstance(context: Context): Server {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = Server(context.applicationContext as Application)
                    Timber.e("SERVER CREATED")
                }
                return instance as Server
            }
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
            if (isAlive) {
                Timber.e("Server is already listening on $listeningPort")
                EventEmitter.setStatus(ServerEvent.Success(SERVER_STARTED))
                return
            }
            start(SOCKET_READ_TIMEOUT, false)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun sendMessage(message: String) {
        synchronized(eventsLock) {
            eventsSocket.fireEvent(message)
        }
    }

    override fun start(timeout: Int, daemon: Boolean) {
        super.start(timeout, daemon)
        if (wasStarted()) {
            Timber.i("Listening on port $listeningPort")
            EventEmitter.setStatus(ServerEvent.Success(SERVER_STARTED))
        }
    }

    override fun stop() {
        super.stop()
        if (!isAlive) Timber.e("Server has been stopped")
    }

    @MainThread
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.removePrefix("/").ifEmpty { "index.html" }

        if(uri.substringBeforeLast("/") == "download") {
            val fileInputStream = context.resources.openRawResource(R.raw.cover)
            return newChunkedResponse(Status.OK, "application/octet-stream", fileInputStream)
        }

        if (uri == "message") {

            if (session.method == Method.OPTIONS) {
                val response = newFixedLengthResponse("")
                response.addHeader("Connection", "keep-alive")
                response.addHeader("Access-Control-Allow-Origin", "*")
                response.addHeader("Access-Control-Allow-Methods", "PUT")
                response.addHeader("Access-Control-Max-Age", "86400")
                response.addHeader("Access-Control-Allow-Headers", "*")
                return response
            }
            val map = HashMap<String, String>()
            session.parseBody(map)
            val jsonData = map["postData"] ?: return newFixedLengthResponse(
                Status.BAD_REQUEST,
                MIME_PLAINTEXT,
                "BAD BAD"
            )
            AppExecutors.networkIO().execute {
                synchronized(eventsLock) {
                    eventsSocket.fireEvent(jsonData)
                }
            }
            val response = newFixedLengthResponse("Success")
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        if (uri == "events") {
            synchronized(eventsLock) {
                return eventsSocket.createSseResponse(myHostName)
            }
        }

        if (uri == "upload") {
            if (session.method == Method.OPTIONS) {
                val response = newFixedLengthResponse("")
                response.addHeader("Connection", "keep-alive")
                response.addHeader("Access-Control-Allow-Origin", "*")
                response.addHeader("Access-Control-Allow-Methods", "PUT")
                response.addHeader("Access-Control-Max-Age", "86400")
                response.addHeader("Access-Control-Allow-Headers", "*")
                response.addHeader("Content-Type", "")
                return response
            }
            val contentType = ContentType(session.headers.get("content-type"))
            if(!contentType.isMultipart) {
                val message = "Invalid mime type. Multipart/form-data required"
                return newFixedLengthResponse(message)
            }
            val fileUpload = ServletFileUpload()
            val iter: FileItemIterator = fileUpload.getItemIterator(NanoHttpdContext(session))
            while (iter.hasNext()) {
                val item: FileItemStream = iter.next()
                val name = item.fieldName
                val inputStream: InputStream = item.openStream()

                if (item.isFormField) {
                    println("Form field $name with value ${Streams.asString(inputStream)} detected.")
                } else {
                    println("File: ${item.name} detected.")
                    val fileDir = File(context.filesDir, "server/files")
                    fileDir.mkdirs()
                    inputStream.use { input ->
                        val outputStream = FileOutputStream(File(fileDir, item.name))
                        outputStream.use { output ->
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
            val response = newFixedLengthResponse("files uploaded")
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        return try {
            val mime = when (uri.substringAfterLast(".")) {
                "ico" -> "image/x-icon"
                "html" -> "text/html"
                "js" -> "application/javascript"
                else -> "text"
            }
            val resId: Int = when(uri) {
                "index.html" -> R.raw.index
                "main.js" -> R.raw.main
                else -> R.raw.index
            }
            newChunkedResponse(Status.OK, mime, context.resources.openRawResource(resId))
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