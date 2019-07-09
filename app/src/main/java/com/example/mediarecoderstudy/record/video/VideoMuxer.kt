package com.example.mediarecoderstudy.record.video

import android.icu.text.IDNA
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.widget.Toast
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 *   @author  li.zhipeng on 2019.7.8
 *
 *      视频合成器
 * */
object VideoMuxer {

    private var mediaMuxer: MediaMuxer? = null

    private const val MAX_BUFF_SIZE = 1048576

    private val mReadBuffer = ByteBuffer.allocate(MAX_BUFF_SIZE)

    @JvmStatic
    fun muxVideoList(videoList: Array<File>, outPath: String, finish: () -> Unit) {
        // 创建MediaMuxer
        if (mediaMuxer == null) {
            mediaMuxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        var findVideoFormat = false
        var findAudioFormat = false

        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null

        var ptsOffset = 0L

        for (file in videoList) {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(file.absolutePath)

            if (!findVideoFormat) {
                // 找到视频
                videoFormat = findTrack(mediaExtractor, "video/")
                if (videoFormat != null) {
                    findVideoFormat = true
                }
            }

            if (!findAudioFormat) {
                audioFormat = findTrack(mediaExtractor, "audio/")
                if (audioFormat != null) {
                    findAudioFormat = true
                }
            }

            mediaExtractor.release()
            if (findAudioFormat && findVideoFormat) {
                break
            }
        }

        mediaMuxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        // 找到视轨
        if (findVideoFormat) {
            videoTrackIndex = mediaMuxer!!.addTrack(videoFormat!!)
        }

        var audioTrackIndex = -1
        // 找到音轨
        if (findAudioFormat) {
            audioTrackIndex = mediaMuxer!!.addTrack(audioFormat!!)
        }
        mediaMuxer!!.start()

        // 遍历视频列表
        for (file in videoList) {

            val path = file.absolutePath

            var hasVideo = true
            var hasAudio = true

            // 选择视轨
            val videoMediaExtractor = MediaExtractor()
            videoMediaExtractor.setDataSource(path)
            val videoIndex = findTrackIndex(videoMediaExtractor, "video/")
            if (videoIndex < 0) {
                hasVideo = false
            } else {
                // 选中视轨
                videoMediaExtractor.selectTrack(videoIndex)
            }

            // 选择音轨
            val audioMediaExtractor = MediaExtractor()
            audioMediaExtractor.setDataSource(path)
            val audioIndex = findTrackIndex(audioMediaExtractor, "audio/")
            if (audioIndex < 0) {
                hasAudio = false
            } else {
                audioMediaExtractor.selectTrack(audioIndex)
            }

            // 如果没有视轨和音轨，直接跳过该文件
            if (!hasAudio && !hasVideo) {
                videoMediaExtractor.release()
                audioMediaExtractor.release()
                continue
            }

            var hasDone = false
            var presentationTimeUs = 0L
            var videoPts = 0L
            var audioPts = 0L

            while (!hasDone) {
                if (!hasAudio && !hasVideo) {
                    break
                }

                var outTrackIndex = 0
                var currentTrackIndex = 0

                // 选择要使用的MediaExtractor
                val mediaExtractor = if ((!hasVideo || audioPts - videoPts <= 50000L) && hasAudio) {
                    currentTrackIndex = audioTrackIndex
                    outTrackIndex = audioTrackIndex
                    audioMediaExtractor
                } else {
                    currentTrackIndex = videoTrackIndex
                    outTrackIndex = videoTrackIndex
                    videoMediaExtractor
                }

                mReadBuffer.rewind()
                // 读取帧数据
                val frameSize = mediaExtractor.readSampleData(mReadBuffer, 0)
                if (frameSize < 0) {
                    if (currentTrackIndex == audioTrackIndex) {
                        hasAudio = false
                    } else if (currentTrackIndex == videoTrackIndex) {
                        hasVideo = false
                    }
                } else {
                    if (mediaExtractor.sampleTrackIndex != currentTrackIndex) {

                    }

                    // 读取帧的pts
                    presentationTimeUs = mediaExtractor.sampleTime
                    if (currentTrackIndex == videoTrackIndex) {
                        videoPts = presentationTimeUs
                    } else if (currentTrackIndex == audioTrackIndex) {
                        audioPts = presentationTimeUs
                    }

                    val bufferInfo = MediaCodec.BufferInfo()
                    bufferInfo.offset = 0
                    bufferInfo.size = frameSize
                    bufferInfo.presentationTimeUs = ptsOffset + presentationTimeUs

                    if ((mediaExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }

                    mReadBuffer.rewind()
                    // 将数据写入到合成文件中
                    mediaMuxer!!.writeSampleData(outTrackIndex, mReadBuffer, bufferInfo)
                    mediaExtractor.advance()
                }
            }
            // 更新pts的偏移值
            ptsOffset += max(videoPts, audioPts)
            ptsOffset += 10000L

            videoMediaExtractor.release()
            audioMediaExtractor.release()

        }

        if (mediaMuxer != null) {
            mediaMuxer!!.stop()
            mediaMuxer!!.release()
            mediaMuxer = null
        }

        finish.invoke()

    }

    private fun findTrack(mediaExtractor: MediaExtractor, prefix: String): MediaFormat? {

        val trackNum = mediaExtractor.trackCount
        for (i in 0 until trackNum) {
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            val format = mediaFormat.getString("mime")
            if (format.startsWith(prefix)) {
                return mediaFormat
            }
        }
        return null
    }

    private fun findTrackIndex(mediaExtractor: MediaExtractor, prefix: String): Int {

        val trackNum = mediaExtractor.trackCount
        for (i in 0 until trackNum) {
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            val format = mediaFormat.getString("mime")
            if (format.startsWith(prefix)) {
                return i
            }
        }
        return -1
    }
}