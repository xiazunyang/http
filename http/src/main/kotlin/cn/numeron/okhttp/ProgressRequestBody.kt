package cn.numeron.okhttp

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*

class ProgressRequestBody(
        private val delegate: RequestBody,
        private val callback: UpProgressCallback,
) : RequestBody() {

    private var writtenLength = 0L

    private var contentLength = contentLength().toFloat()

    override fun isDuplex(): Boolean = delegate.isDuplex()

    override fun isOneShot(): Boolean = delegate.isOneShot()

    override fun contentLength(): Long = delegate.contentLength()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun writeTo(sink: BufferedSink) {
        object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                writtenLength += byteCount
                callback.update(writtenLength / contentLength)
            }
        }.buffer().let {
            delegate.writeTo(it)
            it.close()
        }
    }

    fun setExistLength(existLength: Long) {
        writtenLength = existLength
        contentLength += existLength
    }

}