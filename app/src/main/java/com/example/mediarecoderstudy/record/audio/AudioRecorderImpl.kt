package com.example.mediarecoderstudy.record.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream

/***
 *  音频录制， 通过MediaRecorder实现
 *
 */
class AudioRecorderImpl : AudioRecorder {

    companion object {

        private const val sampleRateInH = 11025

        private const val AudioSource = MediaRecorder.AudioSource.DEFAULT

        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO

        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private var currentRecordTask: RecordTask? = null

    /**
     *  是否正在录音，需要自己维护是否正在录音的状态位
     * */
    private var recording = false

    private fun createAudioRecorder(): AudioRecord {
        return AudioRecord(
            AudioSource,
            sampleRateInH,
            CHANNEL,
            ENCODING,
            AudioRecord.getMinBufferSize(
                sampleRateInH,
                CHANNEL,
                ENCODING
            )
        )
    }

    override fun isRecording(): Boolean = recording

    /**
     * 开始录制
     * */
    override fun startAudioRecord(path: String) {
        if (currentRecordTask == null) {
            currentRecordTask = RecordTask(path)
            currentRecordTask!!.execute()
        }
    }

    /**
     * 停止录制
     * */
    override fun stopAudioRecord() {
        if (recording) {
            recording = false
            currentRecordTask?.stopRecord()
            currentRecordTask?.cancel(true)
            currentRecordTask = null
        }
    }

    /**
     * 录音任务
     * */
    inner class RecordTask(private val audioRecordFile: String) : AsyncTask<Unit, Int, Unit>() {

        private var audioRecorder: AudioRecord? = null
        private var dos: DataOutputStream? = null

        override fun doInBackground(vararg params: Unit) {
            recording = true

            // 创建录音文件的输入流
            dos = DataOutputStream(BufferedOutputStream(FileOutputStream(audioRecordFile)))
            // 计算每次读取的buffer大小
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRateInH,
                CHANNEL,
                ENCODING
            )
            // 创建AudioRecorder
            audioRecorder = createAudioRecorder()
            // 读取录音数据的缓冲池
            val buffer = ShortArray(bufferSize)
            // 开始录音
            audioRecorder!!.startRecording()

            var r = 0
            while (isRecording()) {
                // 读取录音的数据长度
                // 已经把数据写入到了buffer数组里
                val bufferReadResult = audioRecorder!!.read(buffer, 0, bufferSize)
                for (index in 0 until bufferReadResult) {
                    dos!!.writeShort(buffer[index].toInt())
                }
                r++
            }
        }

        fun stopRecord() {
            audioRecorder?.stop()
            audioRecorder = null
            dos?.close()
            dos = null
        }


    }

}