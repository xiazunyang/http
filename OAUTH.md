### Oauth授权管理

* 通过`OkHttp`的拦截器实现的Oauth授权管理工具
* 可添加其它头信息到每个请求当中
* 可以服务端返回401时，尝试重新获取token并重试请求

#### 使用方法

* 实现`OauthProvider`接口
* 创建`OauthInterceptor`实例并添加到`OkHttp`中

```kotlin
//1.使用object单例实现OauthProvider接口
object AuthManagement : OauthProvider

    /** 登录成功后，把权限Token保存到此处 */
    override val accessToken: String? = null

    /** 其它要添加的请求头均添加到此处 */
    override val headers = mutableMapOf()

    override fun refreshToken(): String? {
        TODO("获取新的TOKEN，并作为返回值返回")
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
