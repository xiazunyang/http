### 文件上传/下载
* 通过`OkHttp`的拦截器实现的下载、上传断点续传功能，同时支持`OkHttp`和`Retrofit`。

* 断点续传
  - 无需额外的操作，默认即支持下载的断点续传；
  - 当文件存在时，并且与响应信息中的文件大小一致时，不会重复下载文件；
  - 当文件错误时，或无法从响应信息中获取到文件信息时，完整的下载并覆盖原文件；
  - 当文件只有一部分时，会下载剩余的部分数据，并追加到文件中。

### OkHttp

##### 安装方法
* 在构建`OkHttpClient`时，添加[`BreakpointResumeInterceptor`](https://github.com/xiazunyang/http/blob/master/http/src/main/kotlin/cn/numeron/okhttp/file/BreakpointResumeInterceptor.kt)拦截器。

##### 使用方法
* 指定文件的位置可以用`tag(Class<T>, T)`方法向请求实例中添加一个`File`实例，此参数可以是一个文件，也可以是一个目录：
    - 当`File`参数是文件时，下载的数据会写入到该文件中
    - 当`File`参数是目录时，会自动从响应信息或请求信息中获取文件名，并在该目录下创建一个文件，下载的数据会写入到该文件中

```kotlin
val file = File("文件或存放目录")
val request = Request.Builder()
    .tag(File::Class.java, file)
    ...
val response = okHttpClient.newCall(request).execute()
if (response.isSuccessful) {
    TODO("操作file即可，下载的数据已写入file文件中或目录下")
}
```

### Retrofit

##### 安装方法
* 在构建`OkHttpClient`时，添加[`BreakpointResumeInterceptor`](https://github.com/xiazunyang/http/blob/master/http/src/main/kotlin/cn/numeron/okhttp/file/BreakpointResumeInterceptor.kt)拦截器。
* 在构建`Retrofit`时，添加[`FileConverter`](https://github.com/xiazunyang/http/blob/master/http/src/main/kotlin/cn/numeron/retrofit/FileConverter.kt)转换器。

##### 使用方法
* 在接口中使用`@Tag`注解标记一个`File`类型的参数用于指定要写入的文件或存放目录

```kotlin
interface FileApi {

    /**
     * url 文件的下载地址
     * fileOrDir 要写入的文件或存放目录
     * callback 下载进度的监听接口
     * */
    @GET
    @Streaming
    suspend fun download(@Url url: String, @Tag fileOrDir: File, @Tag callback: DlProgressCallback): File
}

// 创建API接口的实例
val fileApi = retrofit.create<FileApi>()
// 创建参数实例
val downloadUrl = "文件的下载地址"
val file = File("文件位置或存放目录")
val callback = DlProgressCallback { progress -> 
    TODO("处理下载进度")
}
// 请求网络，注：添加`FileConverter`后，可使用File类型直接接收文件
val file = fileApi.download(downloadUrl, file, callback)
TODO("处理file文件")
```

#### 上传断点续传
* 上传的断点续传需要服务端的支持，一般的处理逻辑如下：
1. app端计算文件的`MD5`值；
2. 将`MD5`值提交到服务器，服务器返回`已存在的文件长度`等信息；
3. app将文件转为输入流，并忽略服务器上已存在的长度，将剩余的数据提交到服务器。
* 鉴于服务端的实现各有不同，所以此处只提供基于`Retrofit`的部分的逻辑，剩余逻辑请自行处理。
* 处理方法参考如下：
```kotlin
val file = File("要上传的文件路径")
//获取文件的MD5值
val fileMd5 = file.getMd5()
//将MD5值提交到服务器查询服务器上已存在的数据长度
val existLength: Long = uploadApi.getExistLength(fileMd5)
//创建上传进度监听器
val upProgressCallback = UpProgressCallback { progress ->
    TODO("处理上传进度")
}
val mediaType = TODO("获取MediaType.")
if(existLength <= 0) {
    //正常上传
    val requestBody = file.asRequestBody(mediaType)
    uploadApi.upload(requestBody, upProgressCallback)
} else {
    //断点续传，忽略掉前existLength的数据
    val fileBytes = file.readBytes()
    val requestBody = fileBytes.toRequestBoey(mediaType, existLength.toInt(), fileBytes.size - existLength.toInt())
    uploadApi.upload(file, ContinuableUpProgressCallback(existLength, upProgressCallback))
}
```