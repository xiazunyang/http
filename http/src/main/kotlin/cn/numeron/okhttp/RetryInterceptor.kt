package cn.numeron.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val retryCount: Int = 2) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (exception: IOException) {
            retry(0, exception, chain)
        }
    }

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