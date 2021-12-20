package cn.numeron.okhttp.oauth

import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

class OAuthClientInterceptor(private val provider: OAuthProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
            .newBuilder()
            .apply {
                for ((name, value) in provider.headers) {
                    removeHeader(name)
                    addHeader(name, value)
                }
            }
            .build()

        var response = chain.proceed(request)
        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            //如果返回了401，则尝试通过provider获取新的token
            val accessToken = provider.refreshToken()
            if (!accessToken.isNullOrEmpty()) {
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