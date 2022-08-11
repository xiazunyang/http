package cn.numeron.retrofit.url

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

class DynamicUrlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        if (invocation != null) {
            val httpUrl = getNewHttpUrl(invocation, request.url)
            if (httpUrl != null) {
                request = request.newBuilder().url(httpUrl).build()
            }
        }
        return chain.proceed(request)
    }

    private fun getNewHttpUrl(invocation: Invocation, originalHttpUrl: HttpUrl): HttpUrl? {
        val method = invocation.method()
        val isSpecUrl = method.parameterAnnotations.flatten().contains(retrofit2.http.Url())
        if (isSpecUrl) {
            // 如果API方法通过`retrofit2.http.Url`注解指定了访问地址，则不处理
            return null
        }
        var urlAnnotation = method.getAnnotation(Url::class.java)
        var portAnnotation = method.getAnnotation(Port::class.java)
        if (urlAnnotation == null && portAnnotation == null) {
            val klass = method.declaringClass
            urlAnnotation = klass.getAnnotation(Url::class.java)
            portAnnotation = klass.getAnnotation(Port::class.java)
        }
        if (urlAnnotation == null && portAnnotation == null) {
            // 方法和类上均没有注解，则不处理
            return null
        }
        val newBuilder = originalHttpUrl.newBuilder()
        if (urlAnnotation != null) {
            val httpUrl = urlAnnotation.value.toHttpUrl()
            newBuilder.host(httpUrl.host).port(httpUrl.port).scheme(httpUrl.scheme)
        }
        if (portAnnotation != null) {
            // Port和Url可同时存在，但是Url中的端口将不生效。
            newBuilder.port(portAnnotation.value)
        }
        return newBuilder.build()
    }

}