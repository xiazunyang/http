package cn.numeron.okhttp.oauth

interface OAuthProvider {

    /**
     * 这个Map里面包含的数据会添加到每一个请求头中
     * 可将token信息添加到此map中
     * 此Map中与原请求中已存在的key会被忽略
     */
    val headers: Map<String, String>

    /** 授权Token */
    var accessToken: String?

    /**
     * 刷新token
     * @return String? 刷新后的新token
     */
    fun refreshToken(): String?

}