package com.denytheflowerpot.scrunch.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.denytheflowerpot.scrunch.ScrunchApplication
import com.denytheflowerpot.scrunch.helpers.Constants

class SettingsManager(private val context: Context) {
    private class Keys {
        companion object {
            const val UnfoldSoundURL = "unfoldSoundURL"
            const val FoldSoundURL = "foldSoundURL"
            const val ServiceStarted = "serviceStarted"
            const val Volume = "volume"
            const val PlayOverAudio = "playOverAudio"
            const val StreamType = "streamType"
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(Constants.SharedPreferencesFile, Context.MODE_PRIVATE)
    }

    private val soundPlaybackManager: SoundPlaybackManager by lazy {
        ScrunchApplication.instance.soundPlaybackManager
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Keys.StreamType) {
            soundPlaybackManager.initializeSoundPool()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    var unfoldSoundURL: String
        get() = prefs.getString(Keys.UnfoldSoundURL, "") ?: ""
        set(value) {
            prefs.edit().putString(Keys.UnfoldSoundURL, value).apply()
        }

    var foldSoundURL: String
        get() = prefs.getString(Keys.FoldSoundURL, "") ?: ""
        set(value) {
            prefs.edit().putString(Keys.FoldSoundURL, value).apply()
        }

    var serviceStarted: Boolean
        get() = prefs.getBoolean(Keys.ServiceStarted, false)
        set(value) {
            prefs.edit().putBoolean(Keys.ServiceStarted, value).apply()
        }

    var volume: Float
        get() = prefs.getFloat(Keys.Volume, 1F)
        set(value) {
            prefs.edit().putFloat(Keys.Volume, value).apply()
        }

    //from AdvancedSettingsFragment
    val playOverAudio: Boolean
        get() = prefs.getBoolean(Keys.PlayOverAudio, true)

    val streamType: SoundPlaybackManager.StreamType
        get() = SoundPlaybackManager.StreamType.valueOf(
            (prefs.getString(Keys.StreamType, "system") ?: "system").uppercase()
        )
}