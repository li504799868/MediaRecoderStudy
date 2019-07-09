package com.example.mediarecoderstudy.mux

import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.CameraProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mediarecoderstudy.R
import com.example.mediarecoderstudy.record.video.VideoMuxer
import kotlinx.android.synthetic.main.activity_break_record.*
import java.io.File


/**
 *   @author li.zhipeng 2019.7.8
 *
 *          断点录制
 * */
class BreakRecordActivity : AppCompatActivity(), SurfaceHolder.Callback2 {

    private val filePrefix = "video_"

    private val fileDir = File("${Environment.getExternalStorageDirectory().absolutePath}/test/break")

    init {
        fileDir.mkdirs()
    }

    private var mediaRecorder: MediaRecorder = MediaRecorder()

    private var camera: Camera? = null

    private var isRecording = false

    private val loadingDialog by lazy {
        AlertDialog.Builder(this@BreakRecordActivity)
            .setMessage("视频合成中...")
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_break_record)
        surface_view.holder.addCallback(this)
        video_record.setOnClickListener {
            if (isRecording) {
                stopVideoRecord()
            } else {
                startVideoRecord()
            }

        }

        video_mux.setOnClickListener {
            loadingDialog.show()
            VideoMuxer.muxVideoList(
                fileDir.listFiles { file -> file.name.startsWith(filePrefix) },
                "${fileDir.absolutePath}/result.mp4"
            ) {
                loadingDialog.dismiss()
                Toast.makeText(this@BreakRecordActivity, "合成结束", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun createMediaPlayer() {
        mediaRecorder.apply {
            camera!!.unlock()
            setCamera(camera)
            setOrientationHint(90)
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P))
        }
    }

    private fun startVideoRecord() {
        createMediaPlayer()
        mediaRecorder.setOutputFile("${fileDir.absolutePath}/$filePrefix${System.currentTimeMillis()}.mp4")
        mediaRecorder.prepare()
        mediaRecorder.start()
        isRecording = true
        video_record.text = "停止录制"
    }

    private fun stopVideoRecord() {
        mediaRecorder.stop()
        mediaRecorder.reset()
        camera!!.lock()
        isRecording = false
        video_record.text = "开始录制"
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder.release()
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder?) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (holder.surface == null) {
            return
        }

        camera!!.stopPreview()
        camera!!.setPreviewDisplay(holder)
        camera!!.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        if (camera != null) {
            camera!!.stopPreview()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (camera == null) {
            camera = Camera.open()
        }
        camera!!.setDisplayOrientation(90)
        camera!!.setPreviewDisplay(holder)
        camera!!.startPreview()
    }
}
