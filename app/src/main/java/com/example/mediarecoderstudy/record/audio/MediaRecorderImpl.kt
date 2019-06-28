package com.example.mediarecoderstudy.record.audio

import android.media.MediaRecorder

/***
 *  音频录制， 通过MediaRecorder实现
 *
 */
class MediaRecorderImpl : AudioRecorder {

    /**
     *  是否正在录音，需要自己维护是否正在录音的状态位
     * */
    private var recording = false

    private var currentMediaRecorder: MediaRecorder? = null


    private fun createMediaRecorder(): MediaRecorder {
        // 使用默认的录音设备
        val mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        // 设置录音文件的输出格式
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
        // 设置录音编码器
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        // 可以设置最大时间
        mediaRecorder.setMaxDuration(5000)
        return mediaRecorder
    }

    override fun isRecording(): Boolean = recording

    /**
     * 开始录制
     * */
    override fun startAudioRecord(path: String) {

        if (currentMediaRecorder == null) {
            currentMediaRecorder = createMediaRecorder()
            // 文件输出路径
            currentMediaRecorder!!.setOutputFile(path)
        }
        currentMediaRecorder!!.prepare()
        currentMediaRecorder!!.start()
        recording = true
    }

    /**
     * 停止录制
     * */
    override fun stopAudioRecord() {
        if (recording) {
            recording = false
            if (currentMediaRecorder != null) {
                currentMediaRecorder!!.stop()
                currentMediaRecorder!!.release()
                currentMediaRecorder = null
            }
        }
    }

}