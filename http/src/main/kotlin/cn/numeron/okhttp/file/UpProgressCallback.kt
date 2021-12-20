package cn.numeron.okhttp.file

/**
 * 上传进度回调
 */
fun interface UpProgressCallback {
    fun update(progress: Float)
}