### 下载/上传进度监听
* 通过`OkHttp`的拦截器实现的下载、上传进度监听功能，同时支持`OkHttp`和`Retrofit`。

### `OkHttpClient`的使用方法

##### 安装方法
* 将 [`ProgressInterceptor`](https://github.com/xiazunyang/http/blob/master/http/src/main/kotlin/cn/numeron/okhttp/file/ProgressInterceptor.kt)
  添加到`OkHttpClient`中即可。

##### 监听下载进度
1. 在构建`Request`对象时，构建一个`DlProgressCallback`实例，并通过`tag(Class<T>, T)`方法添加到`Request.Builder`中。
2. 在下载文件或请求网络时，服务器数据传输到本地时会调用该回调实例的`update`方法，参数是一个`float`值，可通过`(progress * 100).toInt()`来得到下载进度的百分比。
3. 注意：`update`方法运行在子线程中（与`Interceptor.intercept`方法的调用线程一致）。
```kotlin
val request = Request.Builder()
    .tag(DlProgressCallback::class.java, DlProgressCallback { progress ->
        val percent = (progress * 100).toInt()
        TODO("处理下载进度监听")
    })
    ...
```

##### 监听上传进度
1. 上传进度则构建一个`UpProgressCallback`实例，并通过`tag(Class<T>, T)`方法添加到`Request.Builder`中。 
2. 在上传文件或请求网络时，本地数据传输到服务器时会调用该回调实例的`update`方法，参数是一个`float`值，可通过`(progress * 100).toInt()`来得到下载进度的百分比。
3. 注意：`update`方法运行在子线程中（与`Interceptor.intercept`方法的调用线程一致）。
```kotlin
val request = Request.Builder()
    .tag(UpProgressCallback::class.java, UpProgressCallback { progress ->
        val percent = (progress * 100).toInt()
        TODO("处理上传进度监听")
    })
    ...
```

### `OkHttpClient`的使用方法

#### `Retrofit`的安装方法
* 参考`OkHttpClient`的安装方法，向`OkHttpClient`中添加[`ProgressInterceptor`](https://github.com/xiazunyang/http/blob/master/http/src/main/kotlin/cn/numeron/okhttp/file/ProgressInterceptor.kt)监听器，并添加到`Retrofit`中。

##### 监听下载进度
1. 在声明的接口中添加`DlProgressCallback`类型的参数，并标记`@Tag`注解。
2. 在调用该接口时，创建`DlProgressCallback`实例，并作为参数传递给该接口中即可。
```kotlin
/** 
 * url 文件的下载地址
 * callback 下载进度的监听接口
 * */
@GET
@Streaming
suspend fun download(@Url url: String, @Tag callback: DlProgressCallback): Call<ResponseBody>
```

##### 监听上传进度
1. 在声明的接口中添加`UpProgressCallback`类型的参数，并标记`@Tag`注解。
2. 在调用该接口时，创建`UpProgressCallback`实例，并作为参数传递给该接口中即可。

```kotlin
@POST
@Streaming
suspend fun upload(@Url url: String, @Tag callback: UpProgressCallback): Call<Unit>
```
