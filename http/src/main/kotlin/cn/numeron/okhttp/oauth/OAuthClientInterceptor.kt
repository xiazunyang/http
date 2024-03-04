package cn.numeron.okhttp.oauth

import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

class OAuthClientInterceptor(private val provider: OAuthProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val headers = request.headers
        val requestBuilder = request.newBuilder()
        // 添加授权
        var accessToken = provider.accessToken
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder
                .removeHeader(AUTHORIZATION)
                .addHeader(AUTHORIZATION, "Bearer $accessToken")
        }
        // 添加额外的请求头，不会覆盖已存在的
        val headersNames = headers.names()
        for ((name, value) in provider.headers) {
            if (!headersNames.contains(name)) {
                requestBuilder.addHeader(name, value)
            }
        }
        // 构建请求，发起请求
        request = requestBuilder.build()
        var response = chain.proceed(request)

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            //如果返回了401，则尝试通过provider获取新的token
            accessToken = provider.refreshToken()
            if (!accessToken.isNullOrEmpty()) {
                // 将更新的token保存起来
                provider.accessToken = accessToken
                //如果获取到了新的token，则构建一个新的请求，再次发起
                request = request.newBuilder()
                    .removeHeader(AUTHORIZATION)
                    .addHeader(AUTHORIZATION, "Bearer $accessToken")
                    .build()
                response.close()
                response = chain.proceed(request)
            }
        }
        return response
    }

    private companion object {

        private const val AUTHORIZATION = "Authorization"

    }

}