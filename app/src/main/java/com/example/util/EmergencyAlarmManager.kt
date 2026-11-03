package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * EmergencyAlarmManager handles continuous siren sound and repeating vibration
 * during emergencies, manual alarm activations, and evacuation drills (simulacros).
 */
object EmergencyAlarmManager {
    private const val TAG = "EmergencyAlarm"
    private var isAlarmRunning = false
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var vibrator: Vibrator? = null

    @Synchronized
    fun start(context: Context) {
        if (isAlarmRunning) return
        isAlarmRunning = true
        Log.d(TAG, "Starting constant emergency alarm sound and vibration loop")

        startRepeatingVibration(context)
        startContinuousSirenAudio()
    }

    @Synchronized
    fun stop(context: Context) {
        if (!isAlarmRunning && audioJob == null) return
        isAlarmRunning = false
        Log.d(TAG, "Stopping emergency alarm sound and vibration")

        stopRepeatingVibration(context)
        stopContinuousSirenAudio()
    }

    private fun startRepeatingVibration(context: Context) {
        try {
            val appCtx = context.applicationContext
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            // Continuous repeating emergency vibration: 500ms buzz, 150ms pause, 500ms buzz, 150ms pause
            val timings = longArrayOf(0, 500, 150, 500, 150)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // repeat = 0 (infinite loop)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, 0) // repeat = 0 (infinite loop)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting repeating vibration: ${e.message}")
        }
    }

    private fun stopRepeatingVibration(context: Context) {
        try {
            vibrator?.cancel()
            val appCtx = context.applicationContext
            val fallbackVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            fallbackVibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration: ${e.message}")
        }
    }

    private fun startContinuousSirenAudio() {
        audioJob?.cancel()
        audioJob = CoroutineScope(Dispatchers.Default).launch {
            val sampleRate = 22050
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufSize.coerceAtLeast(sampleRate / 4)

            try {
                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioTrack(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build(),
                        bufferSize,
                        AudioTrack.MODE_STREAM,
                        android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        android.media.AudioManager.STREAM_ALARM,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack?.play()

                // Generate realistic industrial mining emergency siren waveform
                // Sweeps frequency between 700 Hz and 1300 Hz every 700ms in a continuous loop
                val buffer = ShortArray(1024)
                var phase = 0.0
                var step = 0
                val cycleSteps = (sampleRate * 0.7).toInt() // 700ms full siren cycle

                while (isActive && isAlarmRunning) {
                    for (i in buffer.indices) {
                        val pos = (step % cycleSteps).toDouble() / cycleSteps
                        val freq = if (pos < 0.5) {
                            700.0 + (1300.0 - 700.0) * (pos * 2.0)
                        } else {
                            1300.0 - (1300.0 - 700.0) * ((pos - 0.5) * 2.0)
                        }

                        phase += 2.0 * PI * freq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI

                        buffer[i] = (sin(phase) * 28000).toInt().toShort()
                        step++
                    }
                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack siren loop failed: ${e.message}")
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    // Ignore
                }
                audioTrack = null
            }
        }
    }

    private fun stopContinuousSirenAudio() {
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }
}
