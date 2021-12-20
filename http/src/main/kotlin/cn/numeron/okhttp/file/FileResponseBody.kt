package cn.numeron.okhttp.file

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File

class FileResponseBody(val file: File, private val contentType: MediaType?) : ResponseBody() {

    override fun contentLength(): Long = file.length()

    override fun source(): BufferedSource = file.source().buffer()

    override fun contentType(): MediaType? = contentType

}