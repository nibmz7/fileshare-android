package com.nibmz7gmail.fileshare

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import fi.iki.elonen.NanoHTTPD
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webServer = WebServer(this)
        webServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    class WebServer(private val context: Context) : NanoHTTPD(8080) {

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
                newChunkedResponse(Response.Status.OK, mime, context.assets.open("$uri"))
            } catch (e: Exception) {
                val message = "Failed to load asset $uri because $e"
                newFixedLengthResponse(message)
            }
        }

        class NanoHttpdContext(val session: IHTTPSession) : UploadContext {

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
}
