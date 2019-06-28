package com.example.mediarecoderstudy.record.audio

interface AudioRecorder {

    fun startAudioRecord(path: String)

    fun stopAudioRecord()

    fun isRecording(): Boolean
}