package com.denytheflowerpot.scrunch.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import com.denytheflowerpot.scrunch.ScrunchApplication

class SoundPlaybackManager(private val context: Context) {
    enum class StreamType {
        SYSTEM, MEDIA, NOTIFICATIONS
    }

    private lateinit var pool: SoundPool

    private var foldSoundId: Int? = null
    private var unfoldSoundId: Int? = null
    private val settingsManager by lazy { ScrunchApplication.instance.settingsManager }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    init {
        initializeSoundPool()
    }

    fun initializeSoundPool() {
        pool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(getContentType())
                    .setUsage(getUsage())
                    .build()
            )
            .build()
        loadPreviousSounds()
    }

    fun loadPreviousSounds() {
        loadFoldSound(settingsManager.foldSoundURL)
        loadUnfoldSound(settingsManager.unfoldSoundURL)
    }

    fun loadFoldSound(path: String) {
        if (foldSoundId != null) {
            pool.unload(foldSoundId!!)
        }
        val uri = Uri.parse(path)
        try {
            val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            foldSoundId = pool.load(descriptor, 1)
        } catch (e: Exception) {
            Log.d("Scrunch", "Failed to load fold sound: ${e.message}")
        }
    }

    fun loadUnfoldSound(path: String) {
        if (unfoldSoundId != null) {
            pool.unload(unfoldSoundId!!)
        }
        val uri = Uri.parse(path)
        try {
            val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            unfoldSoundId = pool.load(descriptor, 1)
        } catch (e: Exception) {
            Log.d("Scrunch", "Failed to load unfold sound: ${e.message}")
        }
    }

    fun playFoldSound() {
        playSound(foldSoundId)
    }

    fun playUnfoldSound() {
        playSound(unfoldSoundId)
    }

    private fun getContentType(): Int {
        return when (settingsManager.streamType) {
            //no dedicated notification type, but that should be OK
            StreamType.SYSTEM, StreamType.NOTIFICATIONS -> AudioAttributes.CONTENT_TYPE_SONIFICATION
            StreamType.MEDIA -> AudioAttributes.CONTENT_TYPE_MUSIC
        }
    }

    private fun getUsage(): Int {
        return when (settingsManager.streamType) {
            StreamType.SYSTEM -> AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
            StreamType.NOTIFICATIONS -> AudioAttributes.USAGE_NOTIFICATION_EVENT
            StreamType.MEDIA -> AudioAttributes.USAGE_MEDIA
        }
    }

    private fun playSound(id: Int?) {
        if (id != null && (settingsManager.playOverAudio || audioManager?.isMusicActive == false)) {
            pool.play(id, settingsManager.volume, settingsManager.volume, 0, 0, 1F)
        }
    }
}