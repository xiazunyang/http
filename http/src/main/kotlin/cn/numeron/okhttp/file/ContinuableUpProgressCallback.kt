package cn.numeron.okhttp.file

/**
 * 当上传使用断点伟传时，可使用此对象告知进度回调已经上传了多少数据
 * @property existLength Long 已经上传的数据长度
 */
class ContinuableUpProgressCallback(

        /**
         * 表示已提交的文件长度
         */
        val existLength: Long,

        /**
         * 进度回调
         */
        callback: UpProgressCallback

) : UpProgressCallback by callback