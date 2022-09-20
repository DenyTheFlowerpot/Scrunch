package com.denytheflowerpot.scrunch.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.denytheflowerpot.scrunch.BuildConfig
import com.denytheflowerpot.scrunch.ScrunchApplication
import com.denytheflowerpot.scrunch.databinding.ActivityMainBinding
import com.denytheflowerpot.scrunch.fragments.PermissionTutorialDialogFragment
import com.denytheflowerpot.scrunch.services.FoldActionSignalingService
import com.denytheflowerpot.scrunch.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {
    private enum class PickerType { foldSound, unfoldSound }

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }
    private lateinit var binding: ActivityMainBinding
    private val foldSoundResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flag)
                    viewModel.setFoldSoundPath(uri)
                }
            }
        }
    private val unfoldSoundResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flag)
                    viewModel.setUnfoldSoundPath(uri)
                }
            }
        }

    private val permissionTutorialDialogTag = "permissionTutorialDialog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnFoldSound.setOnClickListener { openPicker(PickerType.foldSound) }
        binding.btnUnfoldSound.setOnClickListener { openPicker(PickerType.unfoldSound) }
        binding.swtStartService.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setServiceStarted(
                isChecked
            )
        }
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var tentativeVolume = binding.volumeSeekBar.progress

            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                tentativeVolume = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.setVolume(tentativeVolume.toFloat() / 100)
            }
        })

        viewModel.foldSoundName.observe(this) {
            binding.lblFoldSoundName.text = it
        }
        viewModel.unfoldSoundName.observe(this) {
            binding.lblUnfoldSoundName.text = it
        }
        viewModel.serviceStarted.observe(this) {
            binding.swtStartService.isChecked = it
        }
        viewModel.volume.observe(this) {
            binding.volumeSeekBar.progress = (it * 100).toInt()
        }
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        this.intent = i
    }

    override fun onResume() {
        super.onResume()
        if (intent != null && intent.getBooleanExtra(
                FoldActionSignalingService.stopServiceAction,
                false
            )
        ) {
            binding.swtStartService.isChecked = false
        }

        if (viewModel.showPermissionTutorial) {
            binding.swtStartService.isEnabled = false
            if (supportFragmentManager.findFragmentByTag(permissionTutorialDialogTag) == null) {
                PermissionTutorialDialogFragment().show(
                    supportFragmentManager,
                    permissionTutorialDialogTag
                )
            }
        }
    }

    private fun openPicker(type: PickerType) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            this.addCategory(Intent.CATEGORY_OPENABLE)
            this.type = "audio/*"
        }
        if (type == PickerType.foldSound) foldSoundResultLauncher.launch(intent) else unfoldSoundResultLauncher.launch(
            intent
        )
    }
}