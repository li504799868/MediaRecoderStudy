package com.example.mediarecoderstudy

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.mediarecoderstudy.mux.BreakRecordActivity
import com.example.mediarecoderstudy.play.AudioPlayer
import com.example.mediarecoderstudy.play.MediaCodecVideoPlayerActivity
import com.example.mediarecoderstudy.record.audio.AudioRecorderImpl
import com.example.mediarecoderstudy.record.video.RecordVideoActivity
import com.example.mediarecoderstudy.record.video.VideoMuxer
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File


@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val audioRecorderFile: File

    private val filePrefix = "video_"

    private val breakFileDir = File("${Environment.getExternalStorageDirectory().absolutePath}/test/break")

    private val loadingDialog by lazy {
        AlertDialog.Builder(this@MainActivity)
            .setMessage("视频合成中...")
            .create()
    }


    init {

        val fileDir = File("${Environment.getExternalStorageDirectory().absolutePath}/test")
        fileDir.mkdirs()
        breakFileDir.mkdirs()
        // 创建要保存的录音文件的路径
        audioRecorderFile = File(fileDir, "record.aac")

    }

    private val audioReorder = AudioRecorderImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val liveData = MutableLiveData<String>()
        liveData.value = ""

        start_audio_record.setOnClickListener {
            startRecordWithPermissionCheck()
        }

        stop_audio_record.setOnClickListener {
            audioReorder.stopAudioRecord()
        }

        play_audio.setOnClickListener {
            if (audioReorder.isRecording()) {
                audioReorder.stopAudioRecord()
            }
            AudioPlayer.startPlay(audioRecorderFile.absolutePath, audioReorder)
        }

        media_codec_video.setOnClickListener {
            playVideoWithMediaCodecWithPermissionCheck()
        }

        // 开始录制视频
        start_video_record.setOnClickListener {
            startRecordVideoWithPermissionCheck()
        }

        break_record.setOnClickListener {
            startBreakRecordVideoWithPermissionCheck()
        }

        combine_audio_video.setOnClickListener {
            VideoMuxer.muxVideoList(
                breakFileDir.listFiles { file -> file.name.startsWith(filePrefix) },
                "${breakFileDir.absolutePath}/result.mp4"
            ) {
                loadingDialog.dismiss()
                Toast.makeText(this@MainActivity, "合成结束", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @NeedsPermission(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun startRecord() {
        audioReorder.startAudioRecord(audioRecorderFile.absolutePath)
    }

    @NeedsPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun playVideoWithMediaCodec() {
        startActivity(Intent(this, MediaCodecVideoPlayerActivity::class.java))
    }


    @NeedsPermission(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.CAMERA
    )
    fun startRecordVideo() {
        startActivity(Intent(this, RecordVideoActivity::class.java))
    }

    @NeedsPermission(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.CAMERA
    )
    fun startBreakRecordVideo() {
        startActivity(Intent(this, BreakRecordActivity::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

}
