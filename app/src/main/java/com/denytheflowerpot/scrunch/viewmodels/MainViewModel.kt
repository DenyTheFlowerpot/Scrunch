package com.denytheflowerpot.scrunch.viewmodels

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.denytheflowerpot.scrunch.ScrunchApplication
import com.denytheflowerpot.scrunch.managers.SettingsManager
import com.denytheflowerpot.scrunch.managers.SoundPlaybackManager
import com.denytheflowerpot.scrunch.util.PermissionUtils

class MainViewModel(app: Application): AndroidViewModel(app) {
    private val foldSoundURL: MutableLiveData<Uri?> by lazy {
        MutableLiveData<Uri?>(Uri.parse(settingsManager.foldSoundURL))
    }
    private val unfoldSoundURL: MutableLiveData<Uri?> by lazy {
        MutableLiveData<Uri?>(Uri.parse(settingsManager.unfoldSoundURL))
    }
    val serviceStarted: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(settingsManager.serviceStarted)
    }
    val volume: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>(settingsManager.volume)
    }

    val showPermissionTutorial = PermissionUtils.needsToGrantReadLogs(app)

    val foldSoundName = MediatorLiveData<String>().apply {
        addSource(foldSoundURL) {
            this.value = if (it != null) DocumentFile.fromSingleUri(getApplication(), it)?.name else ""
        }
    }

    val unfoldSoundName = MediatorLiveData<String>().apply {
        addSource(unfoldSoundURL) {
            this.value = if (it != null) DocumentFile.fromSingleUri(getApplication(), it)?.name else ""
        }
    }

    private val settingsManager: SettingsManager
        get() = getApplication<ScrunchApplication>().settingsManager
    private val soundPlaybackManager: SoundPlaybackManager
        get() = getApplication<ScrunchApplication>().soundPlaybackManager

    fun setFoldSoundPath(path: Uri?) {
        if (path != null) {
            val p = path.toString()
            settingsManager.foldSoundURL = p
            soundPlaybackManager.loadFoldSound(p)
            foldSoundURL.value = path
        }

    }

    fun setUnfoldSoundPath(path: Uri?) {
        if (path != null) {
            val p = path.toString()
            settingsManager.unfoldSoundURL = p
            soundPlaybackManager.loadUnfoldSound(p)
            unfoldSoundURL.value = path
        }
    }

    fun setServiceStarted(started: Boolean) {
        if (serviceStarted.value != started) {
            serviceStarted.value = started
            settingsManager.serviceStarted = started
            val app = getApplication<ScrunchApplication>()
            app.startForegroundService(app.getServiceIntent(started))
        }
    }

    fun setVolume(value: Float) {
        volume.value = value
        settingsManager.volume = value
        soundPlaybackManager.playUnfoldSound()
    }
}