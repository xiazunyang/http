package cn.numeron.okhttp

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

internal open class ProgressResponseBody(
    private val delegate: ResponseBody,
    private val callback: DlProgressCallback?
) : ResponseBody() {

    override fun contentLength(): Long = delegate.contentLength()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun source(): BufferedSource {
        return object : ForwardingSource(delegate.source()) {
            val contentLength = contentLength()
            var writtenLength = 0.0
            override fun read(sink: Buffer, byteCount: Long): Long {
                val readLength = super.read(sink, byteCount)
                if (contentLength > 0 && callback != null) {
                    writtenLength += readLength.coerceAtLeast(0)
                    callback.update((writtenLength / contentLength).toFloat())
                }
                return readLength
            }
        }.buffer()
    }

}