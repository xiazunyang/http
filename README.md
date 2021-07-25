### http开源工具包

适用于`OkHttp`以及`Retrofit`的一些开源工具，包括文件的上传与下载的进度回调、`Oauth2`的`token`管理、请求与响应记录的日志输出等。

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

#### Http请求响应日志输出工具
* 其实就是`HttpLoggingInterceptor`，但是解决了在传输非文本数据时，日志输出乱码的问题。
* 解决了`HttpLoggingInterceptor`导致的上传、下载文件时无法触发回调的问题。
* 使用方法参考`HttpLoggingInterceptor`