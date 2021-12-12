package com.denytheflowerpot.scrunch.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import com.denytheflowerpot.scrunch.ScrunchApplication
import java.lang.Exception

class SoundPlaybackManager(private val context: Context) {
    private val pool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build())
            .build()
    }

    private var foldSoundId: Int? = null
    private var unfoldSoundId: Int? = null
    private val settingsManager by lazy { ScrunchApplication.instance.settingsManager }

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
        } catch(e: Exception) {
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
        } catch(e: Exception) {
            Log.d("Scrunch", "Failed to load unfold sound: ${e.message}")
        }
    }

    fun playFoldSound() {
        playSound(foldSoundId)
    }

    fun playUnfoldSound() {
        playSound(unfoldSoundId)
    }

    private fun playSound(id: Int?) {
        if (id != null) {
            pool.play(id, settingsManager.volume, settingsManager.volume, 0, 0, 1F)
        }
    }
}