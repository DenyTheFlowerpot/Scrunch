package com.denytheflowerpot.scrunch.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.denytheflowerpot.scrunch.R

class PermissionTutorialDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                                .setTitle(R.string.permission_tutorial_title)
                                .setMessage(HtmlCompat.fromHtml(requireContext().getString(R.string.permission_tutorial_msg), HtmlCompat.FROM_HTML_MODE_LEGACY))
                                .setPositiveButton("OK") { _, _ -> dismiss() }
                                .create()
    }
}