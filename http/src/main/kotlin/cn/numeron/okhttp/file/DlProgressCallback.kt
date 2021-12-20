package cn.numeron.okhttp.file

/**
 * 下载进度回调
 */
fun interface DlProgressCallback {
    fun update(progress: Float)
}