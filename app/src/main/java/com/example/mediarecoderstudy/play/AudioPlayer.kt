package com.example.mediarecoderstudy.play

import android.media.*
import android.os.AsyncTask
import com.example.mediarecoderstudy.record.audio.AudioRecorder
import com.example.mediarecoderstudy.record.audio.AudioRecorderImpl
import com.example.mediarecoderstudy.record.audio.MediaRecorderImpl
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FileInputStream

/***
 *  音频录制
 *
 */
object AudioPlayer {

    private const val sampleRateInH = 11025

    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    private val mediaPlayer = MediaPlayer()

    private var lastPath: String? = null

    /**
     * 开始播放
     *
     * */
    fun startPlay(path: String, audioRecorder: AudioRecorder) {
        if (audioRecorder is MediaRecorderImpl) {
            if (lastPath != path) {
                lastPath = path
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepare()
            }
            mediaPlayer.start()
        } else if (audioRecorder is AudioRecorderImpl) {
            PlayAsyncTask(path).execute()
        }

    }

    class PlayAsyncTask(private val audioRecordFile: String) : AsyncTask<Unit, Int, Unit>() {

        override fun doInBackground(vararg params: Unit?) {
            // 要保持与录音的AudioFormat相同，否则读取会出现问题
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRateInH,
                AudioFormat.CHANNEL_IN_MONO,
                ENCODING
            )

            val buffer = ShortArray(bufferSize)

            val dis = DataInputStream(BufferedInputStream(FileInputStream(audioRecordFile)))

            val audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRateInH)
                    .setEncoding(ENCODING).build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack.play()

            while (dis.available() > 0) {
                var i = 0
                while (i < buffer.size) {
                    buffer[i] = dis.readShort()
                    i++
                }
                audioTrack.write(buffer, 0, buffer.size)
            }

            dis.close()
        }
    }
}