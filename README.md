### http开源工具包

适用于`OkHttp`以及`Retrofit`的一些开源工具，包括文件的上传与下载的进度回调、`Oauth2`的`token`管理、请求与响应记录的日志输出等。

#### 安装方法
当前最新版本号：[![](https://jitpack.io/v/cn.numeron/http.svg)](https://jitpack.io/#cn.numeron/http)

1.  在你的android工程的根目录下的build.gradle文件中的适当的位置添加以下代码：
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
2.  在你的android工程中对应的android模块的build.gradle文件中的适当位置添加以下代码：
```groovy
implementation 'cn.numeron:http:latest_version'
```

#### 进度回调和断点续传
* 通过`OkHttp`的拦截器实现的下载、上传进度监听功能
* 同时支持断点续传
* 免去IO的操作过程
* 支持`OkHttp`和`Retrofit`

[点击此处](https://github.com/xiazunyang/http/blob/master/PROGRESS.md) 查看文档

#### Oauth授权管理
* 适用于`Oauth2`授权的APP端token管理工具
* 可添加其它请求头
* 可在服务端返回401响应码时尝试刷新token

[点击此处](https://github.com/xiazunyang/http/blob/master/OAUTH.md) 查看文档

#### 网络连接失败重试拦截器
* 可在网络连接超时、网络不稳定导致的错误时，重新发起连接请求。
* 自定义重试次数。
* 使用方法：在构建`OkHttpClient`实例时，添加`RetryInterceptor`实例即可。

#### Http请求响应日志输出工具
* 其实就是`HttpLoggingInterceptor`，但是解决了在传输非文本数据时，日志输出乱码的问题。
* 解决了`HttpLoggingInterceptor`导致的上传、下载文件时无法触发回调的问题。
* 使用方法：在构建`OkHttpClient`实例时，添加`TextLogInterceptor`实例即可。

### Retrofit 动态Url方案
* 使用`Port`或`Url`注解为Api指定访问端口或地址。
* 在初始化`OkHttpClient`时，添加`RespecifyUrlInterceptor`拦截器，并放在靠前的位置。
* 示例：
    ```kotlin
    /** 此接口下所有的方法均通过指定的url地址访问，优先级低于方法上的注解 */
    @Url("http://192.168.1.111:8081/")
    interface LoginApi {
        
        /** 指定此方法在调用时，访问服务器的8080端口 */
        @Port(8080)
        @POST("api/user/login")
        suspend fun login(@Body payload: LoginPayload): LoginResponse
        
        /** 指定此方法在调用时，访问指定url地址 */
        @Url("http://192.168.1.111:8081/")
        @POST("api/user/login")
        suspend fun logout(@Body payload: LoginPayload): LogoutResponse
        
    }
    ```
