package cn.numeron.okhttp.oauth

interface OAuthProvider {

    /**
     * 这个Map里面包含的数据会添加到每一个请求头中
     * 可将token信息添加到此map中
     * 此map中的请求头有最高优先级，原请求中与此Map中相同的key会被替换
     */
    val headers: Map<String, String>

    /**
     * 刷新token
     * @return String? 刷新后的新token
     */
    fun refreshToken(): String?

}