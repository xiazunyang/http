package cn.numeron

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import java.net.URLDecoder

/** 从请求实例中获取用[Tag]注解标记的参数 */
internal inline fun <reified T> Request.getTag(): T? {
    return tag(T::class.java)
}

/** 从请求头中获取文件大小 */
internal fun Headers.getContentLength(): Long {
    return get("Content-Length")?.toLong() ?: -1L
}

/** 从请求头中获取文件类型 */
internal fun Headers.getContentType(): MediaType? {
    return get("Content-Type")?.toMediaTypeOrNull()
}

/** 从请求头中获取文件名 */
internal fun Headers.getFileName(): String? {
    return get("Content-Disposition")
        ?.split(';')
        ?.let { list ->
            list.find {
                it.contains("filename*")
            } ?: list.find {
                it.contains("filename")
            }
        }
        ?.split('=')
        ?.component2()
        ?.removeSurrounding("\"")
        ?.let {
            if (it.contains('\'')) {
                //如果有'字符，则取出编码格式与字符串，解码后返回
                val split = it.split('\'').filter(String::isNotBlank)
                URLDecoder.decode(split.component2(), split.component1())
            } else {
                it  //否则直接返回字符串
            }
        }
}

/**
 * 从下载地址中获取文件名称
 * 优先用filename参数的值作为文件名
 * 拿不到则用下载地址的最后一位的pathSegment
 * */
internal fun HttpUrl.getFileName(): String {
    return queryParameter("filename") ?: pathSegments.last()
}