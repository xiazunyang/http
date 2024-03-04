package cn.numeron.okhttp.log

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okhttp3.internal.platform.Platform
import okio.Buffer
import okio.GzipSource
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

/**
 * 从[okhttp3.logging.HttpLoggingInterceptor]抄来的代码
 * 主要改变是当传输的数据是非文本数据时，不会显示请求体与响应体。
 * 以解决下载文件时遇到的问题。
 * */
class TextLogInterceptor @JvmOverloads constructor(
    private val logger: Logger = Logger.DEFAULT
) : Interceptor {

    @Volatile
    private var headersToRedact = emptySet<String>()

    @Volatile
    @set:JvmName("requestLevel")
    var requestLevel = LogLevel.NONE

    @Volatile
    @set:JvmName("responseLevel")
    var responseLevel = LogLevel.NONE

    fun interface Logger {
        fun log(message: String)

        companion object {
            /** A [Logger] defaults output appropriate for the current platform. */
            @JvmField
            val DEFAULT: Logger = object : Logger {
                override fun log(message: String) {
                    Platform.get().log(message, Platform.INFO, null)
                }
            }
        }
    }

    fun setRequestLevel(level: LogLevel): TextLogInterceptor = apply {
        this.requestLevel = level
    }

    fun setResponseLevel(level: LogLevel): TextLogInterceptor = apply {
        this.responseLevel = level
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestLevel = this.requestLevel
        val responseLevel = this.responseLevel

        val request = chain.request()
        if (requestLevel == LogLevel.NONE && responseLevel == LogLevel.NONE) {
            return chain.proceed(request)
        }

        val logRequestBody = requestLevel == LogLevel.BODY
        val logRequestHeaders = logRequestBody || requestLevel == LogLevel.HEADERS

        val requestBody = request.body

        val isTextRequestBody = requestBody?.contentType().isTextContent()

        val connection = chain.connection()
        var requestStartMessage =
            ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logRequestHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        logger.log(requestStartMessage)

        if (logRequestHeaders) {
            val headers = request.headers

            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. When not
                // already present, force them to be included (if available) so their values are known.
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        logger.log("Content-Type: $it")
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        logger.log("Content-Length: ${requestBody.contentLength()}")
                    }
                }
            }

            for (i in 0 until headers.size) {
                logHeader(headers, i)
            }

            if (!logRequestBody || requestBody == null) {
                logger.log("--> END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                logger.log("--> END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                logger.log("--> END ${request.method} (duplex request body omitted)")
            } else if (!isTextRequestBody) {
                logger.log("--> END ${request.method} (non text request body omitted.)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                logger.log("")
                if (buffer.isProbablyUtf8()) {
                    logger.log(buffer.readString(charset))
                    logger.log("--> END ${request.method} (${requestBody.contentLength()}-byte body)")
                } else {
                    logger.log(
                        "--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)"
                    )
                }
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.log("<-- HTTP FAILED: $e")
            throw e
        }

        val logResponseBody = responseLevel == LogLevel.BODY
        val logResponseHeaders = logResponseBody || responseLevel == LogLevel.HEADERS

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        logger.log(
            "<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logRequestHeaders) ", $bodySize body" else ""})"
        )

        val isTextResponseBody = responseBody.contentType().isTextContent()

        if (logResponseHeaders) {
            val headers = response.headers
            for (i in 0 until headers.size) {
                logHeader(headers, i)
            }

            if (!logResponseBody || !response.promisesBody()) {
                logger.log("<-- END HTTP")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                logger.log("<-- END HTTP (encoded body omitted)")
            } else if (!isTextResponseBody) {
                logger.log("<-- END HTTP (non text response body omitted.)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                var buffer = source.buffer

                var gzippedLength: Long? = null
                if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                    gzippedLength = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                }

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                if (!buffer.isProbablyUtf8()) {
                    logger.log("")
                    logger.log("<-- END HTTP (binary ${buffer.size}-byte body omitted)")
                    return response
                }

                if (contentLength != 0L) {
                    logger.log("")
                    logger.log(buffer.clone().readString(charset))
                }

                if (gzippedLength != null) {
                    logger.log("<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
                } else {
                    logger.log("<-- END HTTP (${buffer.size}-byte body)")
                }
            }
        }

        return response
    }

    private fun logHeader(headers: Headers, i: Int) {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
        logger.log(headers.name(i) + ": " + value)
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private companion object {

        fun MediaType?.isTextContent(): Boolean {
            return this?.type == "text" || this?.subtype == "json" || this?.subtype == "x-www-form-urlencoded"
        }

        fun Buffer.isProbablyUtf8(): Boolean {
            try {
                val prefix = Buffer()
                val byteCount = size.coerceAtMost(64)
                copyTo(prefix, 0, byteCount)
                for (i in 0 until 16) {
                    if (prefix.exhausted()) {
                        break
                    }
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                return true
            } catch (_: EOFException) {
                return false
            }
        }
    }

}
