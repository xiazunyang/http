package cn.numeron.okhttp

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

class ProgressResponseBody(
        private val delegate: ResponseBody,
        private val callback: DlProgressCallback) : ResponseBody() {

    /**
     * 当前已处理的数据大小
     */
    private var readLength = 0L

    private var contentLength = contentLength().toFloat()

    private val source = object : ForwardingSource(delegate.source()) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val length = super.read(sink, byteCount)
            if (contentLength > 0 && length > 0) {
                readLength += length
                callback.update(readLength / contentLength)
            }
            return length
        }
    }.buffer()

    override fun contentLength(): Long = delegate.contentLength()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun source(): BufferedSource = source

    /**
     * 设置已存在的文件长度，以正确的获取下载进度
     */
    internal fun setExistLength(existLength: Long) {
        readLength = existLength
        contentLength += existLength
    }

}