package cn.numeron.okhttp

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val callback: UpProgressCallback?,
) : RequestBody() {

    override fun isDuplex(): Boolean = delegate.isDuplex()

    override fun isOneShot(): Boolean = delegate.isOneShot()

    override fun contentLength(): Long = delegate.contentLength()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun writeTo(sink: BufferedSink) {
        val contentLength = contentLength()
        object : ForwardingSink(sink) {
            var writtenLength = 0.0
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                writtenLength += byteCount.coerceAtLeast(0)
                callback?.update((writtenLength / contentLength).toFloat())
            }
        }.buffer().let {
            delegate.writeTo(it)
            it.flush()
        }
    }

}