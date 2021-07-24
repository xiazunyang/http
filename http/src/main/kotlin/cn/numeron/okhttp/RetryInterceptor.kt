package cn.numeron.okhttp

import okhttp3.Interceptor
import okhttp3.Response
import java.lang.Exception
import java.net.ConnectException
import java.net.SocketTimeoutException

class RetryInterceptor(private val retryCount: Int = 2) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            chain.proceed(chain.request())
        } catch (exception: SocketTimeoutException) {
            retry(0, exception, chain)
        } catch (exception: ConnectException) {
            retry(0, exception, chain)
        }
    }

    private fun retry(count: Int, exception: Exception, chain: Interceptor.Chain): Response {
        return if (count < retryCount) {
            try {
                chain.proceed(chain.request())
            } catch (exception: SocketTimeoutException) {
                retry(count + 1, exception, chain)
            } catch (exception: ConnectException) {
                retry(count + 1, exception, chain)
            }
        } else {
            throw exception
        }
    }

}