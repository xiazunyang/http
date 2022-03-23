package cn.numeron.okhttp.file

import cn.numeron.okhttp.getFileName
import cn.numeron.okhttp.getTag
import com.j256.simplemagic.ContentType
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.RandomAccessFile

class BreakpointResumeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        //获取原始请求
        var request = chain.request()
        //取出文件参数
        val fileOrDir = request.getTag<File>()
        //取出下载进度回调参数
        val dlProgressCallback = request.getTag<DlProgressCallback>()
        //获取原请求
        var response = chain.proceed(request)
        if (fileOrDir == null) {
            //如果没有文件参数，则直接返回该响应
            return response
        }
        //获取响应体的信息
        var responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val contentType = responseBody.contentType()
        //判断要保存到哪个位置
        val file = getStoredFile(fileOrDir, response, request)
        //检测、创建存放文件夹
        val parentDirectory = file.parentFile
        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs()
        }

        var existLength = file.length()
        //如果文件存在，并且与要下载的文件一致，则直接返回
        if (contentLength > 0 && existLength == contentLength) {
            val fileResponseBody = FileResponseBody(file, contentType)
            return response.newBuilder().body(fileResponseBody).build()
        }

        //如果未能获取到contentLength，或者已存在的文件大于contentLength
        if (contentLength == -1L || existLength > contentLength) {
            //处理文件名重复的错误文件
            if (file.exists()) {
                file.delete()
            }
            //因为已经已删除，所以要将此变量置为0
            existLength = 0
        }
        //如果文件已存在一部分，则重新发起请求，获取其余数据
        if (existLength > 0) {
            request = request.newBuilder()
                .removeHeader("range")
                .addHeader("range", "bytes=${existLength}-")
                .build()
            //获取剩余数据的请求体
            response.closeQuietly()
            val rangeResponse = chain.proceed(request)
            if(rangeResponse.code == 200) {
                //如果服务器返回了剩余的部分数据，则关闭之前的响应
                responseBody = response.body!!
            } else {
                //否则只能继续用原来的响应来写入
                responseBody.source().skip(existLength)
            }
            if (dlProgressCallback != null) {
                //构建有进度回调的请求体
                responseBody = ProgressResponseBody(responseBody, dlProgressCallback)
                //把已有的部分，算作已下载的进度，以处理正确的进度
                responseBody.setExistLength(existLength)
            }
        }

        //将请求体中的数据定入到文件中
        responseBody.writeTo(file, existLength)

        //写完文件数据后，构建一个新的回调并返回
        val fileResponseBody = FileResponseBody(file, contentType)
        return response.newBuilder().body(fileResponseBody).build()
    }

    /**
     * 使用RandomAccessFile将数据写入到文件
     */
    private fun ResponseBody.writeTo(file: File, existLength: Long) {
        //使用RandomAccessFile将数据写入到文件
        val outputFile = RandomAccessFile(file, "rws")
        if (existLength > 0) {
            outputFile.seek(existLength)
        }
        //执行写入操作
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var readLength = source().read(buffer)
        while (readLength > 0) {
            outputFile.write(buffer, 0, readLength)
            readLength = source().read(buffer)
        }
        outputFile.closeQuietly()
        closeQuietly()
    }

    /**
     * 根据[fileOrDir]的类型、响应信息以及请求信息中判断文件名及保存位置
     */
    private fun getStoredFile(fileOrDir: File, response: Response, request: Request): File {
        return if (fileOrDir.isFile || !fileOrDir.exists() && fileOrDir.extension.isNotEmpty()) {
            //如果是一个文件，或者文件不存在并且有扩展名，则将其作为保存数据的文件
            fileOrDir
        } else {
            //否则就是存放的目录，获取文件名并在该目录下创建文件
            var fileName = response.headers.getFileName() ?: request.url.getFileName()
            var extension: String? = fileName.substringAfterLast('.', "")
            val hasNotExtension = extension.isNullOrEmpty()
            if (extension.isNullOrEmpty()) {
                //如果获取到的文件名没有扩展名，则尝试通过Content-Type的内容推断出扩展名
                val mimeType = response.header("Content-Type")
                if (!mimeType.isNullOrEmpty()) {
                    val contentType = ContentType.fromMimeType(mimeType)
                    extension = contentType.fileExtensions.firstOrNull()
                }
            }
            if (hasNotExtension && !extension.isNullOrEmpty()) {
                fileName += ".$extension"
            }
            File(fileOrDir, fileName)
        }
    }

}