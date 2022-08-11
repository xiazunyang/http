package cn.numeron.retrofit.timout

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RequestTimeout(

    /**
     * 超时时间
     * 若[connect]、[read]、[write]属性没有值，则也适用于该属性
     */
    val value: Int = -1,

    /** 连接超时时间 */
    val connect: Int = -1,

    /** 读取超时时间 */
    val read: Int = -1,

    /** 写入超时时间 */
    val write: Int = -1,

    /** 超时时间单位 */
    val unit: TimeUnit = TimeUnit.SECONDS,
)
