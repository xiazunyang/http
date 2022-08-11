package cn.numeron.retrofit

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

class DynamicTimeoutInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        if (invocation != null) {
            val method = invocation.method()
            val requestTimeoutAnnotation = method.getAnnotation(RequestTimeout::class.java)
                ?: method.declaringClass.getAnnotation(RequestTimeout::class.java)
            if (requestTimeoutAnnotation != null) {
                val value = requestTimeoutAnnotation.value
                val unit = requestTimeoutAnnotation.unit
                val readValue = getScopeValue(requestTimeoutAnnotation.read, value)
                val writeValue = getScopeValue(requestTimeoutAnnotation.write, value)
                val connectValue = getScopeValue(requestTimeoutAnnotation.connect, value)

                var newChain: Interceptor.Chain = chain

                if (readValue > -1) {
                    // 如果有设置全局超时时间或读取超时时间，则创建新的Interceptor.Chain
                    newChain = newChain.withReadTimeout(readValue, unit)
                }
                if (writeValue > -1) {
                    // 如果有设置全局超时时间或写入超时时间，则创建新的Interceptor.Chain
                    newChain = newChain.withWriteTimeout(writeValue, unit)
                }
                if (connectValue > -1) {
                    // 如果有设置全局超时时间或连接超时时间，则创建新的Interceptor.Chain
                    newChain = newChain.withConnectTimeout(connectValue, unit)
                }
                if (newChain !== chain) {
                    // 如果创建了新的Interceptor.Chain，则用它发起请求
                    return newChain.proceed(request)
                }
            }
        }
        return chain.proceed(request)
    }

    private fun getScopeValue(scopeValue: Int, globalValue: Int): Int {
        if (scopeValue > -1) {
            return scopeValue
        }
        return globalValue
    }

}