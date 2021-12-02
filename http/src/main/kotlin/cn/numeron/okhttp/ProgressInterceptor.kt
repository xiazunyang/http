package cn.numeron.okhttp

import cn.numeron.getFileName
import cn.numeron.getTag
import okhttp3.*
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.RandomAccessFile

/**
 * 用于支持上传或下载（支持断点续传）功能、并且支持进度回调的拦截器
 * 适用于OkHttp以及Retrofit。
 * 只需要在构造Request实例时：
 *   通过tag方法添加[UpProgressCallback]接口的实例以支持上传进度回调
 *   通过tag方法添加[DlProgressCallback]接口的实例以支持下载进度回调
 *   任意http请求均可添加进度回调，非下载、上传文件时也可以。
 *   ```
 *   Request.Builder()
 *      .tag(DlProgressCallback::class.java, DlProgressCallback { progress: Float ->
 *          //处理下载进度
 *      })
 *      .tag(UpProgressCallback::class.java, UpProgressCallback { progress: Float ->
 *          //处理上传进度
 *      }
 *      ...
 *   ```
 *
 * 如果需要断点续传，需要通过tag方法添加[File]类型的实例来触发断点续传逻辑
 * 断点续传功能遵循以下逻辑：
 *   当[File]实例是文件时，会将下载的数据写入到该文件
 *   当[File]实例是文件夹时，会自动从响应信息中获取文件名，并在该[File]实例的路径下创建新的文件
 *   当文件存在时，会根据该文件的大小判断是否是符合要求的文件，如果符合则不重复下载，直接返回该数据
 *   当文件数据不符合响应中的数据时，会重新下载全新的数据，并覆盖原文件的数据
 *   当文件体积小于响应中的大小时，会从剩余的部分(响应大小-文件大小)开始继续下载，并续写到文件
 * 使用此文件下载文件后，在获取到[Response]实例后，不需要再对[ResponseBody]的数据进行操作
 * 可将[ResponseBody]强转为[FileResponseBody]，然后从中取出[File]实例既是下载完成的文件。
 *
 */
class ProgressInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        //获取原始请求
        var request = chain.request()
        //取出进度回调参数
        val upProgressCallback = request.getTag<UpProgressCallback>()
        val dlProgressCallback = request.getTag<DlProgressCallback>()
        //取出文件参数
        val fileOrPath = request.getTag<File>()
        if (upProgressCallback != null && request.body != null) {
            //如果有上传进度回调，并且有请求体，则构建新的请求体实例，以监听进度回调
            val progressRequestBody = ProgressRequestBody(request.body!!, upProgressCallback)
            if (upProgressCallback is ContinuableUpProgressCallback) {
                //如果回调支持断点续传，则设置已上传的数据长度
                val existLength = upProgressCallback.existLength
                progressRequestBody.setExistLength(existLength)
            }
            request = request.newBuilder()
                .method(request.method, progressRequestBody)
                .build()
        }
        //获取原始响应
        var response = chain.proceed(request)
        if (dlProgressCallback != null && response.body != null) {
            //如果有下载进度回调，并且有响应体，则构建新的响应体以监听进度回调
            val progressResponseBody = ProgressResponseBody(response.body!!, dlProgressCallback)
            response = response.newBuilder()
                .body(progressResponseBody)
                .build()
        }

        if (fileOrPath == null) {
            //如果没有文件参数，则直接返回该响应
            return response
        } else {
            //获取响应体的信息
            var responseBody = response.body!!
            val contentLength = responseBody.contentLength()
            val contentType = responseBody.contentType()
            //判断要保存到哪个位置
            val file = getStoredFile(fileOrPath, response, request)
            //检测、创建存放文件夹
            val parentFile = file.parentFile
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs()
            }

            var existLength = file.length()
            //如果文件存在，并且与要下载的文件一致，则直接返回
            if (contentLength > 0 && existLength == contentLength) {
                response.closeQuietly()
                val fileResponseBody = FileResponseBody(file, contentType)
                return response.newBuilder().body(fileResponseBody).build()
            }

            //如果未能获取到contentLength，或者已存在的文件大于contentLength
            if (contentLength == -1L || existLength > contentLength) {
                //处理文件名重复的错误文件
                if (file.exists()) {
                    file.delete()
                }
                //因为已经已删除，所以要将此变量置0
                existLength = 0
            }
            //如果文件已存在一部分，则重新发起请求，获取其余数据
            if (existLength > 0) {
                response.closeQuietly()
                val rangeRequest = request.newBuilder()
                    .removeHeader("range")
                    .addHeader("range", "bytes=${existLength}-").build()
                //获取剩余数据的请求体
                responseBody = chain.proceed(rangeRequest).body!!
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
     * 根据[fileOrPath]的类型、响应信息以及请求信息中判断文件名及保存位置
     */
    private fun getStoredFile(fileOrPath: File, response: Response, request: Request): File {
        return if (fileOrPath.isFile || fileOrPath.extension.isNotEmpty()) {
            //如果是一个文件，或者文件名有扩展名，则将其作为保存数据的文件
            fileOrPath
        } else {
            //否则就是存放的目录，获取文件名并在该目录下创建文件
            val fileName = response.headers.getFileName() ?: request.url.getFileName()
            File(fileOrPath, fileName)
        }
    }

}