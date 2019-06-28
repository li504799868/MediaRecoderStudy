package com.example.mediarecoderstudy.play

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException


/**
 *  使用MediaCodec解码视频文件，播放在SurfaceView
 * */
class MediaCodecVideoPlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val TAG = "MediaCodecVideoPlayer"

    private val filePath = "${Environment.getExternalStorageDirectory()}/DCIM/Camera/VID_20190624_104933.mp4"

    private var workerThread: WorkerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_media_codec_video_player)

        val surfaceView = SurfaceView(this)
        // 设置Surface不维护自己的缓冲区，等待屏幕的渲染引擎将内容推送到用户面前
        // 该api已经废弃，这个编辑会自动设置
//        surfaceView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surfaceView.holder.addCallback(this)
        setContentView(surfaceView)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (workerThread == null) {
            workerThread = WorkerThread(holder!!.surface)
                workerThread!!.start()
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            if (workerThread != null) {
                workerThread!!.interrupt()
                workerThread = null
            }
        }

        /**
         *   自定义解码的工作线程
         * */
        inner class WorkerThread(private val surface: Surface) : Thread() {

            private var mediaExtractor: MediaExtractor = MediaExtractor()
            private var mediaCodec: MediaCodec? = null

            override fun run() {
//            super.run()
                try {
                    mediaExtractor.setDataSource(filePath)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                for (i in 0 until mediaExtractor.trackCount) {
                    // 遍历数据音视频轨迹
                    val mediaFormat = mediaExtractor.getTrackFormat(i)
                    Log.e(TAG, ">> format i $i : $mediaFormat")
                    val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                    Log.e(TAG, ">> mime i $i : $mime")
                    if (mime.startsWith("video/")) {
                        mediaExtractor.selectTrack(i)
                        try {
                            mediaCodec = MediaCodec.createDecoderByType(mime)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        mediaCodec!!.configure(mediaFormat, surface, null, 0)

                    }
                }

                if (mediaCodec == null) {
                    return
                }

                mediaCodec!!.start()

                val inputBuffers = mediaCodec!!.inputBuffers
                var outputBuffers = mediaCodec!!.outputBuffers

                // 每个buffer的元数据包括具体范围的偏移及大小，以及有效数据中相关的解码的buffer
                val info = MediaCodec.BufferInfo()
                // 是否已经读到了结束的位置
                var isEOS = false
                val startMs = System.currentTimeMillis()
                while (!interrupted()) {
                    if (!isEOS) {
                        // 返回使用有效输出的Buffer索引，如果没有相关Buffer可用，就返回-1
                        // 如果传入的timeoutUs为0， 将立马返回
                        // 如果输入的buffer可用，就无限期等待，timeoutUs的单位是us
                        val inIndex = mediaCodec!!.dequeueInputBuffer(10000)
                        if (inIndex > 0) {
                            val buffer = inputBuffers[inIndex]
                            Log.e(TAG, ">> buffer $buffer")
                            val sampleSize = mediaExtractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                Log.e(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                                mediaCodec!!.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEOS = true
                            } else {
                                mediaCodec!!.queueInputBuffer(inIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)
                                mediaExtractor.advance()
                            }

                        }
                    }

                //
                val outIndex = mediaCodec!!.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        // 当buffer变化时，客户端必须重新指向新的buffer
                        outputBuffers = mediaCodec!!.outputBuffers
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // 当buffer的格式发生改变，须指向新的buffer格式
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // 当dequeueOutputBuffer超时时，会到达此case
                        Log.e(TAG, ">> dequeueOutputBuffer timeout")
                    }
                    else -> {
                        val buffer = outputBuffers[outIndex]
                        // 这里使用简单的时钟方式保持视频的fps，不然视频会播放的很快
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            sleep(10)
                            mediaCodec!!.releaseOutputBuffer(outIndex, true)
                            break
                        }
                    }
                }

                // 在所有解码后的帧都被渲染后，就可以停止播放了
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.e(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    break
                }

            }

            mediaCodec!!.stop()
            mediaCodec!!.release()
            mediaExtractor.release()

        }


    }


}
