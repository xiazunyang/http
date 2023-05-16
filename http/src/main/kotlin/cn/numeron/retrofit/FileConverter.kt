package cn.numeron.retrofit

import cn.numeron.okhttp.file.FileResponseBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.File
import java.lang.reflect.Type

/**
 * 当使用Retrofit下载文件时，使用此转换器后，可以在Api接口中声明File类型的返回值。
 */

class FileConverter : Converter<ResponseBody, File> {

    override fun convert(value: ResponseBody): File {
        return getFile(value)
    }

    private fun getFile(responseBody: ResponseBody): File {
        return if (responseBody is FileResponseBody) {
            responseBody.file
        } else try {
            val declaredField = responseBody::class.java.getDeclaredField("delegate")
            declaredField.isAccessible = true
            val delegate = declaredField.get(responseBody) as ResponseBody
            getFile(delegate)
        } catch (throwable: Throwable) {
            throw RuntimeException("响应体中没有记录文件信息！或者没有使用Tag标记File类型的参数！", throwable)
        }
    }

    class Factory : Converter.Factory() {

        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            if (type == File::class.java) {
                return FileConverter()
            }
            return null
        }

    }

}