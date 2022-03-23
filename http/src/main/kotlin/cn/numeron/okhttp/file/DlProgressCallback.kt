package cn.numeron.okhttp.file

/**
 * 下载进度回调
 */
fun interface DlProgressCallback {
    /** progress是进度的百分比，是从0到1的浮点数值 */
    fun update(progress: Float)
}