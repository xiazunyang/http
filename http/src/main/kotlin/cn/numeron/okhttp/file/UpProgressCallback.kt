package cn.numeron.okhttp.file

/**
 * 上传进度回调
 */
fun interface UpProgressCallback {
    /** progress是进度的百分比，是从0到1的浮点数值 */
    fun update(progress: Float)
}