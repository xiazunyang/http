package cn.numeron.okhttp

import cn.numeron.getContentLength
import cn.numeron.getContentType
import cn.numeron.getFileName
import cn.numeron.getTag
import okhttp3.*
import okhttp3.internal.closeQuietly
import okio.BufferedSource
import java.io.File
import java.io.RandomAccessFile

class ProgressInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        //获取原始请求
        var request = chain.request()
        //取出进度回调
        val dlProgressCallback = request.getTag<DlProgressCallback>()
        val upProgressCallback = request.getTag<UpProgressCallback>()
        //取出文件
        val fileOrPath = request.getTag<File>()

        if (upProgressCallback != null && request.body != null) {
            //如果有上传进度回调，并且请求体不为空，则重新构建request实例，以实现进度回调
            val progressRequestBody = ProgressRequestBody(request.body!!, upProgressCallback)
            request = request.newBuilder()
                .method(request.method, progressRequestBody)
                .build()
        }

        return if (fileOrPath == null && dlProgressCallback == null) {
            //如果没有文件参数也没有回调参数，则当普通请求处理
            chain.proceed(request)
        } else if (fileOrPath != null) {
            //使用HEAD方式请求API，获取文件大小、类型以及名称等数据
            val response = chain.proceed(request.newBuilder().head().build())
            //如果有文件参数，则当作下载文件处理
            val headers = response.headers
            val contentLength = headers.getContentLength()
            val contentType = headers.getContentType()
            //判断要保存到哪个位置
            val file = if (fileOrPath.extension.isNotEmpty()) {
                fileOrPath
            } else {
                val fileName = headers.getFileName() ?: request.url.getFileName()
                File(fileOrPath, fileName)
            }
            //检测、创建存放文件的文件夹
            val fileDir = file.parentFile
            if (fileDir != null && !fileDir.exists()) {
                fileDir.mkdirs()
            }
            //获取已保存的文件的大小
            val existLength = file.length()
            request = when {
                //如果文件存在，并且体积相等，直接返回
                contentLength > 0 && existLength == contentLength -> {
                    response.closeQuietly()
                    val fileResponseBody = FileResponseBody(file, contentType)
                    return response.newBuilder().body(fileResponseBody).build()
                }
                //存在重复名称的错误文件、或未获取到文件大小时，需要重新获取完整的数据
                existLength > contentLength || contentLength == -1L -> {
                    if (file.exists()) {
                        file.delete()
                    }
                    response.closeQuietly()
                    request.newBuilder().get().build()
                }
                //文件存在，并且体积小于contentLength，则重新发起请求，获取其余数据
                else -> {
                    //关闭响应体，释放资源
                    response.closeQuietly()
                    request.newBuilder()
                        .get()
                        .removeHeader("range")
                        .addHeader("range", "bytes=${existLength}-")
                        .build()
                }
            }
            //获取数据流
            val inputSource = chain.proceed(request).body!!.source()
            inputSource.writeToFile(file, contentLength, dlProgressCallback)
            //构建新的响应体并返回
            val fileResponseBody = FileResponseBody(file, contentType)
            return response.newBuilder().body(fileResponseBody).build()
        } else {
            //如果只有进度回调，构建有进度回调的响应
            val response = chain.proceed(request)
            val responseBody = ProgressResponseBody(response.body!!, dlProgressCallback)
            return response.newBuilder().body(responseBody).build()
        }
    }

    private fun BufferedSource.writeToFile(file: File, length: Long, dlProgressCallback: DlProgressCallback?) {
        //要写入的文件
        val outputFile = RandomAccessFile(file, "rws")
        outputFile.seek(file.length())
        //把数据写入文件
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var readLength = read(buffer)
        while (readLength > 0) {
            outputFile.write(buffer, 0, readLength)
            if (length > 0 && dlProgressCallback != null) {
                val progress = outputFile.length().toDouble() / length
                dlProgressCallback.update(progress.toFloat())
            }
            readLength = read(buffer)
        }
        outputFile.closeQuietly()
        closeQuietly()
    }

}