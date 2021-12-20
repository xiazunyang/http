### Oauth授权管理

* 通过`OkHttp`的拦截器实现的Oauth授权管理工具
* 可添加其它头信息到每个请求当中
* 可以服务端返回401时，尝试重新获取token并重试请求

#### 使用方法

* 实现`OauthProvider`接口
* 创建`OauthInterceptor`实例并添加到`OkHttp`中

```kotlin
//1.使用object单例实现OauthProvider接口
object AuthManagement : OauthProvider {

    private const val KEY_ACCESS_TOKEN = "Authorization"

    private val headersMap = mutableMapOf()

    override val headers: Map<String, String>
        get() = headersMap

    override fun refreshToken(): String? {
        TODO("获取新的TOKEN，并作为返回值返回")
    }

    fun setAccessToken(accessToken: String?) {
        if (accessToken.isNullOrEmpty()) {
            headersMap.remove(KEY_ACCESS_TOKEN)
        } else {
            headersMap[KEY_ACCESS_TOKEN] = "Bearer $accessToken"
        }
    }

}

//2.创建OAuthClientInterceptor的实例
val oauthClientInterceptor = OAuthClientInterceptor(AuthManagement)

//3.添加到OkHttp中
val okHttpClient = OkHttpClient.Builder()
    ...
    .addInterceptor(oauthClientInterceptor)
    ...
    .build()
```
