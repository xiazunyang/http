package cn.numeron.okhttp

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/** 从请求头中获取文件名 */
fun Headers.getFileName(): String? {
    val contentDisposition = get("Content-Disposition") ?: return null
    val properties = contentDisposition
        .splitToList(';')
        .associate {
            it.splitToList('=').toPair()
        }
    val encodedFilename = properties["filename*"]
    if (!encodedFilename.isNullOrEmpty()) {
        var (coding, fileName) = encodedFilename.splitToList('\'').toPair()
        if (fileName == null) {
            fileName = coding
            coding = "utf-8"
        }
        try {
            return URLDecoder.decode(fileName, coding)
        } catch (_: UnsupportedEncodingException) {
        }
    }
    val filename = properties["filename"]
    if (!filename.isNullOrEmpty()) {
        return filename
    }
    return null
}

internal fun <T> List<T>.toPair(): Pair<T, T?> {
    return get(0) to getOrNull(1)
}

internal fun String.splitToList(char: Char): List<String> {
    return split(char).map(String::trim).filter(String::isNotEmpty)
}

/**
 * 从下载地址中获取文件名称
 * 优先用filename参数的值作为文件名
 * 其次用name参数的值作为文件名
 * 拿不到则用下载地址的最后一位的pathSegment
 * */
internal fun HttpUrl.getFileName(): String {
    return queryParameter("filename") ?: queryParameter("name") ?: pathSegments.last()
}