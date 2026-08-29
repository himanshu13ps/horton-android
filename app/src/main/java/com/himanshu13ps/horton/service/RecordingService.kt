package com.himanshu13ps.horton.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.himanshu13ps.horton.audio.AudioCaptureManager
import com.himanshu13ps.horton.audio.VADProcessor
import com.himanshu13ps.horton.ml.TranscriptionEngine
import com.himanshu13ps.horton.data.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RecordingService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var vadProcessor: VADProcessor
    private lateinit var transcriptionEngine: TranscriptionEngine

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        audioCaptureManager = AudioCaptureManager()
        vadProcessor = VADProcessor(this)
        
        // Assuming latest conversationId is handled, we'll use a stub ID 1 for now
        val db = NoteDatabase.getDatabase(this)
        transcriptionEngine = TranscriptionEngine(db.noteDao(), 1L, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> startRecordingSession()
            ACTION_STOP -> stopRecordingSession()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Note-Taker")
            .setContentText("Actively listening and transcribing...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // placeholder icon
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start listening to completed audio files for STT
        serviceScope.launch {
            transcriptionEngine.startProcessing(vadProcessor.completedFilesFlow)
        }

        // Start capturing audio and feeding it to VAD
        serviceScope.launch {
            audioCaptureManager.startCapture().collect { chunk ->
                vadProcessor.processAudioChunk(chunk)
            }
        }
    }

    private fun stopRecordingSession() {
        audioCaptureManager.stopCapture()
        vadProcessor.finishSession()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NoteTaker::RecordingWakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L /*10 hours max*/)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        private const val CHANNEL_ID = "recording_service_channel"
        private const val NOTIFICATION_ID = 1
    }
}
