package cn.numeron.okhttp.file

import cn.numeron.okhttp.getTag
import okhttp3.*
import java.io.File

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
        return response
    }

}