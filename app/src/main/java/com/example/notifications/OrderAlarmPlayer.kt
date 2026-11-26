package com.example.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OrderAlarmPlayer {
    private const val TAG = "OrderAlarmPlayer"
    private const val AUTO_STOP_MS = 25_000L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val stopRunnable = Runnable { stop() }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun playLoudAlarm(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            stopInternal()
            val alarmUri = resolveAlarmUri()
            var startedAudio = false
            if (alarmUri != null) {
                try {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(appContext, alarmUri)
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                    startedAudio = true
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to start MediaPlayer alarm: ${e.message}")
                    runCatching { mediaPlayer?.release() }
                    mediaPlayer = null
                }
            }

            try {
                val vib = getVibrator(appContext)
                if (vib?.hasVibrator() == true) {
                    val pattern = longArrayOf(0L, 600L, 250L, 600L, 250L, 800L)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(pattern, 0)
                    }
                    vibrator = vib
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to vibrate for new order: ${e.message}")
            }

            _isPlaying.value = startedAudio || vibrator != null
            mainHandler.removeCallbacks(stopRunnable)
            if (_isPlaying.value) {
                mainHandler.postDelayed(stopRunnable, AUTO_STOP_MS)
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        mainHandler.removeCallbacks(stopRunnable)
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling vibrator: ${e.message}")
        } finally {
            vibrator = null
        }
        _isPlaying.value = false
    }

    private fun resolveAlarmUri(): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    @Suppress("DEPRECATION")
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
