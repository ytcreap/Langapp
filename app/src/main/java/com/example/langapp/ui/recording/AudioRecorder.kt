package com.example.langapp.ui.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(filePrefix: String): File {
        stop()

        val file = File.createTempFile(
            filePrefix.replace(Regex("[^A-Za-z0-9_-]"), "_"),
            ".m4a",
            context.cacheDir
        )

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(96_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        val file = outputFile
        recorder?.let { mediaRecorder ->
            try {
                mediaRecorder.stop()
            } catch (e: RuntimeException) {
                file?.delete()
            } finally {
                mediaRecorder.reset()
                mediaRecorder.release()
            }
        }

        recorder = null
        outputFile = null
        return file?.takeIf { it.exists() && it.length() > 0L }
    }

    fun cancel() {
        stop()?.delete()
    }
}
