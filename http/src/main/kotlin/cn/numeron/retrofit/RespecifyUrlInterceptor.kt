package cn.numeron.retrofit

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

class RespecifyUrlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        if (invocation != null) {
            val httpUrl = getNewHttpUrl(invocation, request.url)
            request = request.newBuilder().url(httpUrl).build()
        }
        return chain.proceed(request)
    }

    private fun getNewHttpUrl(invocation: Invocation, originalHttpUrl: HttpUrl): HttpUrl {
        val method = invocation.method()
        var urlAnnotation = method.getAnnotation(Url::class.java)
        var portAnnotation = method.getAnnotation(Port::class.java)
        if (urlAnnotation == null && portAnnotation == null) {
            val klass = method.declaringClass
            urlAnnotation = klass.getAnnotation(Url::class.java)
            portAnnotation = klass.getAnnotation(Port::class.java)
        }
        return when {
            urlAnnotation != null -> {
                val url = urlAnnotation.value.toHttpUrl()
                originalHttpUrl.newBuilder().host(url.host).port(url.port).build()
            }
            portAnnotation != null -> {
                val port = portAnnotation.value
                originalHttpUrl.newBuilder().port(port).build()
            }
            else -> {
                originalHttpUrl
            }
        }
    }

}