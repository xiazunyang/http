package cn.numeron.okhttp

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val retryCount: Int = 2) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (exception: IOException) {
            if (isAllowRetry(chain.request())) {
                retry(0, exception, chain)
            } else throw exception
        }
    }

    /** 是否是GET请求 */
    private fun isAllowRetry(request: Request): Boolean {
        if (request.method == "GET") {
            return true
        }
        return request.url.pathSegments.any(::isGetUrl)
    }

    /** 是否是获取数据的url */
    private fun isGetUrl(segment: String) = segment.startsWith("get")

    private fun retry(count: Int, exception: IOException, chain: Interceptor.Chain): Response {
        return if (count < retryCount) {
            try {
                chain.proceed(chain.request())
            } catch (exception: IOException) {
                retry(count + 1, exception, chain)
            }
        } else throw exception
    }

}